# M4-03: Kafka 主题规划与可观测性架构

## 1. Kafka 集群架构

### 1.1 集群拓扑

```
[Kafka Cluster: 3 Broker + 3 Controller]
   ├─ Broker-0 (KRaft Controller + Broker)
   ├─ Broker-1 (KRaft Controller + Broker)
   └─ Broker-2 (KRaft Controller + Broker)
```

KRaft 模式（Kafka 3.9 替代 Zookeeper）部署更简单，无需 ZK 集群。

### 1.2 Docker Compose 部署

```yaml
version: "3.8"
services:
  kafka-0:
    image: confluentinc/cp-kafka:7.6.1
    container_name: zhihuan-kafka-0
    environment:
      KAFKA_NODE_ID: 0
      KAFKA_PROCESS_ROLES: "broker,controller"
      KAFKA_CONTROLLER_QUORUM_VOTERS: "0@kafka-0:9093,1@kafka-1:9093,2@kafka-2:9093"
      KAFKA_LISTENERS: "PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093"
      KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://kafka-0:9092"
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: "PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT"
      KAFKA_INTER_BROKER_LISTENER_NAME: "PLAINTEXT"
      KAFKA_CONTROLLER_LISTENER_NAMES: "CONTROLLER"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"
    ports:
      - "9092:9092"
      - "9093:9093"
```

## 2. Kafka 主题规划

### 2.1 主题清单

| Topic | 分区数 | 副本数 | 保留时间 | 用途 | 生产者 | 消费者 |
|---|---|---|---|---|---|---|
| user-events | 6 | 3 | 7 天 | 用户注册等事件 | user | notification, recommend |
| user-follow-events | 6 | 3 | 7 天 | 关注事件 | user | feed |
| credit-events | 3 | 3 | 30 天 | 信用分变更 | trade, audit | user |
| product-events | 6 | 3 | 7 天 | 商品生命周期事件 | product | search, feed, recommend, audit |
| ai-events | 3 | 3 | 7 天 | AI 任务完成事件 | ai | product, trade |
| ai-audit-events | 3 | 3 | 7 天 | AI 审核结果 | ai | product, audit |
| ai-cost-events | 3 | 3 | 30 天 | AI Token 使用 | ai | job |
| order-events | 12 | 3 | 30 天 | 订单全生命周期事件 | trade | user, recommend, feed, ai |
| order-tx-topic | 6 | 3 | 7 天 | 订单事务消息 | trade | kafka 转发 |
| order-delay-topic | 3 | 3 | 7 天 | 订单超时延迟消息 | trade | order-timeout-group |
| refund-events | 3 | 3 | 30 天 | 退款事件 | trade | audit, notification |
| seckill-order-events | 6 | 3 | 1 天 | 秒杀下单事件 | marketing | seckill-group |
| coupon-events | 3 | 3 | 7 天 | 优惠券事件 | marketing | notification, user |
| groupon-events | 3 | 3 | 7 天 | 拼团事件 | marketing | notification |
| bargain-events | 3 | 3 | 7 天 | 砍价事件 | marketing | notification |
| ticket-events | 3 | 3 | 7 天 | 客服工单事件 | ai, notification | admin |
| feed-events | 6 | 3 | 7 天 | Feed 内部事件 | product, user | feed |
| search-events | 6 | 3 | 7 天 | 搜索行为日志 | search | Kafka Stream |
| search-hot-words | 3 | 3 | 30 天 | 热词统计结果 | Kafka Stream | job |
| notification-events | 3 | 3 | 7 天 | 通知事件 | 多服务 | notification |
| refund-events | 3 | 3 | 30 天 | 退款事件 | trade | audit, notification |

### 2.2 分区数设计原则

- **按数据量估算**：日均消息量 / 单分区吞吐（10万/天/分区）
- **按消费者并行度**：分区数 >= 消费者实例数
- **预留扩展**：留 50% 余量

## 3. Kafka 关键设计

### 3.1 消息可靠性保证

| 级别 | acks | 适用场景 |
|---|---|---|
| **at most once** | 0 | 日志收集（允许丢失）|
| **at least once** | 1 / all | **大多数业务场景** |
| **exactly once** | all + 幂等 | **资金相关** |

### 3.2 生产者配置（可靠投递）

```yaml
spring:
  kafka:
    producer:
      acks: all  # 所有副本确认
      retries: 3  # 重试次数
      properties:
        enable.idempotence: true  # 幂等性
        max.in.flight.requests.per.connection: 5
        compression.type: lz4  # 压缩
        linger.ms: 10  # 批量发送
        batch.size: 16384  # 批量大小
      transaction-id-prefix: tx-zhihuan-  # 事务前缀
```

### 3.3 消费者配置

```yaml
spring:
  kafka:
    consumer:
      group-id: zhihuan-default
      auto-offset-reset: earliest
      enable-auto-commit: false  # 手动提交
      max-poll-records: 500  # 单次拉取
      session-timeout: 30000
      properties:
        isolation.level: read_committed  # 事务隔离
        auto.offset.reset: earliest
    listener:
      ack-mode: manual_immediate  # 手动 ACK
      concurrency: 3  # 并发消费者
```

### 3.4 死信队列（DLQ）

```java
@KafkaListener(topics = "order-events", groupId = "order-group")
public void onOrderEvent(OrderEvent event,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset) {
    try {
        processOrderEvent(event);
    } catch (Exception e) {
        log.error("处理订单事件失败", e);

        // 重试 3 次后 → 死信队列
        retryTemplate.execute(ctx -> {
            processOrderEvent(event);
            return null;
        });

        // 发送 DLQ
        kafkaTemplate.send("order-events-dlq",
            String.format("%s-%d-%d", topic, partition, offset),
            JsonUtil.toJson(event));
    }
}
```

## 4. Kafka Stream 实时计算

### 4.1 搜索热词统计（5 分钟窗口）

```java
@Configuration
public class SearchHotWordStreamConfig {

    @Bean
    public KStream<String, String> kStream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream("search-events");

        stream.groupBy((key, value) -> value)
            .windowedBy(TimeWindows.ofSizeWithNoGrace(
                Duration.ofMinutes(5)))
            .count(Materialized.as("search-hot-words-store"))
            .toStream()
            .map((windowedKey, count) -> {
                SearchHotWord hot = new SearchHotWord();
                hot.setKeyword(windowedKey.key());
                hot.setCount(count);
                hot.setWindowStart(LocalDateTime.ofInstant(
                    windowedKey.window().startTime(),
                    ZoneId.systemDefault()));
                hot.setWindowEnd(LocalDateTime.ofInstant(
                    windowedKey.window().endTime(),
                    ZoneId.systemDefault()));
                return new KeyValue<>(windowedKey.key(),
                    JsonUtil.toJson(hot));
            })
            .to("search-hot-words");

        return stream;
    }
}
```

## 5. 可观测性架构

### 5.1 Prometheus + Grafana

```yaml
# docker-compose
prometheus:
  image: prom/prometheus:v2.48.0
  volumes:
    - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana:10.2.0
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
  ports:
    - "3000:3000"
```

#### 5.1.1 Spring Boot Actuator 集成

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
  prometheus:
    metrics:
      export:
        enabled: true
```

#### 5.1.2 自定义业务指标

```java
@Component
public class BusinessMetrics {

    @Resource
    private MeterRegistry registry;

    // 订单创建计数器
    private final Counter orderCreateCounter = Counter.builder(
            "biz.order.create")
        .description("订单创建总数")
        .register(registry);

    // AI 任务耗时
    private final Timer aiTaskTimer = Timer.builder("biz.ai.task.duration")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry);

    public void recordOrderCreate() {
        orderCreateCounter.increment();
    }

    public void recordAiTask(Runnable task) {
        aiTaskTimer.record(task);
    }
}
```

#### 5.1.3 Grafana 看板

| 看板 | 指标 |
|---|---|
| **JVM 看板** | Heap, GC, Thread |
| **应用看板** | QPS, RT, Error Rate |
| **业务看板** | 订单数, GMV, AI 调用数 |
| **中间件看板** | MySQL QPS, Redis 命中率, Kafka Lag |

### 5.2 ELK 日志聚合

```yaml
# docker-compose
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.15.0
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
  ports:
    - "9200:9200"

kibana:
  image: docker.elastic.co/kibana/kibana:8.15.0
  ports:
    - "5601:5601"

logstash:
  image: docker.elastic.co/logstash/logstash:8.15.0
  volumes:
    - ./logstash/pipeline:/usr/share/logstash/pipeline
  ports:
    - "5044:5044"

filebeat:
  image: docker.elastic.co/beats/filebeat:8.15.0
  volumes:
    - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml
    - /var/log/zhihuan:/var/log/zhihuan
```

#### 5.2.1 日志规范

```yaml
# logback-spring.xml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [${spring.application.name},%X{traceId},%X{spanId}] %logger{36} - %msg%n"
```

```java
// 日志使用示例
@Slf4j
@Service
public class OrderService {

    public Long createOrder(OrderCreateDTO dto) {
        log.info("[订单创建] userId={}, productId={}",
            dto.getBuyerId(), dto.getProductId());
        // ...
        log.info("[订单创建成功] orderId={}, orderNo={}",
            order.getId(), order.getOrderNo());
    }
}
```

### 5.3 SkyWalking 链路追踪

```yaml
# Java Agent 启动参数
-javaagent:/path/to/skywalking-agent.jar
-Dskywalking.agent.service_name=zhihuan-user
-Dskywalking.collector.backend_service=skywalking-oap:11800
```

### 5.4 告警配置（AlertManager）

```yaml
# alertmanager.yml
route:
  group_by: [alertname, service]
  routes:
    - match:
        severity: critical
      receiver: pagerduty
    - match:
        severity: warning
      receiver: slack

receivers:
  - name: slack
    slack_configs:
      - channel: "#alerts"
        api_url: "https://hooks.slack.com/..."

  - name: pagerduty
    pagerduty_configs:
      - service_key: "..."
```

## 6. 简历话术

> 搭建完整可观测性体系：Prometheus + Grafana 实现应用指标采集 + 自定义业务指标（订单/AI任务）+ 4 大看板；ELK（Filebeat + Logstash + ES + Kibana）实现日志聚合，统一日志格式包含 TraceId；SkyWalking 实现全链路追踪，故障排查时间从小时级降到分钟级；AlertManager 配置告警分级（critical/warning），critical 级别直接对接 PagerDuty。
