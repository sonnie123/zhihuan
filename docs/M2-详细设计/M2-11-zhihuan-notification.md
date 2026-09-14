# M2-11: zhihuan-notification 通知服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 消息聚合 | Kafka 事件订阅 + 多渠道分发 |
| 短信通知 | 阿里云短信 SDK（mock）|
| 邮件通知 | Spring Mail + 模板 |
| 站内信 | WebSocket 推送 + 持久化 |
| App Push | 极光 / 个推（mock）|
| 模板引擎 | FreeMarker / Thymeleaf |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
Kafka 3.9.1（事件订阅）
Redis 7.4（通知去重 + 限流）
FreeMarker 2.3.x（模板引擎）
WebSocket（站内信推送）
```

## 3. 关键代码骨架

### 3.1 通知服务

```java
@Service
public class NotificationServiceImpl implements NotificationDubboService {

    @Resource
    private SmsSender smsSender;

    @Resource
    private EmailSender emailSender;

    @Resource
    private PushSender pushSender;

    @Resource
    private WebSocketHandler webSocketHandler;

    @Override
    public boolean sendNotification(NotificationDTO dto) {
        // 1. Redis 幂等检查
        String dedupKey = "notify:dedup:" + dto.getUserId() + ":"
            + dto.getType() + ":" + dto.getBizId();
        if (!redisTemplate.opsForValue().setIfAbsent(
            dedupKey, "1", Duration.ofMinutes(5))) {
            return false;  // 已发送
        }

        // 2. 多渠道分发
        List<NotificationChannel> channels = resolveChannels(dto);

        for (NotificationChannel channel : channels) {
            try {
                switch (channel) {
                    case SMS:
                        smsSender.send(dto.getUserId(), dto.getContent());
                        break;
                    case EMAIL:
                        emailSender.send(dto.getUserId(), dto.getContent());
                        break;
                    case PUSH:
                        pushSender.push(dto.getUserId(), dto.getTitle(),
                            dto.getContent());
                        break;
                    case IN_APP:
                        webSocketHandler.pushMessage(dto.getUserId(),
                            new ChatMessage(dto.getTitle(), dto.getContent()));
                        break;
                }
            } catch (Exception e) {
                log.error("Send notification failed: {}", channel, e);
                // 重试 / 降级到站内信
            }
        }

        // 3. 持久化
        notificationMapper.insert(dto);

        return true;
    }

    private List<NotificationChannel> resolveChannels(NotificationDTO dto) {
        // 根据通知类型 + 用户偏好决定渠道
        List<NotificationChannel> channels = new ArrayList<>();
        switch (dto.getType()) {
            case ORDER_PAID:
                channels.add(NotificationChannel.PUSH);
                channels.add(NotificationChannel.IN_APP);
                break;
            case SECURITY:
                channels.add(NotificationChannel.SMS);
                channels.add(NotificationChannel.IN_APP);
                break;
            default:
                channels.add(NotificationChannel.IN_APP);
        }
        return channels;
    }
}
```

### 3.2 Kafka 事件订阅

```java
@Component
public class NotificationEventListener {

    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void onOrderEvent(OrderEvent event) {
        switch (event.getType()) {
            case OrderEventType.PAID:
                sendPaidNotification(event);
                break;
            case OrderEventType.SHIPPED:
                sendShippedNotification(event);
                break;
            case OrderEventType.FINISHED:
                sendFinishedNotification(event);
                break;
        }
    }

    @KafkaListener(topics = "user-events", groupId = "notification-group")
    public void onUserEvent(UserEvent event) {
        if (event.getType() == UserEventType.REGISTERED) {
            sendWelcomeNotification(event);
        }
    }

    private void sendPaidNotification(OrderEvent event) {
        String template = freeMarkerTemplate.process(
            "order_paid.ftl", buildContext(event));
        notificationService.sendNotification(
            NotificationDTO.builder()
                .userId(event.getBuyerId())
                .type(NotificationType.ORDER_PAID)
                .content(template)
                .bizId(event.getOrderId())
                .build());
    }
}
```

## 4. Dubbo 接口设计

```java
public interface NotificationDubboService {
    boolean sendNotification(NotificationDTO dto);
    List<NotificationDTO> getUserNotifications(Long userId, int page, int size);
    boolean markAsRead(Long notificationId, Long userId);
}
```

## 5. 技术亮点

| 亮点 | 说明 |
|---|---|
| **多渠道分发** | 短信 / 邮件 / Push / 站内信 |
| **Kafka 事件订阅** | 解耦订单/用户等事件 |
| **Redis 幂等** | 防止重复通知 |
| **模板引擎** | FreeMarker 模板动态渲染 |

## 6. 简历话术

> 基于 Kafka 事件订阅构建统一通知中心，支持短信 / 邮件 / Push / 站内信多渠道分发；使用 FreeMarker 模板引擎动态渲染通知内容；Redis 实现通知幂等去重 + 限流，保证通知不重复不刷屏。
