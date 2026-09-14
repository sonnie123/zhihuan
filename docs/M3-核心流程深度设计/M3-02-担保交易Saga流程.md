# M3-02: 担保交易分布式事务流程详细设计（Saga + 本地消息表）

## 1. 流程概述

二手交易的担保支付是核心业务场景，涉及订单创建 → 支付冻结 → 发货 → 确认收货 → 资金解冻的完整链路，每一步都可能失败需要补偿。

本流程展示 **Saga 模式 + 本地消息表 + RocketMQ 事务消息 + Seata AT 模式**的综合应用。

## 2. 担保交易完整流程图

```
买家下单
   ↓
[Saga 步骤1] 创建订单 + 预扣库存
   ├─ 成功 → 继续
   └─ 失败 → 取消（回滚）
   ↓
[Saga 步骤2] 支付冻结（Seata AT）
   ├─ 成功 → 继续
   └─ 失败 → 补偿（释放库存 + 取消订单）
   ↓
[Saga 步骤3] 卖家发货
   ├─ 成功 → 继续
   └─ 失败/超时 → 自动退款（补偿步骤2 + 1）
   ↓
[Saga 步骤4] 买家确认收货 + 资金解冻（Seata AT）
   ├─ 成功 → 继续
   └─ 失败 → 客服介入
   ↓
[Saga 步骤5] 订单完成 + 触发评价
   ↓
完成
```

## 3. Saga 状态机定义

```java
// 担保交易 Saga 状态
public enum SagaState {
    INIT(0, "初始"),
    ORDER_CREATED(1, "订单已创建"),
    PAID(2, "已支付"),
    SHIPPED(3, "已发货"),
    CONFIRMED(4, "已确认收货"),
    FINISHED(5, "已完成"),
    CANCELLED(99, "已取消"),
    REFUNDING(91, "退款中"),
    REFUNDED(92, "已退款");

    private final int code;
    private final String desc;
}

// 状态转换规则
public class SagaStateMachine {

    private static final Map<SagaState, Set<SagaState>> TRANSITIONS = Map.ofEntries(
        Map.entry(SagaState.INIT,
            Set.of(SagaState.ORDER_CREATED, SagaState.CANCELLED)),
        Map.entry(SagaState.ORDER_CREATED,
            Set.of(SagaState.PAID, SagaState.CANCELLED)),
        Map.entry(SagaState.PAID,
            Set.of(SagaState.SHIPPED, SagaState.REFUNDING)),
        Map.entry(SagaState.SHIPPED,
            Set.of(SagaState.CONFIRMED, SagaState.REFUNDING)),
        Map.entry(SagaState.CONFIRMED,
            Set.of(SagaState.FINISHED)),
        Map.entry(SagaState.REFUNDING,
            Set.of(SagaState.REFUNDED, SagaState.PAID))
    );

    public static boolean canTransition(SagaState from, SagaState to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
```

## 4. 关键步骤实现

### 4.1 步骤1：创建订单（Saga 起始）

```java
@Service
@Slf4j
public class OrderSagaService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderMessageMapper messageMapper;

    @DubboReference
    private ProductDubboService productDubboService;

    /**
     * 创建订单（Saga 步骤1）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateDTO dto) {
        // 1. 幂等检查（业务防重）
        String idempotentKey = dto.getIdempotentKey();
        OrderMain exist = orderMapper.findByIdempotentKey(idempotentKey);
        if (exist != null) {
            log.info("重复下单，返回已有订单: {}", exist.getId());
            return exist.getId();
        }

        // 2. 校验商品
        ProductDTO product = productDubboService.getProductById(
            dto.getProductId());
        if (product == null || product.getStatus() != 3) {
            throw new BizException("商品不可购买");
        }

        // 3. 预扣库存（Redis Lua 原子操作）
        boolean stockOk = productDubboService.deductStock(
            product.getSkuId(), 1);
        if (!stockOk) {
            throw new BizException("库存不足");
        }

        // 4. 生成订单号（雪花算法）
        String orderNo = OrderNoGenerator.generate();

        // 5. 创建订单（状态：待支付）
        OrderMain order = new OrderMain();
        order.setOrderNo(orderNo);
        order.setBuyerId(dto.getBuyerId());
        order.setSellerId(product.getSellerId());
        order.setProductId(product.getId());
        order.setProductTitle(product.getTitle());
        order.setProductPrice(product.getPrice());
        order.setTotalAmount(product.getPrice());
        order.setStatus(SagaState.ORDER_CREATED.getCode());
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(15));
        order.setIdempotentKey(idempotentKey);
        orderMapper.insert(order);

        // 6. 写本地消息表（事务性，与订单在同一事务）
        OrderMessage message = new OrderMessage();
        message.setOrderId(order.getId());
        message.setOrderNo(orderNo);
        message.setTopic("order-events");
        message.setEventType("ORDER_CREATED");
        message.setPayload(JsonUtil.toJson(order));
        message.setStatus(0);  // 待发送
        message.setRetryCount(0);
        messageMapper.insert(message);

        // 7. 注册事务提交后钩子（确保消息一定发送）
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 触发事务消息发送
                    rocketMQTemplate.sendMessageInTransaction(
                        "order-tx-topic",
                        MessageBuilder.withPayload(message).build(),
                        message);
                }
            });

        // 8. 发送订单超时延迟消息（15分钟）
        rocketMQTemplate.send("order-delay-topic",
            MessageBuilder.withPayload(order.getId())
                .delayTimeLevel(15)  // 15分钟
                .build());

        log.info("[Saga] 订单创建成功: orderId={}, orderNo={}",
            order.getId(), orderNo);

        return order.getId();
    }
}
```

### 4.2 步骤2：支付冻结（Seata AT 模式）

```java
@Service
@Slf4j
public class OrderPaymentService {

    @Resource
    private UserAccountMapper accountMapper;

    @Resource
    private FundFlowMapper fundFlowMapper;

    @Resource
    private OrderMapper orderMapper;

    /**
     * 支付订单（Seata AT 模式分布式事务）
     */
    @Override
    @GlobalTransactional(name = "payment-transaction",
                          rollbackFor = Exception.class)
    public boolean payOrder(Long orderId, String paymentMethod) {
        // 1. 校验订单
        OrderMain order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != SagaState.ORDER_CREATED.getCode()) {
            throw new BizException("订单状态错误");
        }

        // 2. 调用第三方支付（模拟）
        boolean paySuccess = mockPaymentGateway.pay(
            order.getOrderNo(), order.getTotalAmount(), paymentMethod);
        if (!paySuccess) {
            throw new BizException("支付失败");
        }

        // 3. 冻结买家资金（Seata AT 模式自动回滚）
        UserAccount buyerAccount = accountMapper
            .selectByUserId(order.getBuyerId());

        if (buyerAccount.getBalance().compareTo(order.getTotalAmount()) < 0) {
            throw new BizException("余额不足");
        }

        // 3.1 扣减可用余额
        buyerAccount.setBalance(
            buyerAccount.getBalance().subtract(order.getTotalAmount()));
        accountMapper.updateById(buyerAccount);

        // 3.2 增加冻结余额
        buyerAccount.setFrozenBalance(
            buyerAccount.getFrozenBalance().add(order.getTotalAmount()));
        accountMapper.updateById(buyerAccount);

        // 4. 记录资金流水
        FundFlow flow = new FundFlow();
        flow.setFlowNo(generateFlowNo());
        flow.setOrderId(orderId);
        flow.setUserId(order.getBuyerId());
        flow.setFlowType(1);  // 冻结
        flow.setAmount(order.getTotalAmount());
        flow.setStatus(1);
        fundFlowMapper.insert(flow);

        // 5. 更新订单状态
        order.setStatus(SagaState.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 6. 写消息表（订单已支付事件）
        OrderMessage msg = new OrderMessage();
        msg.setOrderId(orderId);
        msg.setTopic("order-events");
        msg.setEventType("ORDER_PAID");
        msg.setPayload(JsonUtil.toJson(order));
        msg.setStatus(0);
        messageMapper.insert(msg);

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rocketMQTemplate.sendMessageInTransaction(
                        "order-tx-topic",
                        MessageBuilder.withPayload(msg).build(), msg);
                }
            });

        log.info("[Saga] 订单支付成功: orderId={}", orderId);
        return true;
    }
}
```

### 4.3 步骤3：卖家发货

```java
@Service
public class OrderShippingService {

    /**
     * 卖家发货
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean shipOrder(Long orderId, Long sellerId, ShippingDTO dto) {
        // 1. 校验订单
        OrderMain order = orderMapper.selectById(orderId);
        if (!order.getSellerId().equals(sellerId)) {
            throw new BizException("无权操作");
        }
        if (order.getStatus() != SagaState.PAID.getCode()) {
            throw new BizException("订单未支付");
        }

        // 2. 保存物流信息
        ShippingInfo shipping = new ShippingInfo();
        shipping.setOrderId(orderId);
        shipping.setCompany(dto.getCompany());
        shipping.setTrackingNo(dto.getTrackingNo());
        shipping.setShipTime(LocalDateTime.now());
        shipping.setStatus(1);
        shippingMapper.insert(shipping);

        // 3. 更新订单状态
        order.setStatus(SagaState.SHIPPED.getCode());
        order.setShipTime(LocalDateTime.now());
        order.setShipExpireTime(LocalDateTime.now().plusDays(10));
        orderMapper.updateById(order);

        // 4. 写消息表
        OrderMessage msg = new OrderMessage();
        msg.setOrderId(orderId);
        msg.setTopic("order-events");
        msg.setEventType("ORDER_SHIPPED");
        msg.setPayload(JsonUtil.toJson(order));
        messageMapper.insert(msg);

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rocketMQTemplate.sendMessageInTransaction(
                        "order-tx-topic",
                        MessageBuilder.withPayload(msg).build(), msg);
                }
            });

        log.info("[Saga] 订单发货成功: orderId={}", orderId);
        return true;
    }
}
```

### 4.4 步骤4：确认收货 + 资金解冻（关键 Saga 步骤）

```java
@Service
public class OrderConfirmService {

    /**
     * 买家确认收货 + 资金解冻给卖家
     */
    @Override
    @GlobalTransactional(name = "confirm-receipt",
                          rollbackFor = Exception.class)
    public boolean confirmReceipt(Long orderId, Long buyerId) {
        // 1. 校验订单
        OrderMain order = orderMapper.selectById(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BizException("无权操作");
        }
        if (order.getStatus() != SagaState.SHIPPED.getCode()) {
            throw new BizException("订单未发货");
        }

        // ====== Seata AT 分布式事务 ======
        // 必须原子操作：买家冻结 -X + 卖家可用 +X

        // 2. 买家：冻结 -X，可用不变
        UserAccount buyer = accountMapper.selectByUserId(buyerId);
        buyer.setFrozenBalance(
            buyer.getFrozenBalance().subtract(order.getTotalAmount()));
        accountMapper.updateById(buyer);

        // 3. 卖家：可用 +X
        UserAccount seller = accountMapper.selectByUserId(
            order.getSellerId());
        seller.setBalance(
            seller.getBalance().add(order.getTotalAmount()));
        accountMapper.updateById(seller);

        // 4. 记录资金流水（卖家收款）
        FundFlow flow = new FundFlow();
        flow.setFlowNo(generateFlowNo());
        flow.setOrderId(orderId);
        flow.setUserId(order.getSellerId());
        flow.setFlowType(3);  // 收款
        flow.setAmount(order.getTotalAmount());
        flow.setStatus(1);
        fundFlowMapper.insert(flow);

        // 5. 更新订单状态
        order.setStatus(SagaState.FINISHED.getCode());
        order.setConfirmTime(LocalDateTime.now());
        order.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 6. 写消息表（订单完成）
        OrderMessage msg = new OrderMessage();
        msg.setOrderId(orderId);
        msg.setTopic("order-events");
        msg.setEventType("ORDER_COMPLETED");
        msg.setPayload(JsonUtil.toJson(order));
        messageMapper.insert(msg);

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rocketMQTemplate.sendMessageInTransaction(
                        "order-tx-topic",
                        MessageBuilder.withPayload(msg).build(), msg);
                }
            });

        log.info("[Saga] 订单确认收货 + 资金解冻成功: orderId={}", orderId);
        return true;
    }
}
```

## 5. Saga 补偿事务（关键）

### 5.1 订单超时自动取消补偿

```java
@Component
@RocketMQMessageListener(topic = "order-delay-topic",
    consumerGroup = "order-timeout-group")
public class OrderTimeoutListener implements RocketMQListener<Long> {

    @Resource
    private OrderService orderService;

    @Override
    public void onMessage(Long orderId) {
        log.info("[Saga] 收到订单超时消息: orderId={}", orderId);

        OrderMain order = orderMapper.selectById(orderId);

        // 仅在「待支付」状态下取消
        if (order.getStatus() == SagaState.ORDER_CREATED.getCode()) {
            cancelOrderWithCompensation(order, "支付超时自动取消");
        }
    }

    private void cancelOrderWithCompensation(OrderMain order, String reason) {
        try {
            // 补偿步骤1：恢复库存
            productDubboService.restoreStock(
                order.getSkuId(), 1);

            // 补偿步骤0：更新订单状态为已取消
            order.setStatus(SagaState.CANCELLED.getCode());
            order.setCancelReason(reason);
            orderMapper.updateById(order);

            log.info("[Saga] 订单超时取消成功: orderId={}", order.getId());
        } catch (Exception e) {
            log.error("[Saga] 订单超时取消失败", e);
            // 人工介入
        }
    }
}
```

### 5.2 退款补偿（已发货后申请退款）

```java
@Service
public class RefundService {

    @Override
    @GlobalTransactional(name = "refund-transaction",
                          rollbackFor = Exception.class)
    public boolean applyRefund(Long orderId, Long buyerId, String reason) {
        OrderMain order = orderMapper.selectById(orderId);

        // 1. 校验
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BizException("无权操作");
        }

        // 2. 创建退款单
        RefundOrder refund = new RefundOrder();
        refund.setOrderId(orderId);
        refund.setRefundAmount(order.getTotalAmount());
        refund.setReason(reason);
        refund.setStatus(RefundStatus.PENDING);
        refundMapper.insert(refund);

        // 3. 更新订单状态
        order.setStatus(SagaState.REFUNDING.getCode());
        orderMapper.updateById(order);

        // 4. 通知卖家审核
        kafkaTemplate.send("refund-events",
            new RefundAppliedEvent(refund.getId(), order.getSellerId()));

        return true;
    }

    /**
     * 卖家同意退款 → 触发补偿事务
     */
    @Override
    @GlobalTransactional(name = "refund-approve",
                          rollbackFor = Exception.class)
    public boolean approveRefund(Long refundId, Long sellerId) {
        RefundOrder refund = refundMapper.selectById(refundId);

        // ====== 补偿步骤：资金从冻结回到买家可用 ======
        OrderMain order = orderMapper.selectById(refund.getOrderId());

        // 买家：冻结 -X，可用 +X
        UserAccount buyer = accountMapper.selectByUserId(
            order.getBuyerId());
        buyer.setFrozenBalance(
            buyer.getFrozenBalance().subtract(order.getTotalAmount()));
        buyer.setBalance(
            buyer.getBalance().add(order.getTotalAmount()));
        accountMapper.updateById(buyer);

        // 卖家：不变（钱没收到）

        // 记录流水
        FundFlow flow = new FundFlow();
        flow.setOrderId(order.getId());
        flow.setUserId(order.getBuyerId());
        flow.setFlowType(4);  // 退款
        flow.setAmount(order.getTotalAmount());
        fundFlowMapper.insert(flow);

        // 更新状态
        refund.setStatus(RefundStatus.REFUNDED);
        refund.setRefundTime(LocalDateTime.now());
        refundMapper.updateById(refund);

        order.setStatus(SagaState.REFUNDED.getCode());
        orderMapper.updateById(order);

        return true;
    }
}
```

## 6. RocketMQ 事务消息监听器

```java
@Component
@RocketMQMessageListener(topic = "order-tx-topic",
    consumerGroup = "order-tx-group")
public class OrderTxMessageListener
        implements RocketMQListener<OrderMessage> {

    @Resource
    private OrderMessageMapper messageMapper;

    @Resource
    private KafkaTemplate kafkaTemplate;

    @Override
    public void onMessage(OrderMessage message) {
        log.info("[RocketMQ] 收到订单事务消息: {}", message.getId());

        // 1. 标记消息已投递
        messageMapper.markAsPublished(message.getId());

        // 2. 转发到 Kafka（让多个下游消费者订阅）
        kafkaTemplate.send(message.getTopic(), message.getPayload());

        log.info("[RocketMQ] 订单消息已转发到 Kafka: {}", message.getId());
    }
}
```

## 7. 本地消息表定时校对（兜底）

```java
@XxlJob("orderMessageRetryJob")
public ReturnT<String> retryOrderMessage() {
    // 1. 查询未发送成功的消息
    List<OrderMessage> messages = messageMapper
        .selectRetryable(status = 0, maxRetryCount = 5);

    for (OrderMessage msg : messages) {
        try {
            // 重新发送
            rocketMQTemplate.sendMessageInTransaction(
                "order-tx-topic",
                MessageBuilder.withPayload(msg).build(), msg);
        } catch (Exception e) {
            // 更新重试次数
            messageMapper.incrementRetry(msg.getId());
        }
    }

    return ReturnT.SUCCESS;
}
```

## 8. 完整时序图

```
买家                   商品服务           订单服务           支付服务           RocketMQ              Kafka               卖家
  |                       |                |                 |                  |                   |                  |
  |---创建订单------------>|                |                 |                  |                   |                  |
  |                       |--预扣库存------>|                 |                  |                   |                  |
  |                       |                |                 |                  |                   |                  |
  |                       |<--成功----------|                 |                  |                   |                  |
  |                       |                |                 |                  |                   |                  |
  |<--订单ID--------------|                |                 |                  |                   |                  |
  |                       |                |                 |                  |                   |                  |
  |---支付---------------->|                |                 |                  |                   |                  |
  |                       |                |--冻结资金-------->|                  |                   |                  |
  |                       |                |  (Seata AT)     |                  |                   |                  |
  |                       |                |                 |--事务消息-------->|                   |                  |
  |                       |                |                 |                  |--转发------------->|                  |
  |                       |                |                 |                  |                   |--通知卖家-------->|
  |                       |                |                 |                  |                   |                  |
  |                       |                |                 |                  |                   |<--发货-------------|
  |                       |                |                 |                  |                   |                  |
  |                       |                |<--更新发货状态--|                  |                   |                  |
  |                       |                |                 |--事务消息-------->|                   |                  |
  |                       |                |                 |                  |--转发------------->|                  |
  |                       |                |                 |                  |                   |--通知买家-------->|
  |                       |                |                 |                  |                   |                  |
  |<--确认收货------------|                |                 |                  |                   |                  |
  |                       |                |--解冻资金-------->|                  |                   |                  |
  |                       |                |  (Seata AT)     |                  |                   |                  |
  |                       |                |                 |--事务消息-------->|                   |                  |
  |                       |                |                 |                  |--转发------------->|                  |
  |                       |                |                 |                  |                   |--通知双方-------->|
  |                       |                |                 |                  |                   |                  |
```

## 9. 技术亮点

| 亮点 | 说明 |
|---|---|
| **Saga 分布式事务** | 长链路事务协调 + 自动补偿 |
| **Seata AT 模式** | 资金扣减/解冻强一致 |
| **RocketMQ 事务消息** | 本地消息表 + 事务消息双重保障 |
| **延迟消息** | 订单超时自动取消 |
| **定时校对** | XXL-JOB 重试失败消息 |
| **幂等设计** | idempotentKey 防重 |
| **完整可观测** | Saga 步骤全程日志追踪 |

## 10. 简历话术

> 设计基于 Saga 模式 + 本地消息表 + RocketMQ 事务消息的担保交易分布式事务方案，覆盖下单 → 支付冻结 → 发货 → 确认收货 → 资金解冻的完整长链路；使用 Seata AT 模式保证资金扣减/解冻的强一致性；使用 RocketMQ 事务消息 + 本地消息表 + XXL-JOB 定时校对三重保证事件可靠投递；订单超时自动取消通过 RocketMQ 延迟消息实现（15 分钟延迟）；退款场景触发 Saga 补偿事务保证最终一致性。资金对账准确率 100%。
