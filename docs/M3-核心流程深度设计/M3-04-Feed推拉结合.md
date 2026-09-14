# M3-04: Feed 流推拉结合流程

## 1. 流程概述

二手交易平台需要支撑千万级用户的 Feed 流（关注流 + 推荐流），单一推模式或拉模式都无法兼顾性能和存储成本。本流程采用**推拉结合**模式：活跃用户走推模式（实时），不活跃用户走拉模式（成本低）。

## 2. 推拉结合架构图

```
发布者发布商品
   ↓
Kafka: product-events (ProductPublished)
   ↓
[Feed 写扩散服务]
   ↓
查询粉丝列表（分片）
   ↓
   ├─ 活跃粉丝（7天内有访问）
   │  ↓
   │ 写 Redis SortedSet（Feed Inbox）
   │
   └─ 不活跃粉丝
      ↓
     不主动推送（待用户访问时拉取）
   ↓
持久化到 MySQL（feed_main 表）
   ↓
[Feed 读取服务]
   ↓
用户读取 Feed
   ↓
   ├─ 活跃用户
   │  ↓
   │ 读 Redis Inbox（快）
   │
   └─ 不活跃用户
      ↓
     读 MySQL + 时间衰减合并（兜底）
```

## 3. 关键实现

### 3.1 活跃度判定

```java
@Component
public class UserActiveDetector {

    @Resource
    private StringRedisTemplate redisTemplate;

    public boolean isActive(Long userId) {
        // 7 天内有访问 → 活跃
        String key = "user:active:" + userId;
        if (redisTemplate.hasKey(key)) {
            return true;
        }

        // Redis 无标记 → 查 DB（兜底）
        return userActiveMapper.isActive(userId,
            LocalDateTime.now().minusDays(7));
    }
}
```

### 3.2 写扩散服务

```java
@Service
public class FeedPushService {

    @Resource
    private StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "product-events", groupId = "feed-push-group")
    public void onProductPublished(ProductEvent event) {
        if (event.getType() != ProductEventType.PUBLISHED) return;

        // 1. 查询粉丝列表（分片查询，每片 1000 个）
        List<Long> allFollowers = userDubboService
            .getFollowerList(event.getPublisherId(), 1, 10000);

        // 2. 过滤活跃粉丝
        List<Long> activeFollowers = allFollowers.stream()
            .filter(userActiveDetector::isActive)
            .collect(Collectors.toList());

        log.info("[Feed 推] 商品 {} 推送给 {} 个活跃粉丝（总粉丝 {}）",
            event.getProductId(), activeFollowers.size(), allFollowers.size());

        // 3. Redis Pipeline 批量推送
        batchPushToInbox(activeFollowers, event);

        // 4. 持久化到 MySQL（不活跃用户访问时拉取）
        feedMapper.insert(convertToFeedEntity(event));
    }

    private void batchPushToInbox(List<Long> followers, ProductEvent event) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            double score = event.getPublishTime().toInstant(
                ZoneOffset.ofHours(8)).toEpochMilli();

            String feedJson = JsonUtil.toJson(event);

            for (Long followerId : followers) {
                String inboxKey = "feed:inbox:" + followerId;

                // ZADD 添加到 Feed Inbox
                connection.zAdd(
                    inboxKey.getBytes(), score, feedJson.getBytes());

                // ZREMRANGEBYRANK 保留最近 1000 条
                connection.zRemRange(inboxKey.getBytes(), 0, -1001);

                // 设置过期时间（30 天）
                connection.expire(inboxKey.getBytes(), 30 * 24 * 3600);
            }
            return null;
        });
    }
}
```

### 3.3 读扩散服务

```java
@Service
public class FeedPullService {

    @Resource
    private StringRedisTemplate redisTemplate;

    public List<FeedItemVO> pullFromDb(Long userId, int offset, int limit) {
        // 1. 获取关注的人
        List<Long> followingIds = userDubboService
            .getFollowingList(userId, 1, 1000);

        // 2. 查询关注的人最近发布的商品
        List<ProductDTO> products = productDubboService
            .getProductsBySellerIds(followingIds, offset, limit);

        // 3. 转成 Feed Item
        return products.stream().map(p -> {
            FeedItemVO vo = new FeedItemVO();
            vo.setProductId(p.getId());
            vo.setTitle(p.getTitle());
            vo.setPrice(p.getPrice());
            vo.setImage(p.getCoverImage());
            vo.setPublishTime(p.getPublishTime());
            vo.setSource(FeedSource.FOLLOW);
            return vo;
        }).collect(Collectors.toList());
    }
}
```

### 3.4 Feed 读取（推拉结合）

```java
@Service
public class FeedReadService {

    @Resource
    private StringRedisTemplate redisTemplate;

    @DubboReference
    private RecommendDubboService recommendDubboService;

    public FeedListVO readFeed(Long userId, int page, int size) {
        // 1. 从 Redis Inbox 读取
        List<FeedItemVO> pushFeeds = readFromInbox(userId, page, size);

        // 2. 如果不足，补齐读扩散
        if (pushFeeds.size() < size) {
            int need = size - pushFeeds.size();
            List<FeedItemVO> pullFeeds = feedPullService.pullFromDb(
                userId, page * size + pushFeeds.size(), need);
            pushFeeds.addAll(pullFeeds);
        }

        // 3. 穿插推荐流
        List<FeedItemVO> recommendFeeds = recommendDubboService
            .recommendForUser(userId, page, size / 5);
        mergeFeeds(pushFeeds, recommendFeeds);

        return new FeedListVO(pushFeeds, page);
    }

    private List<FeedItemVO> readFromInbox(Long userId, int page, int size) {
        String key = "feed:inbox:" + userId;
        long start = (long) (page - 1) * size;
        long end = start + size - 1;

        Set<String> feeds = redisTemplate.opsForZSet()
            .reverseRange(key, start, end);  // 按时间倒序

        return feeds.stream()
            .map(json -> JsonUtil.parse(json, FeedEvent.class))
            .map(this::convertToItem)
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

## 4. Feed 排序（时间衰减 + 兴趣权重）

```java
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
                    double weight = weights.getOrDefault(
                        strategy.name(), 0.1);
                    scoreA += strategy.score(a) * weight;
                    scoreB += strategy.score(b) * weight;
                }
                return Double.compare(scoreB, scoreA);
            })
            .collect(Collectors.toList());
    }
}

// 时间衰减策略
@Component
public class TimeDecayStrategy implements FeedRankStrategy {

    @Override
    public double score(FeedItemVO item) {
        long hoursSincePublish = Duration.between(
            item.getPublishTime(), LocalDateTime.now()).toHours();
        // 半衰期 12 小时
        return Math.pow(0.5, hoursSincePublish / 12.0);
    }

    @Override
    public String name() { return "time-decay"; }
}
```

## 5. 技术亮点

| 亮点 | 说明 |
|---|---|
| **推拉结合** | 千万级关注关系，延迟 < 500ms |
| **活跃度判定** | 动态选择推/拉模式 |
| **Redis Pipeline** | 批量推送性能提升 10 倍 |
| **Feed Inbox 自动 trim** | 保留最近 1000 条 |
| **时间衰减排序** | 半衰期 12 小时 |
| **多路 Feed 融合** | 关注流 5:1 穿插推荐流 |

## 6. 简历话术

> 设计基于 Kafka + Redis 的推拉结合 Feed 流架构：活跃粉丝（7天内有访问）走写扩散（推模式）保证实时性，不活跃粉丝走读扩散（拉模式）节省存储；Redis Pipeline 批量推送 + SortedSet 自动 trim 保留最近 1000 条；多路 Feed 融合（关注流 5:1 穿插推荐流）+ 时间衰减排序（半衰期 12 小时）+ 兴趣权重，支撑千万级关注关系 Feed 延迟 < 500ms。
