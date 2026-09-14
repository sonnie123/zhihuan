# M4-02: Redis 集群架构与缓存设计

## 1. Redis 集群拓扑

### 1.1 整体架构

```
                [Redis Cluster: 6 主 6 从]
                        |
        ┌───────────────┼───────────────┐
        ↓               ↓               ↓
  Master-0         Master-3         Master-5
  Slave-0          Slave-3          Slave-5
  (slots 0-5460)   (slots 5461-10922)  (slots 10923-16383)
```

### 1.2 Docker Compose 部署

```yaml
version: "3.8"
services:
  redis-node-0:
    image: redis:7.4
    container_name: zhihuan-redis-0
    command: >
      redis-server
      --cluster-enabled yes
      --cluster-config-file nodes.conf
      --cluster-node-timeout 5000
      --appendonly yes
      --port 7000
    ports:
      - "7000:7000"
      - "17000:17000"

  redis-node-1:
    image: redis:7.4
    command: redis-server --cluster-enabled yes --port 7001
    ports:
      - "7001:7001"
      - "17001:17001"

  redis-node-2:
    image: redis:7.4
    command: redis-server --cluster-enabled yes --port 7002
    ports:
      - "7002:7002"
      - "17002:17002"

  redis-node-3:
    image: redis:7.4
    command: redis-server --cluster-enabled yes --port 7003
    ports:
      - "7003:7003"
      - "17003:17003"

  redis-node-4:
    image: redis:7.4
    command: redis-server --cluster-enabled yes --port 7004
    ports:
      - "7004:7004"
      - "17004:17004"

  redis-node-5:
    image: redis:7.4
    command: redis-server --cluster-enabled yes --port 7005
    ports:
      - "7005:7005"
      - "17005:17005"
```

### 1.3 集群创建

```bash
docker exec -it zhihuan-redis-0 redis-cli \
  --cluster create 172.18.0.2:7000 172.18.0.3:7001 172.18.0.4:7002 \
                   172.18.0.5:7003 172.18.0.6:7004 172.18.0.7:7005 \
  --cluster-replicas 1
```

## 2. 缓存 Key 设计规范

### 2.1 Key 命名规范

**格式**：`{业务域}:{数据类型}:{业务标识}:{子键}`

```
user:profile:123              # 用户 123 的资料
user:token:access:123         # 用户 123 的访问 Token
user:active:123               # 用户 123 的活跃标记
follow:123                    # 用户 123 的关注 Set
fans:456                      # 用户 456 的粉丝 Set

product:detail:789            # 商品 789 的详情
product:stock:101             # SKU 101 的库存
product:hot                   # 热门商品排行榜

feed:inbox:123                # 用户 123 的 Feed Inbox

coupon:stock:50               # 优惠券 50 的库存
coupon:user:50                # 已领取优惠券 50 的用户 Set

seckill:stock:200             # 秒杀商品 200 的库存
seckill:user:200              # 秒杀商品 200 的用户 Set

credit:123                    # 用户 123 的信用分
embedding:abc123              # 内容 abc123 的 embedding
rag:retrieve:ORDER:def456     # RAG 检索结果缓存
```

### 2.2 Key 过期策略

| Key 类型 | TTL | 说明 |
|---|---|---|
| 用户资料 | 1 小时 | 缓存兜底，DB 兜底 |
| 商品详情 | 10 分钟 | 热点商品缓存时间长 |
| 库存 | 永久 | 业务手动维护 |
| Feed Inbox | 30 天 | 自动过期 |
| 限流计数 | 1 秒 / 1 分钟 | 滑动窗口 |
| 验证码 | 5 分钟 | 验证码时效 |
| RAG 检索 | 10 分钟 | 减少重复检索 |
| Embedding | 7 天 | 节省 token |

## 4. 三级缓存架构

### 4.1 整体架构

```
用户请求
   ↓
[L1] Nginx Proxy Cache（静态资源 + 热点商品）
   ↓ 未命中
[L2] Caffeine（JVM 本地缓存）
   ↓ 未命中
[L3] Redis Cluster（分布式缓存）
   ↓ 未命中
[L4] MySQL（DB 查询，结果回写 Redis）
```

### 4.2 Caffeine 本地缓存

```java
@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<Long, ProductDetailVO> productCache() {
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .recordStats()
            .build();
    }
}

// 使用示例
@Service
public class ProductDetailService {

    @Resource
    private Cache<Long, ProductDetailVO> productCache;

    @Resource
    private StringRedisTemplate redisTemplate;

    public ProductDetailVO getDetail(Long productId) {
        // L1: Caffeine
        ProductDetailVO cached = productCache.get(productId, this::loadFromRedis);
        return cached;
    }

    private ProductDetailVO loadFromRedis(Long productId) {
        // L2: Redis
        String key = "product:detail:" + productId;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            return JsonUtil.parse(json, ProductDetailVO.class);
        }

        // L3: MySQL
        ProductDetailVO detail = loadFromDb(productId);

        if (detail != null) {
            redisTemplate.opsForValue().set(key,
                JsonUtil.toJson(detail), Duration.ofMinutes(10));
        }
        return detail;
    }
}
```

## 5. 缓存一致性策略

### 5.1 Cache Aside 模式（最常用）

```java
// 读：先读缓存，未命中读 DB，然后回写缓存
public ProductDetailVO getDetail(Long productId) {
    ProductDetailVO cached = cache.get(productId);
    if (cached != null) return cached;

    ProductDetailVO detail = db.get(productId);
    if (detail != null) {
        cache.set(productId, detail, TTL);
    }
    return detail;
}

// 写：先更新 DB，再失效缓存
@Transactional
public void updateProduct(ProductDTO dto) {
    // 1. 更新 DB
    productMapper.updateById(dto);

    // 2. 失效缓存（不是更新缓存！）
    cache.invalidate(dto.getId());
    redisTemplate.delete("product:detail:" + dto.getId());
}
```

### 5.2 缓存穿透（布隆过滤器）

```java
@Component
public class BloomFilterHelper {

    @Resource
    private RedissonClient redissonClient;

    private RBloomFilter<Long> productBloomFilter;

    @PostConstruct
    public void init() {
        productBloomFilter = redissonClient.getBloomFilter("product:bloom");
        productBloomFilter.tryInit(1_000_000L, 0.001);
    }

    public boolean mightContain(Long productId) {
        return productBloomFilter.contains(productId);
    }

    public void add(Long productId) {
        productBloomFilter.add(productId);
    }
}

// 使用：缓存穿透保护
public ProductDetailVO getDetail(Long productId) {
    // 1. 布隆过滤器
    if (!bloomFilterHelper.mightContain(productId)) {
        return null;  // 一定不存在
    }

    // 2. 查缓存
    ProductDetailVO cached = cache.get(productId);
    if (cached != null) return cached;

    // 3. 查 DB
    return loadFromDb(productId);
}
```

### 5.3 缓存雪崩（随机过期时间）

```java
public void cacheProduct(ProductDetailVO detail) {
    String key = "product:detail:" + detail.getId();
    // 基础 TTL 10 分钟 + 随机 0-60 秒
    Duration ttl = Duration.ofMinutes(10)
        .plusSeconds(ThreadLocalRandom.current().nextInt(60));
    redisTemplate.opsForValue().set(key, JsonUtil.toJson(detail), ttl);
}
```

### 5.4 缓存击穿（分布式锁）

```java
public ProductDetailVO getDetailWithLock(Long productId) {
    ProductDetailVO cached = cache.get(productId);
    if (cached != null) return cached;

    // 分布式锁（防击穿）
    String lockKey = "lock:product:" + productId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
        if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
            // 双重检查
            cached = cache.get(productId);
            if (cached != null) return cached;

            ProductDetailVO detail = loadFromDb(productId);
            if (detail != null) {
                cache.set(productId, detail, TTL);
            }
            return detail;
        }
        return null;
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null;
    } finally {
        lock.unlock();
    }
}
```

## 6. Redis 高级应用

### 6.1 分布式锁（Redisson）

```java
@Component
public class DistributedLockHelper {

    @Resource
    private RedissonClient redissonClient;

    public <T> T executeWithLock(String lockKey, int waitSeconds,
                                  int leaseSeconds, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS)) {
                return supplier.get();
            }
            throw new BizException("获取锁失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("加锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 6.2 Redis Lua 脚本（原子操作）

```java
// 秒杀原子扣库存 + 防重复
public boolean seckill(Long userId, Long productId) {
    String stockKey = "seckill:stock:" + productId;
    String userKey = "seckill:user:" + productId;

    String luaScript =
        "if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then " +
        "  return -1 " +
        "end " +
        "local stock = tonumber(redis.call('get', KEYS[1])) " +
        "if stock == nil or stock <= 0 then " +
        "  return 0 " +
        "end " +
        "redis.call('decr', KEYS[1]) " +
        "redis.call('sadd', KEYS[2], ARGV[1]) " +
        "return 1";

    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(luaScript, Long.class),
        Arrays.asList(stockKey, userKey),
        String.valueOf(userId));

    return result != null && result == 1L;
}
```

### 6.3 Feed Inbox（SortedSet）

```java
// 写入 Feed
public void pushFeed(Long userId, FeedEvent event) {
    String key = "feed:inbox:" + userId;
    double score = event.getPublishTime().toInstant(
                ZoneOffset.ofHours(8)).toEpochMilli();

    redisTemplate.opsForZSet().add(key, JsonUtil.toJson(event), score);

    // 保留最近 1000 条
    redisTemplate.opsForZSet().removeRange(key, 0, -1001);
}

// 读取 Feed（按时间倒序）
public List<FeedEvent> readFeed(Long userId, int page, int size) {
    String key = "feed:inbox:" + userId;
    long start = (long) (page - 1) * size;
    long end = start + size - 1;

    Set<String> values = redisTemplate.opsForZSet()
        .reverseRange(key, start, end);

    return values.stream()
        .map(json -> JsonUtil.parse(json, FeedEvent.class))
        .collect(Collectors.toList());
}
```

## 7. 性能监控

| 指标 | 监控项 | 告警阈值 |
|---|---|---|
| 内存使用 | used_memory | > 80% 告警 |
| 连接数 | connected_clients | > 10000 告警 |
| QPS | total_commands_processed | > 100000 告警 |
| 命中率 | keyspace_hit_ratio | < 90% 告警 |
| 大 Key | 内存 > 10MB 的 Key | 告警 |
| 慢查询 | > 10ms 的命令 | 告警 |
| 主从同步 | master_repl_offset | 同步延迟 > 5s |

## 8. 简历话术

> 设计 Redis Cluster 6 主 6 从架构（16384 槽位），支撑千万级 QPS 缓存请求；构建 Caffeine + Redis + MySQL 三级缓存架构，热点商品 QPS 10万+，DB 压力降低 90%；针对缓存穿透（布隆过滤器）/ 雪崩（随机 TTL）/ 击穿（Redisson 分布式锁）三大问题设计完整防护方案；使用 Redis Lua 脚本实现秒杀原子扣库存 + 防重复，结合 SortedSet 实现 Feed Inbox 自动 trim。
