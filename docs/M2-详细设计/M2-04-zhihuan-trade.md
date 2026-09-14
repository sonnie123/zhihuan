# M2-04: zhihuan-trade 交易服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 订单创建 | 担保订单生成 + 库存预扣 |
| 支付集成 | 模拟支付（实际对接微信/支付宝） |
| 资金担保 | 支付冻结 + 收货放款 |
| 订单状态机 | 多状态流转 + Saga 分布式事务 |
| 物流跟踪 | 模拟物流 + 确认收货 |
| 退款管理 | 退货退款 + 仅退款 |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
MyBatis-Plus 3.5.9
Seata 2.5.0（AT 模式分布式事务）
RocketMQ 5.3.1（订单延迟消息：超时自动关闭）
Redis 7.4（库存预扣 + 订单缓存）
Kafka 3.9.1（订单事件）
设计模式：状态机 + Saga + 责任链
```

## 3. 领域模型

### 3.1 核心实体

```java
// 订单主表
@Data @TableName("order_main")
public class OrderMain {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;              // 订单号（业务唯一）
    private Long buyerId;                // 买家
    private Long sellerId;               // 卖家
    private Long productId;              // 商品ID
    private Long skuId;                  // SKU ID
    private String productTitle;         // 商品快照标题
    private String productImage;         // 商品快照图片
    private BigDecimal productPrice;     // 商品快照价格
    private BigDecimal shippingFee;      // 运费
    private BigDecimal totalAmount;      // 总金额
    private BigDecimal payAmount;        // 实付金额
    private String addressSnapshot;      // 收货地址快照 JSON
    private Integer status;              // 订单状态
    private LocalDateTime payExpireTime; // 支付超时时间
    private LocalDateTime shipExpireTime; // 发货超时时间
    private LocalDateTime confirmExpireTime; // 确认收货超时时间
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime confirmTime;
    private LocalDateTime finishTime;
    @Version
    private Integer version;
}

// 订单状态流转表
@Data @TableName("order_status_log")
public class OrderStatusLog {
    @TableId
    private Long id;
    private Long orderId;
    private Integer fromStatus;
    private Integer toStatus;
    private String operator;
    private String reason;
    private LocalDateTime createTime;
}

// 资金流水表
@Data @TableName("fund_flow")
public class FundFlow {
    @TableId
    private Long id;
    private String flowNo;            // 流水号
    private Long orderId;
    private Long userId;              // 操作人
    private Integer flowType;         // 1冻结 2解冻 3扣款 4退款
    private BigDecimal amount;
    private Integer status;           // 1成功 2失败
    private LocalDateTime createTime;
}

// 物流信息表
@Data @TableName("shipping_info")
public class ShippingInfo {
    @TableId
    private Long id;
    private Long orderId;
    private String company;           // 物流公司
    private String trackingNo;        // 物流单号
    private String currentLocation;   // 当前位置
    private LocalDateTime shipTime;   // 发货时间
    private Integer status;        // 1已发货 2运输中 3已签收
}
```

### 3.2 订单状态机

```
   待支付(1)
      ↓ 支付
   已支付(2)
      ↓ 发货
   已发货(3)
      ↓ 确认收货
   已完成(4)
      ↓ 评价
   已评价(5)

   取消(99)  ← 超时/手动取消
   退款中(91) ← 申请退款
   已退款(92) ← 退款成功
```

## 4. 分布式事务设计（Saga + 本地消息表）

### 4.1 担保交易流程（Saga 模式）

```
下单 → 冻结库存 → 支付冻结 → 发货 → 确认收货 → 放款给卖家
  ↓        ↓          ↓         ↓         ↓          ↓
 子事务1  子事务2   子事务3   子事务4   子事务5    子事务6

任意步骤失败 → 触发补偿事务
```

### 4.2 本地消息表 + RocketMQ 事务消息

```java
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderMessageMapper messageMapper;

    @DubboReference
    private ProductDubboService productDubboService;

    @DubboReference
    private PaymentDubboService paymentDubboService;

    @DubboReference
    private UserDubboService userDubboService;

    /**
     * 创建订单（Saga 起始）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateDTO dto) {
        // 1. 幂等检查
        String idempotentKey = dto.getIdempotentKey();
        OrderMain exist = orderMapper.findByIdempotentKey(idempotentKey);
        if (exist != null) return exist.getId();

        // 2. 查询商品信息
        ProductDTO product = productDubboService.getProductById(dto.getProductId());
        if (product == null || !product.getStatus().equals(3)) {
            throw new BizException("商品不可购买");
        }

        // 3. 预扣库存（Redis）
        boolean stockOk = productDubboService.deductStock(product.getSkuId(), 1);
        if (!stockOk) {
            throw new BizException("库存不足");
        }

        // 4. 创建订单
        OrderMain order = new OrderMain();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(dto.getBuyerId());
        order.setSellerId(product.getSellerId());
        order.setProductId(product.getId());
        order.setProductTitle(product.getTitle());
        order.setProductPrice(product.getPrice());
        order.setTotalAmount(product.getPrice().add(dto.getShippingFee()));
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(15));
        orderMapper.insert(order);

        // 5. 写本地消息表（事务性）
        OrderMessage message = new OrderMessage();
        message.setOrderId(order.getId());
        message.setTopic("order-events");
        message.setEventType("ORDER_CREATED");
        message.setPayload(JsonUtil.toJson(order));
        message.setStatus(0);  // 待发送
        messageMapper.insert(message);

        // 6. 触发事务消息发送（事务提交后）
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rocketMQTemplate.sendMessageInTransaction("order-tx-topic",
                        MessageBuilder.withPayload(message).build(), null);
                }
            });

        // 7. 发送订单超时延迟消息
        rocketMQTemplate.send("order-delay-topic",
            MessageBuilder.withPayload(order.getId())
                .delayTimeLevel(15)  // 15分钟延迟
                .build());

        return order.getId();
    }
}
```

### 4.3 RocketMQ 事务消息监听器

```java
@Component
@RocketMQMessageListener(topic = "order-tx-topic", consumerGroup = "order-tx-group")
public class OrderTxMessageListener implements RocketMQListener<OrderMessage> {

    @Resource
    private OrderMessageMapper messageMapper;

    @Override
    public void onMessage(OrderMessage message) {
        // 标记消息已投递
        messageMapper.updateStatus(message.getId(), 1);
        log.info("Order tx message published: {}", message.getId());
    }
}
```

### 4.4 订单超时自动关闭（延迟消息）

```java
@Component
@RocketMQMessageListener(topic = "order-delay-topic",
    consumerGroup = "order-timeout-group")
public class OrderTimeoutListener implements RocketMQListener<Long> {

    @Resource
    private OrderService orderService;

    @Override
    public void onMessage(Long orderId) {
        // 查询订单当前状态
        OrderMain order = orderMapper.selectById(orderId);
        if (order.getStatus().equals(OrderStatus.PENDING_PAY)) {
            // 仍未支付 → 自动关闭
            orderService.cancelOrder(orderId, "超时自动取消");
        }
    }
}
```

## 5. 关键代码骨架

### 5.1 支付流程（担保交易）

```java
@Service
public class PaymentServiceImpl implements PaymentService {

    @Resource
    private FundFlowMapper fundFlowMapper;

    @DubboReference
    private UserDubboService userDubboService;

    @Override
    @GlobalTransactional(name = "payment-transaction", rollbackFor = Exception.class)
    public boolean payOrder(Long orderId, String paymentMethod) {
        // 1. 查询订单
        OrderMain order = orderMapper.selectById(orderId);
        if (!order.getStatus().equals(OrderStatus.PENDING_PAY)) {
            throw new BizException("订单状态错误");
        }

        // 2. Seata AT 模式分布式事务
        // 2.1 冻结买家资金
        UserAccount buyerAccount = userAccountMapper.selectByUserId(order.getBuyerId());
        if (buyerAccount.getBalance().compareTo(order.getPayAmount()) < 0) {
            throw new BizException("余额不足");
        }
        buyerAccount.setFrozenBalance(
            buyerAccount.getFrozenBalance().add(order.getPayAmount()));
        userAccountMapper.updateById(buyerAccount);

        // 2.2 记录资金流水
        FundFlow flow = new FundFlow();
        flow.setFlowNo(generateFlowNo());
        flow.setOrderId(orderId);
        flow.setUserId(order.getBuyerId());
        flow.setFlowType(1);  // 冻结
        flow.setAmount(order.getPayAmount());
        flow.setStatus(1);
        fundFlowMapper.insert(flow);

        // 2.3 更新订单状态
        order.setStatus(OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 3. 发送订单支付事件
        kafkaTemplate.send("order-events",
            new OrderPaidEvent(orderId, order.getBuyerId(), order.getSellerId(),
                order.getPayAmount()));

        return true;
    }
}
```

### 5.2 确认收货 + 放款

```java
@Service
public class OrderConfirmServiceImpl implements OrderConfirmService {

    @Resource
    private UserAccountMapper accountMapper;

    @Resource
    private FundFlowMapper fundFlowMapper;

    @Override
    @GlobalTransactional(name = "confirm-receipt", rollbackFor = Exception.class)
    public boolean confirmReceipt(Long orderId, Long buyerId) {
        OrderMain order = orderMapper.selectById(orderId);

        // 1. 校验订单
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BizException("无权操作");
        }
        if (!order.getStatus().equals(OrderStatus.SHIPPED)) {
            throw new BizException("订单状态错误");
        }

        // 2. 解冻买家资金 → 转入卖家可用余额
        // 2.1 买家：冻结 -X，可用 +X
        UserAccount buyer = accountMapper.selectByUserId(buyerId);
        buyer.setFrozenBalance(buyer.getFrozenBalance().subtract(order.getPayAmount()));
        accountMapper.updateById(buyer);

        // 2.2 卖家：可用 +X
        UserAccount seller = accountMapper.selectByUserId(order.getSellerId());
        seller.setBalance(seller.getBalance().add(order.getPayAmount()));
        accountMapper.updateById(seller);

        // 2.3 记录资金流水（卖家收款）
        FundFlow flow = new FundFlow();
        flow.setOrderId(orderId);
        flow.setUserId(order.getSellerId());
        flow.setFlowType(3);  // 收款
        flow.setAmount(order.getPayAmount());
        fundFlowMapper.insert(flow);

        // 3. 更新订单状态
        order.setStatus(OrderStatus.FINISHED);
        order.setConfirmTime(LocalDateTime.now());
        order.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 4. 发布订单完成事件
        kafkaTemplate.send("order-events",
            new OrderCompletedEvent(orderId, buyerId, order.getSellerId(),
                order.getPayAmount()));

        // 5. 触发卖家信用加分
        kafkaTemplate.send("credit-events",
            new CreditEvent(order.getSellerId(), CreditAction.ORDER_COMPLETED, 1));

        return true;
    }
}
```

### 5.3 退款流程

```java
@Service
public class RefundServiceImpl implements RefundService {

    @Override
    @GlobalTransactional(name = "refund-transaction", rollbackFor = Exception.class)
    public boolean applyRefund(Long orderId, Long buyerId, RefundReason reason) {
        OrderMain order = orderMapper.selectById(orderId);

        // 1. 校验
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BizException("无权操作");
        }
        if (order.getStatus() < OrderStatus.PAID
            || order.getStatus() > OrderStatus.SHIPPED) {
            throw new BizException("当前状态不支持退款");
        }

        // 2. 创建退款单
        RefundOrder refund = new RefundOrder();
        refund.setOrderId(orderId);
        refund.setRefundAmount(order.getPayAmount());
        refund.setReason(reason);
        refund.setStatus(RefundStatus.PENDING);
        refundMapper.insert(refund);

        // 3. 更新订单状态为「退款中」
        order.setStatus(OrderStatus.REFUNDING);
        orderMapper.updateById(order);

        // 4. 触发卖家审核
        kafkaTemplate.send("refund-events",
            new RefundAppliedEvent(refund.getId(), order.getSellerId()));

        return true;
    }

    @Override
    public boolean approveRefund(Long refundId, Long sellerId) {
        RefundOrder refund = refundMapper.selectById(refundId);

        // 卖家同意 → 直接退款
        if (refund.getSellerApproved()) {
            // 1. 解冻买家资金
            UserAccount buyer = accountMapper.selectByUserId(refund.getBuyerId());
            buyer.setFrozenBalance(
                buyer.getFrozenBalance().subtract(refund.getRefundAmount()));
            accountMapper.updateById(buyer);

            // 2. 记录流水
            FundFlow flow = new FundFlow();
            flow.setOrderId(refund.getOrderId());
            flow.setFlowType(4);  // 退款
            flow.setAmount(refund.getRefundAmount());
            fundFlowMapper.insert(flow);

            // 3. 更新状态
            refund.setStatus(RefundStatus.REFUNDED);
            refundMapper.updateById(refund);

            OrderMain order = orderMapper.selectById(refund.getOrderId());
            order.setStatus(OrderStatus.REFUNDED);
            orderMapper.updateById(order);
        }

        return true;
    }
}
```

### 5.4 物流跟踪

```java
@Service
public class ShippingServiceImpl implements ShippingService {

    @Override
    public boolean shipOrder(Long orderId, Long sellerId, ShippingDTO dto) {
        OrderMain order = orderMapper.selectById(orderId);

        if (!order.getSellerId().equals(sellerId)) {
            throw new BizException("无权操作");
        }
        if (!order.getStatus().equals(OrderStatus.PAID)) {
            throw new BizException("订单未支付");
        }

        // 1. 保存物流信息
        ShippingInfo shipping = new ShippingInfo();
        shipping.setOrderId(orderId);
        shipping.setCompany(dto.getCompany());
        shipping.setTrackingNo(dto.getTrackingNo());
        shipping.setShipTime(LocalDateTime.now());
        shipping.setStatus(1);
        shippingMapper.insert(shipping);

        // 2. 更新订单状态
        order.setStatus(OrderStatus.SHIPPED);
        order.setShipTime(LocalDateTime.now());
        order.setShipExpireTime(LocalDateTime.now().plusDays(10));
        orderMapper.updateById(order);

        // 3. 通知买家
        kafkaTemplate.send("notification-events",
            new NotificationEvent(order.getBuyerId(),
                NotificationType.ORDER_SHIPPED, order.getOrderNo()));

        return true;
    }
}
```

## 6. Dubbo 接口设计

### 6.1 Provider 接口

```java
public interface TradeDubboService {

    // 订单
    Long createOrder(OrderCreateDTO dto);
    OrderDTO getOrderById(Long orderId);
    List<OrderDTO> getBuyerOrders(Long buyerId, Integer status, int page, int size);
    List<OrderDTO> getSellerOrders(Long sellerId, Integer status, int page, int size);

    // 支付
    boolean payOrder(Long orderId, String paymentMethod);
    boolean cancelOrder(Long orderId, String reason);

    // 发货收货
    boolean shipOrder(Long orderId, Long sellerId, ShippingDTO dto);
    boolean confirmReceipt(Long orderId, Long buyerId);

    // 退款
    Long applyRefund(Long orderId, Long buyerId, RefundReason reason);
    boolean approveRefund(Long refundId, Long sellerId);

    // 资金
    UserAccountDTO getUserAccount(Long userId);
}
```

### 6.2 Consumer 接口

```java
@DubboReference
private ProductDubboService productDubboService;  // 预扣库存

@DubboReference
private UserDubboService userDubboService;        // 查询用户

@DubboReference
private NotificationDubboService notificationDubboService;  // 通知
```

## 7. Kafka 事件

### 7.1 生产事件

| Topic | 事件 | 消费者 |
|---|---|---|
| order-events | OrderCreated / OrderPaid / OrderShipped / OrderConfirmed / OrderCompleted | user / recommend / feed / ai |
| refund-events | RefundApplied / RefundCompleted | audit / notification |
| credit-events | CreditEvent | user |

### 7.2 消费事件

| Topic | 事件 | 处理逻辑 |
|---|---|---|
| product-events | ProductOffShelf | 取消未支付订单 |

## 8. 设计模式应用

| 模式 | 应用场景 |
|---|---|
| **状态机** | 订单状态流转 |
| **Saga** | 担保交易分布式事务 |
| **本地消息表** | 订单事件可靠投递 |
| **责任链** | 退款审核：规则 → AI → 人工 |
| **模板方法** | 不同支付方式（微信/支付宝）|

## 9. 技术亮点

| 亮点 | 说明 |
|---|---|
| **Saga 分布式事务** | 担保交易分布式事务，保证资金最终一致 |
| **本地消息表** | 订单事件可靠投递 + 事务一致性 |
| **RocketMQ 延迟消息** | 订单超时自动关闭 |
| **Seata AT 模式** | 资金扣减/解冻的强一致性 |
| **状态机** | 订单状态清晰可控 |
| **资金流水** | 所有资金变动可追溯 |

## 10. 简历话术

> 设计并实现了基于 Saga 模式 + 本地消息表的担保交易分布式事务，整合 Seata AT 模式保证资金扣减强一致性；使用 RocketMQ 延迟消息实现订单超时自动关闭（15 分钟）；基于状态机管理订单全生命周期（待支付/已支付/已发货/已完成/退款中/已退款），状态转换清晰可控；实现资金流水表记录所有资金变动，支持账务追溯。

---

## 11. 关键依赖关系

```
zhihuan-trade (本服务)
   ├─ 提供 → Dubbo: TradeDubboService (被 product/user/im/notification 调用)
   ├─ 调用 → Dubbo: ProductDubboService (库存预扣)
   ├─ 调用 → Dubbo: UserDubboService (用户信息)
   ├─ 调用 → Dubbo: NotificationDubboService (通知)
   ├─ 使用 → Seata AT (分布式事务)
   ├─ 使用 → RocketMQ (延迟消息)
   └─ 发布/消费 → Kafka: order-events / refund-events / credit-events
```
