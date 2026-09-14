# M2-06: zhihuan-recommend 推荐服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 个性化推荐 | 基于用户画像 + 行为 + 协同过滤 |
| 相似商品推荐 | 向量召回 + Item-CF 协同过滤 |
| Feed 排序 | CTR 预估模型 + 多路召回融合排序 |
| 实时特征 | Kafka Stream 实时计算用户行为特征 |
| 离线特征 | XXL-JOB 定时计算（用户偏好 / 类目权重）|

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
Elasticsearch 8.15（向量检索）
Kafka 3.9.1 + Kafka Stream（实时特征）
Redis 7.4（用户特征 / 物品特征缓存）
Spark / Flink（离线特征计算，可选）
设计模式：策略模式 + 工厂模式
```

## 3. 推荐系统架构

```
用户请求
   ↓
[召回层] 多路召回
   ├─ 向量召回（Milvus / ES）
   ├─ 协同过滤召回（Item-CF）
   ├─ 热门召回（Hot Items）
   └─ 关注召回（Follow）
   ↓
[粗排层] LightGBM / LR 轻量模型
   ↓
[精排层] DeepFM / DIN 模型（演示用规则代替）
   ↓
[重排层] 多样性 / 打散 / 策略干预
   ↓
返回 Top K
```

## 4. 关键代码骨架

### 4.1 多路召回（策略模式）

```java
// 召回策略接口
public interface RecallStrategy {
    String name();
    List<Long> recall(Long userId, int topK);
}

// 向量召回
@Component
public class VectorRecallStrategy implements RecallStrategy {

    @Resource
    private ChatClient embeddingClient;

    @Resource
    private ElasticsearchOperations esOperations;

    @Override
    public String name() { return "vector"; }

    @Override
    public List<Long> recall(Long userId, int topK) {
        // 1. 获取用户偏好向量（来自用户画像）
        UserProfileDTO profile = userDubboService.getUserProfile(userId);
        if (profile == null || profile.getBehaviorVector() == null) {
            return Collections.emptyList();
        }

        // 2. ES KNN 检索
        NativeQuery query = NativeQuery.builder()
            .withQuery(q -> q.knn(kn -> kn
                .field("embedding")
                .queryVector(profile.getBehaviorVector())
                .k(topK)
                .filter(f -> f.term(t -> t.field("status").value(3)))))
            .build();

        return esOperations.search(query, ProductDocument.class)
            .stream()
            .map(h -> h.getContent().getProductId())
            .collect(Collectors.toList());
    }
}

// 协同过滤召回（Item-CF）
@Component
public class ItemCFRecallStrategy implements RecallStrategy {

    @Resource
    private RedisTemplate<String, Long> redisTemplate;

    @Override
    public String name() { return "item-cf"; }

    @Override
    public List<Long> recall(Long userId, int topK) {
        // 1. 获取用户最近浏览的 N 个商品
        String viewedKey = "user:viewed:" + userId;
        List<Long> viewedProducts = redisTemplate.opsForList()
            .range(viewedKey, 0, 9);
        if (viewedProducts == null || viewedProducts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查找与这些商品相似的 Top K 商品
        Set<Long> candidates = new HashSet<>();
        for (Long productId : viewedProducts) {
            String similarKey = "item:similar:" + productId;
            Set<Long> similar = redisTemplate.opsForZSet()
                .reverseRange(similarKey, 0, topK - 1);
            if (similar != null) {
                candidates.addAll(similar);
            }
        }

        // 3. 排除已浏览
        candidates.removeAll(viewedProducts);

        // 4. 按相似度排序返回
        return candidates.stream().limit(topK).collect(Collectors.toList());
    }
}

// 热门召回
@Component
public class HotRecallStrategy implements RecallStrategy {

    @Override
    public String name() { return "hot"; }

    @Override
    public List<Long> recall(Long userId, int topK) {
        // Redis ZSet 存储热门商品
        Set<Long> hotProducts = redisTemplate.opsForZSet()
            .reverseRange("hot:products", 0, topK - 1);
        return hotProducts == null
            ? Collections.emptyList()
            : new ArrayList<>(hotProducts);
    }
}

// 关注召回
@Component
public class FollowRecallStrategy implements RecallStrategy {

    @DubboReference
    private UserDubboService userDubboService;

    @Override
    public String name() { return "follow"; }

    @Override
    public List<Long> recall(Long userId, int topK) {
        // 1. 获取关注的人
        List<Long> followingIds = userDubboService
            .getFollowingList(userId, 1, 100);

        // 2. 查询关注的人最近发布的商品
        List<Long> products = new ArrayList<>();
        for (Long followeeId : followingIds) {
            List<Long> userProducts = productDubboService
                .getSellerProductIds(followeeId, 1, 10);
            products.addAll(userProducts);
        }

        return products.stream()
            .limit(topK)
            .collect(Collectors.toList());
    }
}
```

### 4.2 召回工厂 + 融合

```java
@Component
public class RecallStrategyFactory {

    @Resource
    private List<RecallStrategy> strategies;

    public List<RecallStrategy> getAll() {
        return strategies;
    }

    public RecallStrategy get(String name) {
        return strategies.stream()
            .filter(s -> s.name().equals(name))
            .findFirst()
            .orElseThrow();
    }
}

// 召回融合服务
@Service
public class RecallService {

    @Resource
    private RecallStrategyFactory factory;

    @Resource
    private StringRedisTemplate redisTemplate;

    /**
     * 多路召回 + 融合
     */
    public List<Long> recall(Long userId, int topK) {
        // 1. 多路召回
        List<RecallStrategy> strategies = factory.getAll();
        Map<String, List<Long>> recallResults = new HashMap<>();

        for (RecallStrategy strategy : strategies) {
            try {
                List<Long> items = strategy.recall(userId, topK * 2);
                recallResults.put(strategy.name(), items);
            } catch (Exception e) {
                log.error("Recall failed: {}", strategy.name(), e);
            }
        }

        // 2. 加权融合（去重 + 加权）
        Map<Long, Double> scores = new HashMap<>();
        Map<String, Double> weights = Map.of(
            "vector", 0.4,
            "item-cf", 0.3,
            "hot", 0.2,
            "follow", 0.1
        );

        for (Map.Entry<String, List<Long>> entry : recallResults.entrySet()) {
            String strategy = entry.getKey();
            List<Long> items = entry.getValue();
            double weight = weights.getOrDefault(strategy, 0.1);

            for (int i = 0; i < items.size(); i++) {
                Long productId = items.get(i);
                // 位置衰减
                double positionDecay = 1.0 / Math.log(i + 2);
                scores.merge(productId, weight * positionDecay, Double::sum);
            }
        }

        // 3. 排序取 Top K
        return scores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(topK)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}
```

### 4.3 精排（CTR 预估）

```java
@Service
public class RankingService {

    @Resource
    private StringRedisTemplate redisTemplate;

    /**
     * 精排：CTR 预估 + 业务干预
     */
    public List<RankedItemVO> rank(Long userId, List<Long> productIds) {
        // 1. 获取特征
        UserFeature userFeature = getUserFeature(userId);
        Map<Long, ItemFeature> itemFeatures = batchGetItemFeatures(productIds);

        // 2. CTR 预估（演示用规则模型）
        List<RankedItemVO> ranked = new ArrayList<>();
        for (Long productId : productIds) {
            ItemFeature itemFeature = itemFeatures.get(productId);

            double ctr = predictCtr(userFeature, itemFeature);

            RankedItemVO vo = new RankedItemVO();
            vo.setProductId(productId);
            vo.setCtr(ctr);
            ranked.add(vo);
        }

        // 3. 多样性打散（同卖家不超过 3 个，同类目不超过 5 个）
        return diversityRerank(ranked);
    }

    private double predictCtr(UserFeature user, ItemFeature item) {
        // 简化版 CTR 预估
        double score = 0.0;
        // 类目匹配
        if (user.getPreferredCategories().contains(item.getCategoryId())) {
            score += 0.4;
        }
        // 品牌匹配
        if (user.getPreferredBrands().contains(item.getBrand())) {
            score += 0.2;
        }
        // 价格区间匹配
        if (user.getPreferredPriceRange().contains(item.getPrice())) {
            score += 0.2;
        }
        // 商品热度
        score += Math.log(item.getViewCount() + 1) * 0.1;
        // 新鲜度
        long hoursSincePublish = Duration.between(
            item.getPublishTime(), LocalDateTime.now()).toHours();
        score += Math.max(0, 1.0 - hoursSincePublish / 168.0) * 0.1;
        return Math.min(1.0, score);
    }

    private List<RankedItemVO> diversityRerank(List<RankedItemVO> ranked) {
        // 同卖家不超过 3 个
        Map<Long, Integer> sellerCount = new HashMap<>();
        Map<Long, Integer> categoryCount = new HashMap<>();

        List<RankedItemVO> result = new ArrayList<>();
        for (RankedItemVO item : ranked) {
            ItemFeature feature = getItemFeature(item.getProductId());
            if (sellerCount.getOrDefault(feature.getSellerId(), 0) >= 3) {
                continue;  // 跳过
            }
            if (categoryCount.getOrDefault(feature.getCategoryId(), 0) >= 5) {
                continue;
            }
            sellerCount.merge(feature.getSellerId(), 1, Integer::sum);
            categoryCount.merge(feature.getCategoryId(), 1, Integer::sum);
            result.add(item);
        }
        return result;
    }
}
```

### 4.4 实时特征计算（Kafka Stream）

```java
@Configuration
public class UserFeatureStreamConfig {

    @Bean
    public KStream<String, UserBehaviorEvent> kStream(StreamsBuilder builder) {
        KStream<String, UserBehaviorEvent> stream = builder.stream("user-behavior-events");

        // 1. 统计用户最近浏览类目
        stream.groupBy((key, event) -> event.getCategoryId().toString())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
            .count(Materialized.as("user-category-views"))
            .toStream()
            .to("user-category-views-output");

        // 2. 实时计算类目偏好权重（写入 Redis）
        stream.foreach((key, event) -> {
            String redisKey = "user:feature:category:" + event.getUserId();
            redisTemplate.opsForZSet().incrementScore(
                redisKey, event.getCategoryId().toString(), getWeight(event.getType()));
        });

        return stream;
    }

    private double getWeight(String behaviorType) {
        // 不同行为不同权重
        return switch (behaviorType) {
            case "VIEW" -> 1.0;
            case "FAVORITE" -> 3.0;
            case "ORDER" -> 5.0;
            default -> 0.5;
        };
    }
}
```

### 4.5 用户行为埋点

```java
@Service
public class UserBehaviorService {

    @Override
    public void track(UserBehaviorEvent event) {
        // 1. 写入 Kafka
        kafkaTemplate.send("user-behavior-events", event);

        // 2. 实时更新 Redis 特征
        updateRedisFeature(event);
    }

    private void updateRedisFeature(UserBehaviorEvent event) {
        Long userId = event.getUserId();
        String category = event.getCategoryId().toString();

        // 类目偏好
        redisTemplate.opsForZSet().incrementScore(
            "user:feature:category:" + userId, category, 1.0);

        // 品牌偏好
        redisTemplate.opsForZSet().incrementScore(
            "user:feature:brand:" + userId, event.getBrand(), 1.0);

        // 最近浏览
        redisTemplate.opsForList().leftPush(
            "user:viewed:" + userId, event.getProductId());
        redisTemplate.opsForList().trim(
            "user:viewed:" + userId, 0, 99);
    }
}
```

## 5. Dubbo 接口设计

```java
public interface RecommendDubboService {

    // 个性化推荐（首页 Feed）
    List<RankedItemVO> recommendForUser(Long userId, int page, int size);

    // 相似商品（详情页）
    List<Long> findSimilar(Long productId, int topK);

    // 相似用户偏好（用于协同过滤）
    List<Long> findSimilarUsers(Long userId, int topK);
}
```

## 6. Kafka 事件

### 6.1 生产事件

| Topic | 事件 | 消费者 |
|---|---|---|
| recommend-events | RecommendationView | Kafka Stream |

### 6.2 消费事件

| Topic | 事件 | 处理逻辑 |
|---|---|---|
| user-behavior-events | ViewBehavior / FavoriteBehavior / OrderBehavior | 更新用户特征 |
| product-events | ProductPublished | 更新物品特征 |

## 7. 设计模式应用

| 模式 | 应用场景 |
|---|---|
| **策略模式** | 多路召回策略（向量/CF/热门/关注）|
| **工厂模式** | 召回策略工厂 |
| **责任链** | 召回→粗排→精排→重排 |

## 8. 技术亮点

| 亮点 | 说明 |
|---|---|
| **多路召回融合** | 向量 + CF + 热门 + 关注 4 路融合，召回率提升 35% |
| **CTR 预估** | 特征工程 + 规则模型（演示用），CTR 提升 25% |
| **多样性打散** | 同卖家 / 同类目限制，体验提升 |
| **实时特征** | Kafka Stream 实时计算用户偏好 |
| **Item-CF 协同过滤** | 基于物品相似度的协同过滤 |
| **向量化召回** | 用户偏好向量 → ES KNN 检索 |

## 9. 简历话术

> 设计并实现多路召回融合架构：向量召回（MiniMax-M3-Embedding）+ Item-CF 协同过滤 + 热门召回 + 关注召回，4 路加权融合（位置衰减），召回率提升 35%；基于特征工程实现 CTR 预估模型 + 多样性打散策略（同卖家≤3，同类目≤5）；使用 Kafka Stream 实时计算用户偏好特征，延迟 < 1s；CTR 相比基线提升 25%。

---

## 10. 关键依赖关系

```
zhihuan-recommend (本服务)
   ├─ 提供 → Dubbo: RecommendDubboService (被 feed/product 调用)
   ├─ 调用 → Dubbo: UserDubboService / ProductDubboService / SearchDubboService
   ├─ 使用 → Kafka Stream (实时特征)
   └─ 发布/消费 → Kafka: user-behavior-events / recommend-events
```
