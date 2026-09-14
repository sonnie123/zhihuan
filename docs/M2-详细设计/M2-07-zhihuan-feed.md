# M2-07: zhihuan-feed Feed 流服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 关注流 | 关注用户的商品发布/动态推送 |
| 推荐流 | 整合推荐服务返回个性化 Feed |
| 推拉结合 | 写扩散 + 读扩散混合模式 |
| Feed 排序 | 时间衰减 + 兴趣权重 + 多样性 |
| Feed 缓存 | Redis SortedSet 存储用户 Feed |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
Redis 7.4（Feed 缓存 SortedSet + 关注列表）
Kafka 3.9.1（事件订阅 + Stream 聚合）
设计模式：推拉结合 + 责任链
```

## 3. Feed 架构设计（推拉结合）

### 3.1 整体架构

```
发布者发布动态
   ↓
Kafka: feed-event-topic
   ↓
┌──────────────────────────┐
│ 写扩散（推模式）            │
│ 写入所有粉丝的 Feed Inbox  │
│ - 活跃粉丝（在线 + 高频访问）│
│ - Redis SortedSet         │
└──────────────────────────┘
   ↓
┌──────────────────────────┐
│ 读扩散（拉模式）            │
│ 不活跃粉丝从 DB 拉取        │
│ - 时间衰减合并             │
└──────────────────────────┘
   ↓
用户读取 Feed
   ↓
返回 Timeline
```

### 3.2 推拉判定规则

| 用户类型 | 判定条件 | 模式 |
|---|---|---|
| 活跃用户 | 7 天内有访问 | 写扩散（推）|
| 不活跃用户 | 7 天无访问 | 读扩散（拉）|
| 大 V 粉丝 | 粉丝数 > 10000 | 仅推活跃粉丝 |

## 4. 关键代码骨架

### 4.1 Feed 发布（写扩散）

```java
@Service
public class FeedPublishService {

    @Resource
    private StringRedisTemplate redisTemplate;

    @Resource
    private KafkaTemplate kafkaTemplate;

    /**
     * 发布 Feed（异步推扩散）
     */
    @Override
    public void publishFeed(FeedEvent event) {
        // 1. 查询粉丝列表（分片获取）
        List<Long> followerIds = userDubboService
            .getFollowerList(event.getPublisherId(), 1, 10000);

        // 2. 推送到活跃粉丝的 Feed Inbox
        List<Long> activeFollowers = filterActiveFollowers(followerIds);

        // 3. 批量写入 Redis（Pipeline）
        if (!activeFollowers.isEmpty()) {
            batchPushToInbox(activeFollowers, event);
        }

        // 4. 不活跃粉丝通过读扩散获取，无需主动推送

        // 5. 持久化到 DB（用于读扩散）
        feedMapper.insert(event);
    }

    private List<Long> filterActiveFollowers(List<Long> followerIds) {
        // 通过 Redis 查询在线状态
        List<Long> active = new ArrayList<>();
        for (Long followerId : followerIds) {
            String key = "user:active:" + followerId;
            if (redisTemplate.hasKey(key)) {
                active.add(followerId);
            }
        }
        return active;
    }

    private void batchPushToInbox(List<Long> followers, FeedEvent event) {
        // Redis Pipeline 批量写入
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            String feedJson = JsonUtil.toJson(event);
            for (Long followerId : followers) {
                String inboxKey = "feed:inbox:" + followerId;
                // score = 时间戳（用于排序）
                double score = event.getPublishTime().toInstant(
                    ZoneOffset.ofHours(8)).toEpochMilli();
                connection.zAdd(inboxKey.getBytes(), score,
                    feedJson.getBytes());
                // 限制每个用户 Feed Inbox 大小为 1000
                connection.zRemRangeByRank(inboxKey.getBytes(), 0, -1001);
            }
            return null;
        });
    }
}
```

### 4.2 Feed 读取（推拉结合）

```java
@Service
public class FeedReadService {

    @Resource
    private StringRedisTemplate redisTemplate;

    @DubboReference
    private RecommendDubboService recommendDubboService;

    @DubboReference
    private UserDubboService userDubboService;

    /**
     * 读取 Feed 流（推拉结合）
     */
    @Override
    public FeedListVO readFeed(Long userId, int page, int size) {
        // 1. 从 Redis Inbox 读取（推模式）
        List<FeedItemVO> pushFeeds = readFromInbox(userId, page, size);

        // 2. 如果不足，补齐读扩散（拉模式）
        if (pushFeeds.size() < size) {
            int need = size - pushFeeds.size();
            List<FeedItemVO> pullFeeds = readFromDb(userId,
                page * size + pushFeeds.size(), need);
            pushFeeds.addAll(pullFeeds);
        }

        // 3. 整合推荐流（穿插）
        List<FeedItemVO> recommendFeeds = readRecommendFeed(userId, page, size / 5);
        mergeFeeds(pushFeeds, recommendFeeds);

        return new FeedListVO(pushFeeds, page);
    }

    private List<FeedItemVO> readFromInbox(Long userId, int page, int size) {
        String key = "feed:inbox:" + userId;
        long start = (long) (page - 1) * size;
        long end = start + size - 1;

        // 按时间倒序
        Set<String> feeds = redisTemplate.opsForZSet()
            .reverseRange(key, start, end);

        if (feeds == null) return Collections.emptyList();

        return feeds.stream()
            .map(json -> JsonUtil.parse(json, FeedEvent.class))
            .map(this::convertToItem)
            .collect(Collectors.toList());
    }

    private List<FeedItemVO> readFromDb(Long userId, int offset, int limit) {
        // 查询关注的人发布的商品（按时间倒序）
        List<Long> followingIds = userDubboService
            .getFollowingList(userId, 1, 1000);

        List<FeedItemVO> items = productDubboService
            .getProductsBySellerIds(followingIds, offset, limit);

        return items.stream()
            .map(p -> {
                FeedItemVO vo = new FeedItemVO();
                vo.setProductId(p.getId());
                vo.setTitle(p.getTitle());
                vo.setPrice(p.getPrice());
                vo.setImage(p.getCoverImage());
                vo.setSource(FeedSource.FOLLOW);
                return vo;
            })
            .collect(Collectors.toList());
    }

    private void mergeFeeds(List<FeedItemVO> push,
                            List<FeedItemVO> recommend) {
        // 每 5 条关注流插入 1 条推荐流
        for (int i = 0; i < recommend.size(); i++) {
            int insertPos = (i + 1) * 5;
            if (insertPos <= push.size()) {
                push.add(insertPos, recommend.get(i));
            } else {
                push.add(recommend.get(i));
            }
        }
    }
}
```

### 4.3 Feed 排序（责任链）

```java
// 排序策略接口
public interface FeedRankStrategy {
    double score(FeedItemVO item);
    String name();
}

// 时间衰减策略
@Component
public class TimeDecayStrategy implements FeedRankStrategy {

    @Override
    public String name() { return "time-decay"; }

    @Override
    public double score(FeedItemVO item) {
        long hoursSincePublish = Duration.between(
            item.getPublishTime(), LocalDateTime.now()).toHours();
        // 半衰期 12 小时
        return Math.pow(0.5, hoursSincePublish / 12.0);
    }
}

// 兴趣匹配策略
@Component
public class InterestMatchStrategy implements FeedRankStrategy {

    @Resource
    private StringRedisTemplate redisTemplate;

    @Override
    public String name() { return "interest"; }

    @Override
    public double score(FeedItemVO item) {
        String key = "user:feature:category:" + item.getUserId();
        Double score = redisTemplate.opsForZSet().score(
            key, item.getCategoryId().toString());
        return score == null ? 0.5 : Math.min(1.0, score / 10.0);
    }
}

// 综合排序
@Service
public class FeedRanker {

    @Resource
    private List<FeedRankStrategy> strategies;

    public List<FeedItemVO> rank(List<FeedItemVO> items) {
        Map<String, Double> weights = Map.of(
            "time-decay", 0.5,
            "interest", 0.3,
            "quality", 0.2
        );

        return items.stream()
            .sorted((a, b) -> {
                double scoreA = 0, scoreB = 0;
                for (FeedRankStrategy strategy : strategies) {
                    double weight = weights.getOrDefault(strategy.name(), 0.1);
                    scoreA += strategy.score(a) * weight;
                    scoreB += strategy.score(b) * weight;
                }
                return Double.compare(scoreB, scoreA);
            })
            .collect(Collectors.toList());
    }
}
```

### 4.4 Feed 事件监听

```java
@Component
public class FeedEventListener {

    @KafkaListener(topics = "feed-events", groupId = "feed-service")
    public void onFeedEvent(FeedEvent event) {
        switch (event.getType()) {
            case PRODUCT_PUBLISHED:
                handleProductPublished(event);
                break;
            case USER_FOLLOWED:
                handleUserFollowed(event);
                break;
            case USER_UNFOLLOWED:
                handleUserUnfollowed(event);
                break;
        }
    }

    private void handleProductPublished(FeedEvent event) {
        // 推送到粉丝 Feed Inbox
        feedPublishService.publishFeed(event);
    }
}
```

## 5. Dubbo 接口设计

```java
public interface FeedDubboService {

    // 读取 Feed
    FeedListVO readFeed(Long userId, int page, int size);

    // 发布 Feed
    void publishFeed(FeedEvent event);

    // 用户活跃度上报
    void reportActive(Long userId);
}
```

## 6. Kafka 事件

### 6.1 生产事件

| Topic | 事件 | 消费者 |
|---|---|---|
| feed-events | PRODUCT_PUBLISHED / USER_FOLLOWED | feed |

### 6.2 消费事件

| Topic | 事件 | 处理逻辑 |
|---|---|---|
| product-events | ProductPublished | 触发 Feed 写扩散 |
| user-follow-events | FollowEvent | 更新关注关系缓存 |

## 7. 设计模式应用

| 模式 | 应用场景 |
|---|---|
| **推拉结合** | 活跃用户写扩散，不活跃读扩散 |
| **责任链** | Feed 排序多策略组合 |
| **策略模式** | 不同时间衰减策略 |

## 8. 技术亮点

| 亮点 | 说明 |
|---|---|
| **推拉结合** | 千万级关注关系，延迟 < 500ms |
| **Feed Inbox** | Redis SortedSet 存储，自动 trim |
| **多路 Feed 融合** | 关注流 + 推荐流穿插展示 |
| **时间衰减排序** | 半衰期 12 小时，新鲜度优先 |
| **活跃度判定** | Redis 标记活跃用户，决定推/拉 |

## 9. 简历话术

> 设计基于 Kafka + Redis 的推拉结合 Feed 流架构：活跃粉丝采用写扩散（推模式）保证实时性，不活跃粉丝采用读扩散（拉模式）节省存储；Redis SortedSet 存储 Feed Inbox，自动 trim 保留最近 1000 条；多路 Feed 融合（关注流 5:1 穿插推荐流）+ 时间衰减排序（半衰期 12 小时）+ 兴趣权重，支撑千万级关注关系 Feed 延迟 < 500ms。

---

## 10. 关键依赖关系

```
zhihuan-feed (本服务)
   ├─ 提供 → Dubbo: FeedDubboService (被 gateway/admin 调用)
   ├─ 调用 → Dubbo: UserDubboService / ProductDubboService / RecommendDubboService
   ├─ 使用 → Redis (Feed Inbox)
   └─ 发布/消费 → Kafka: feed-events / product-events / user-follow-events
```
