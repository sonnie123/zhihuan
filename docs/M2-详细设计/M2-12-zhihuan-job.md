# M2-12: zhihuan-job 任务调度服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 分布式任务调度 | XXL-JOB 集成 |
| 数据统计 | 用户/商品/订单报表 |
| 缓存预热 | 热点商品预加载 |
| 模型更新 | 推荐模型离线训练 |
| 数据清理 | 过期数据归档 |

## 2. 技术栈

```
Spring Boot 3.5.7
XXL-JOB 2.5.0（任务调度中心）
Kafka 3.9.1（事件触发）
Redis 7.4（分布式任务锁）
MyBatis-Plus 3.5.9
```

## 3. 关键任务清单

```java
@Component
public class JobHandlers {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private TradeMapper tradeMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 1. 缓存预热（每天 6:00 执行）
     */
    @XxlJob("hotProductCacheWarmup")
    public ReturnT<String> warmupHotProducts(String param) {
        try {
            // 查询浏览量 Top 1000 商品
            List<ProductMain> hotProducts = productMapper
                .selectHotProducts(1000);

            // 预热到 Redis
            for (ProductMain product : hotProducts) {
                String key = "product:detail:" + product.getId();
                redisTemplate.opsForValue().set(
                    key, JsonUtil.toJson(product), Duration.ofHours(2));
            }
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            return ReturnT.FAIL;
        }
    }

    /**
     * 2. 订单超时扫描（每分钟）
     */
    @XxlJob("orderTimeoutScan")
    public ReturnT<String> orderTimeoutScan(String param) {
        // 扫描超时未支付订单
        List<OrderMain> timeoutOrders = tradeMapper
            .selectTimeoutOrders(LocalDateTime.now());
        for (OrderMain order : timeoutOrders) {
            // 触发取消逻辑（Kafka 异步）
            kafkaTemplate.send("order-timeout-events",
                new OrderTimeoutEvent(order.getId()));
        }
        return ReturnT.SUCCESS;
    }

    /**
     * 3. 搜索热词持久化（每 10 分钟）
     */
    @XxlJob("searchHotWordPersist")
    public ReturnT<String> persistHotWords(String param) {
        // 从 Redis 同步热词到 DB
        Set<String> hotWords = redisTemplate.opsForZSet()
            .reverseRange("search:hot:words", 0, 99);
        if (hotWords != null) {
            for (String word : hotWords) {
                Double score = redisTemplate.opsForZSet()
                    .score("search:hot:words", word);
                hotWordMapper.upsert(word, score.longValue());
            }
        }
        return ReturnT.SUCCESS;
    }

    /**
     * 4. 数据清理（每天 3:00）
     */
    @XxlJob("dataCleanup")
    public ReturnT<String> cleanupData(String param) {
        // 清理 1 年前的已完结订单
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        int rows = tradeMapper.archiveFinishedOrders(oneYearAgo);
        log.info("Archived {} orders", rows);
        return ReturnT.SUCCESS;
    }

    /**
     * 5. 推荐模型离线训练（每天 4:00）
     */
    @XxlJob("recommendModelTrain")
    public ReturnT<String> trainRecommendModel(String param) {
        // 导出用户行为数据到 S3 / OSS
        // 触发 Spark / Flink 任务训练（演示用 mock）
        log.info("Training recommend model...");
        return ReturnT.SUCCESS;
    }

    /**
     * 6. RAG 知识库重建（每周一次）
     */
    @XxlJob("ragKnowledgeRebuild")
    public ReturnT<String> rebuildRagKnowledge(String param) {
        // 重建向量索引
        knowledgeService.rebuildIndex();
        return ReturnT.SUCCESS;
    }

    /**
     * 7. 优惠券过期处理（每小时）
     */
    @XxlJob("couponExpire")
    public ReturnT<String> expireCoupons(String param) {
        couponMapper.expireCoupons(LocalDateTime.now());
        return ReturnT.SUCCESS;
    }
}
```

## 4. 技术亮点

| 亮点 | 说明 |
|---|---|
| **XXL-JOB 分布式调度** | 任务分片 + 故障转移 |
| **缓存预热** | 热点商品预加载，零冷启动 |
| **订单超时扫描** | 双保险（RocketMQ + XXL-JOB）|
| **数据归档** | 自动清理历史数据 |

## 5. 简历话术

> 基于 XXL-JOB 2.5 构建分布式任务调度平台，统一管理 50+ 定时任务（缓存预热/订单超时/数据统计/RAG 重建等）；支持任务分片 + 故障转移 + 可视化运维；定时缓存预热保证热点商品零冷启动，订单超时扫描作为 RocketMQ 延迟消息的双保险。
