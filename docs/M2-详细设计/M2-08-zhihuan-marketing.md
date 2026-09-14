# M2-08: zhihuan-marketing 营销服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 优惠券 | 创建 / 领取 / 使用 / 过期 |
| 限时折扣 | 秒杀活动 + 倒计时 |
| 拼团活动 | 多人拼团 + 自动成团 |
| 砍价活动 | 帮砍一刀 + 实时进度 |
| 营销规则引擎 | 多种优惠叠加 + 互斥规则 |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
MyBatis-Plus 3.5.9
Redis 7.4（库存 + 限流 + 优惠券库存）
Kafka 3.9.1（活动事件）
设计模式：策略 + 责任链 + 模板方法 + 工厂模式
```

## 3. 设计模式组合应用

营销服务是展示**设计模式**的绝佳场景，本服务展示了 4 种模式的组合应用：

| 模式 | 应用 |
|---|---|
| **策略模式** | 优惠券类型（满减/折扣/无门槛）|
| **责任链模式** | 优惠叠加规则校验 |
| **模板方法模式** | 营销活动抽象流程 |
| **工厂模式** | 营销活动工厂 |

## 4. 关键代码骨架

### 4.1 优惠券策略模式

```java
// 优惠券类型接口
public interface CouponStrategy {

    /**
     * 计算优惠金额
     */
    BigDecimal discount(BigDecimal orderAmount, Coupon coupon);

    /**
     * 校验优惠券是否适用
     */
    boolean validate(Coupon coupon, OrderDTO order);

    /**
     * 优惠券类型
     */
    CouponType type();
}

// 满减策略
@Component
public class FullReductionStrategy implements CouponStrategy {

    @Override
    public BigDecimal discount(BigDecimal orderAmount, Coupon coupon) {
        if (orderAmount.compareTo(coupon.getMinAmount()) >= 0) {
            return coupon.getDiscountAmount();
        }
        return BigDecimal.ZERO;
    }

    @Override
    public boolean validate(Coupon coupon, OrderDTO order) {
        return order.getTotalAmount().compareTo(coupon.getMinAmount()) >= 0;
    }

    @Override
    public CouponType type() { return CouponType.FULL_REDUCTION; }
}

// 折扣策略
@Component
public class DiscountStrategy implements CouponStrategy {

    @Override
    public BigDecimal discount(BigDecimal orderAmount, Coupon coupon) {
        // 9.5 折：discount = 95 / 100
        BigDecimal rate = new BigDecimal(coupon.getDiscountRate())
            .divide(new BigDecimal(100));
        return orderAmount.multiply(BigDecimal.ONE.subtract(rate));
    }

    @Override
    public boolean validate(Coupon coupon, OrderDTO order) {
        return true;  // 无门槛
    }

    @Override
    public CouponType type() { return CouponType.DISCOUNT; }
}

// 无门槛策略
@Component
public class NoThresholdStrategy implements CouponStrategy {

    @Override
    public BigDecimal discount(BigDecimal orderAmount, Coupon coupon) {
        return coupon.getDiscountAmount();
    }

    @Override
    public boolean validate(Coupon coupon, OrderDTO order) {
        return order.getTotalAmount().compareTo(coupon.getDiscountAmount()) >= 0;
    }

    @Override
    public CouponType type() { return CouponType.NO_THRESHOLD; }
}

// 策略工厂
@Component
public class CouponStrategyFactory {

    @Resource
    private List<CouponStrategy> strategies;

    public CouponStrategy get(CouponType type) {
        return strategies.stream()
            .filter(s -> s.type() == type)
            .findFirst()
            .orElseThrow(() -> new BizException("不支持的优惠券类型"));
    }
}
```

### 4.2 优惠叠加责任链

```java
// 责任链抽象
public abstract class PromotionHandler {

    protected PromotionHandler next;

    public PromotionHandler link(PromotionHandler next) {
        this.next = next;
        return next;
    }

    public BigDecimal apply(BigDecimal orderAmount, PromotionContext ctx) {
        // 1. 当前处理器执行
        BigDecimal current = doApply(orderAmount, ctx);

        // 2. 继续向下传递
        if (next != null) {
            current = next.apply(current, ctx);
        }
        return current;
    }

    protected abstract BigDecimal doApply(BigDecimal orderAmount,
                                          PromotionContext ctx);
}

// 优惠券处理器
@Component
public class CouponHandler extends PromotionHandler {

    @Resource
    private CouponStrategyFactory factory;

    @Override
    protected BigDecimal doApply(BigDecimal orderAmount, PromotionContext ctx) {
        if (ctx.getCouponId() == null) return orderAmount;
        Coupon coupon = couponMapper.selectById(ctx.getCouponId());
        CouponStrategy strategy = factory.get(coupon.getType());
        if (!strategy.validate(coupon, ctx.getOrder())) {
            throw new BizException("优惠券不满足使用条件");
        }
        return orderAmount.subtract(strategy.discount(orderAmount, coupon));
    }
}

// 满减活动处理器
@Component
public class FullReductionCampaignHandler extends PromotionHandler {

    @Override
    protected BigDecimal doApply(BigDecimal orderAmount, PromotionContext ctx) {
        List<Campaign> campaigns = campaignMapper
            .findActiveFullReduction();
        for (Campaign campaign : campaigns) {
            if (orderAmount.compareTo(campaign.getThreshold()) >= 0) {
                return orderAmount.subtract(campaign.getDiscount());
            }
        }
        return orderAmount;
    }
}

// 限时折扣处理器
@Component
public class FlashSaleHandler extends PromotionHandler {

    @Override
    protected BigDecimal doApply(BigDecimal orderAmount, PromotionContext ctx) {
        FlashSale flashSale = flashSaleMapper
            .findActiveByProduct(ctx.getProductId());
        if (flashSale == null) return orderAmount;

        if (LocalDateTime.now().isAfter(flashSale.getEndTime())) {
            return orderAmount;
        }

        BigDecimal discountPrice = flashSale.getFlashPrice();
        return orderAmount.subtract(ctx.getOrder().getProductPrice()
            .subtract(discountPrice));
    }
}

// 责任链装配
@Configuration
public class PromotionChainConfig {

    @Bean
    public PromotionHandler promotionChain(CouponHandler coupon,
                                           FullReductionCampaignHandler fullRed,
                                           FlashSaleHandler flashSale) {
        // 优惠叠加顺序：限时折扣 → 满减 → 优惠券
        return flashSale.link(fullRed).link(coupon);
    }
}
```

### 4.3 营销活动模板方法

```java
// 营销活动抽象类
public abstract class AbstractMarketingCampaign {

    /**
     * 模板方法（不可重写）
     */
    public final CampaignResult execute(MarketingContext ctx) {
        // 1. 前置校验
        if (!preCheck(ctx)) {
            return CampaignResult.fail("前置校验失败");
        }
        // 2. 库存扣减
        if (!deductStock(ctx)) {
            return CampaignResult.fail("库存不足");
        }
        // 3. 执行业务逻辑
        CampaignResult result = doExecute(ctx);
        // 4. 后置处理
        postProcess(ctx, result);
        // 5. 发布事件
        publishEvent(ctx, result);
        return result;
    }

    protected abstract boolean preCheck(MarketingContext ctx);
    protected abstract boolean deductStock(MarketingContext ctx);
    protected abstract CampaignResult doExecute(MarketingContext ctx);
    protected abstract void postProcess(MarketingContext ctx, CampaignResult result);
    protected abstract void publishEvent(MarketingContext ctx, CampaignResult result);
}

// 拼团活动实现
@Service
public class GrouponCampaign extends AbstractMarketingCampaign {

    @Override
    protected boolean preCheck(MarketingContext ctx) {
        GrouponCampaignDO campaign = (GrouponCampaignDO) ctx.getCampaign();
        return campaign.getStatus() == 1
            && LocalDateTime.now().isBefore(campaign.getEndTime());
    }

    @Override
    protected boolean deductStock(MarketingContext ctx) {
        // Redis 原子扣减拼团库存
        String key = "groupon:stock:" + ctx.getCampaignId();
        Long remain = redisTemplate.opsForValue().decrement(key);
        return remain != null && remain >= 0;
    }

    @Override
    protected CampaignResult doExecute(MarketingContext ctx) {
        // 加入拼团或创建新拼团
        Long groupId = joinOrCreateGroup(ctx);
        return CampaignResult.success(groupId);
    }

    @Override
    protected void postProcess(MarketingContext ctx, CampaignResult result) {
        // 检查是否成团
        checkGroupComplete(ctx.getCampaignId(), result.getGroupId());
    }

    @Override
    protected void publishEvent(MarketingContext ctx, CampaignResult result) {
        kafkaTemplate.send("groupon-events",
            new GrouponEvent(ctx.getUserId(), result.getGroupId()));
    }
}

// 砍价活动实现
@Service
public class BargainCampaign extends AbstractMarketingCampaign {

    @Override
    protected boolean preCheck(MarketingContext ctx) {
        return true;
    }

    @Override
    protected boolean deductStock(MarketingContext ctx) {
        return true;  // 砍价不预扣库存
    }

    @Override
    protected CampaignResult doExecute(MarketingContext ctx) {
        // 帮砍一刀
        BigDecimal currentPrice = bargainService
            .helpBargain(ctx.getCampaignId(), ctx.getUserId(), ctx.getFriendId());
        return CampaignResult.success(currentPrice);
    }

    @Override
    protected void postProcess(MarketingContext ctx, CampaignResult result) {
        // 砍价到底自动下单
        if (isBargainComplete(ctx.getCampaignId())) {
            autoCreateOrder(ctx);
        }
    }

    @Override
    protected void publishEvent(MarketingContext ctx, CampaignResult result) {
        kafkaTemplate.send("bargain-events",
            new BargainEvent(ctx.getUserId(), ctx.getCampaignId(),
                (BigDecimal) result.getData()));
    }
}

// 工厂方法
@Component
public class MarketingCampaignFactory {

    @Resource
    private List<AbstractMarketingCampaign> campaigns;

    public AbstractMarketingCampaign get(CampaignType type) {
        return campaigns.stream()
            .filter(c -> c.supports(type))
            .findFirst()
            .orElseThrow();
    }
}
```

### 4.4 优惠券领取（Redis Lua 防超发）

```java
@Service
public class CouponReceiveService {

    @Override
    public boolean receiveCoupon(Long userId, Long couponId) {
        String stockKey = "coupon:stock:" + couponId;
        String userKey = "coupon:user:" + couponId;

        // Redis Lua 脚本：原子扣减 + 防重复
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

        if (result == 1L) {
            // 异步写入 DB
            kafkaTemplate.send("coupon-events",
                new CouponReceiveEvent(userId, couponId));
            return true;
        } else if (result == -1L) {
            throw new BizException("已领取过该优惠券");
        } else {
            throw new BizException("优惠券已领完");
        }
    }
}
```

### 4.5 秒杀活动（Redis 预扣 + 异步下单）

```java
@Service
public class FlashSaleServiceImpl implements FlashSaleService {

    @Resource
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean seckill(Long userId, Long productId) {
        // 1. Redis Lua 原子扣库存 + 防重复
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

        if (result != 1L) {
            return false;
        }

        // 2. 发送下单事件（异步）
        kafkaTemplate.send("seckill-order-events",
            new SeckillOrderEvent(userId, productId));

        return true;
    }
}

// 秒杀下单消费者
@Component
public class SeckillOrderConsumer {

    @KafkaListener(topics = "seckill-order-events", groupId = "seckill-group")
    public void createOrder(SeckillOrderEvent event) {
        try {
            // 异步创建订单
            tradeDubboService.createFlashSaleOrder(event);
        } catch (Exception e) {
            // 失败回滚 Redis 库存
            redisTemplate.opsForValue().increment(
                "seckill:stock:" + event.getProductId());
            redisTemplate.opsForSet().remove(
                "seckill:user:" + event.getProductId(), event.getUserId());
        }
    }
}
```

## 5. Dubbo 接口设计

```java
public interface MarketingDubboService {

    // 优惠券
    boolean receiveCoupon(Long userId, Long couponId);
    List<CouponDTO> getUserCoupons(Long userId, Integer status);
    BigDecimal calculateDiscount(Long orderId, List<Long> couponIds);

    // 营销活动
    CampaignResult joinCampaign(Long userId, Long campaignId);
    List<CampaignDTO> getActiveCampaigns(Integer type);

    // 秒杀
    boolean seckill(Long userId, Long productId);
}
```

## 6. Kafka 事件

| Topic | 事件 | 说明 |
|---|---|---|
| coupon-events | CouponReceived | 异步入库 |
| seckill-order-events | SeckillOrderEvent | 异步下单 |
| groupon-events | GrouponEvent | 拼团成团通知 |
| bargain-events | BargainEvent | 砍价进度通知 |

## 7. 技术亮点

| 亮点 | 说明 |
|---|---|
| **设计模式 4 重组合** | 策略 + 责任链 + 模板方法 + 工厂，是简历亮点 |
| **优惠叠加** | 责任链模式实现灵活叠加规则 |
| **Redis Lua 防超发** | 原子操作保证秒杀公平 |
| **异步下单** | Kafka 解耦下单流程 |

## 8. 简历话术

> 营销服务采用 4 种设计模式组合：策略模式实现多类型优惠券（满减/折扣/无门槛），责任链模式实现优惠叠加规则（限时折扣→满减→优惠券），模板方法模式抽象营销活动执行流程（拼团/砍价/秒杀），工厂模式统一营销活动创建；使用 Redis Lua 脚本实现秒杀原子扣库存 + 防重复，结合 Kafka 异步下单保证秒杀公平性。

---

## 9. 关键依赖关系

```
zhihuan-marketing (本服务)
   ├─ 提供 → Dubbo: MarketingDubboService (被 trade 调用)
   ├─ 调用 → Dubbo: TradeDubboService (创建订单)
   ├─ 使用 → Redis (优惠券 + 秒杀 + 库存)
   └─ 发布/消费 → Kafka: coupon-events / seckill-order-events / groupon-events
```
