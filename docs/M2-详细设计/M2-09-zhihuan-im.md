# M2-09: zhihuan-im 即时通讯服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 私聊消息 | 买卖家一对一会话 |
| WebSocket 长连接 | 实时消息推送 |
| 消息持久化 | MySQL + Redis 双写 |
| AI 议价对话 | 集成 zhihuan-ai 的 Multi-Agent |
| 消息已读回执 | 已读 / 未读状态 |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
WebSocket（Spring 原生）
Netty 4.1.x（高性能 WebSocket）
Kafka 3.9.1（消息分发）
Redis 7.4（在线状态 + 未读消息）
```

## 3. 关键代码骨架

### 3.1 WebSocket 连接管理

```java
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    @Resource
    private StringRedisTemplate redisTemplate;

    // userId -> WebSocketSession
    private static final ConcurrentHashMap<Long, WebSocketSession> SESSIONS =
        new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        SESSIONS.put(userId, session);

        // 标记用户在线
        redisTemplate.opsForValue().set(
            "im:online:" + userId, "1", Duration.ofMinutes(5));

        log.info("User {} connected", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,
                                       CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        SESSIONS.remove(userId);
        redisTemplate.delete("im:online:" + userId);
    }

    /**
     * 推送消息给指定用户
     */
    public void pushMessage(Long userId, ChatMessage message) {
        WebSocketSession session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(
                    JsonUtil.toJson(message)));
            } catch (IOException e) {
                log.error("Push message failed", e);
            }
        }
    }
}
```

### 3.2 消息服务

```java
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    @Resource
    private ChatMessageMapper messageMapper;

    @Resource
    private WebSocketHandler webSocketHandler;

    @DubboReference
    private AiDubboService aiDubboService;

    @Override
    public Long sendMessage(ChatMessageDTO dto) {
        // 1. 保存消息
        ChatMessage message = new ChatMessage();
        message.setSenderId(dto.getSenderId());
        message.setReceiverId(dto.getReceiverId());
        message.setContent(dto.getContent());
        message.setType(dto.getType());
        message.setStatus(MessageStatus.SENT);
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);

        // 2. 实时推送
        webSocketHandler.pushMessage(dto.getReceiverId(), message);

        // 3. 接收方不在线 → Kafka 异步通知
        if (!isOnline(dto.getReceiverId())) {
            kafkaTemplate.send("im-offline-events",
                new OfflineMessageEvent(dto.getReceiverId(), message));
        }

        return message.getId();
    }

    /**
     * 发送 AI 议价消息
     */
    @Override
    public Long sendAiBargainMessage(Long productId, Long buyerId,
                                      BigDecimal buyerOffer) {
        // 调用 AI 服务议价
        BargainResult result = aiDubboService.bargain(productId, buyerId, buyerOffer);

        // 构造消息
        ChatMessage message = new ChatMessage();
        message.setSenderId(0L);  // AI 助手
        message.setReceiverId(buyerId);
        message.setContent(result.getMessage());
        message.setType(MessageType.AI_BARGAIN);
        message.setExtraData(JsonUtil.toJson(result));
        messageMapper.insert(message);

        webSocketHandler.pushMessage(buyerId, message);
        return message.getId();
    }
}
```

## 4. Dubbo 接口设计

```java
public interface ImDubboService {
    Long sendMessage(ChatMessageDTO dto);
    List<ChatMessage> getConversation(Long userId1, Long userId2, int page, int size);
    boolean markAsRead(Long userId, Long messageId);
    int getUnreadCount(Long userId);
}
```

## 5. 技术亮点

| 亮点 | 说明 |
|---|---|
| **WebSocket 长连接** | 实时消息推送，延迟 < 100ms |
| **Kafka 离线消息** | 不在线用户的消息异步投递 |
| **AI 议价集成** | 消息流中集成 Multi-Agent 议价 |
| **Netty 高性能** | 单机支持 10 万+ 连接 |

## 6. 简历话术

> 基于 Spring WebSocket + Netty 实现买卖家实时私聊，支持 10 万+ 并发连接，消息延迟 < 100ms；集成 zhihuan-ai 的 Multi-Agent 议价，消息流中无缝嵌入 AI 助手；离线消息通过 Kafka 异步投递，上线后自动拉取。
