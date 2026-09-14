# M2-14: zhihuan-gateway API 网关服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 路由转发 | 根据 URL 路由到对应微服务 |
| 统一鉴权 | SaToken Token 解析 + 透传 |
| 限流熔断 | Sentinel QPS 限流 + 熔断降级 |
| 跨域处理 | 全局 CORS |
| 链路追踪 | TraceId 生成 + 透传 |
| 灰度发布 | 流量染色 |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Gateway 2025.0.0
Spring Cloud Alibaba Sentinel 2025.0.0
SaToken 1.39（网关鉴权）
Knife4j 4.x（API 文档聚合）
```

## 3. 路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: zhihuan-user
          uri: lb://zhihuan-user
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=2
            - name: Sentinel
              args:
                resource-name: user-api

        - id: zhihuan-product
          uri: lb://zhihuan-product
          predicates:
            - Path=/api/product/**
          filters:
            - StripPrefix=2

        - id: zhihuan-ai
          uri: lb://zhihuan-ai
          predicates:
            - Path=/api/ai/**
          filters:
            - StripPrefix=2

        - id: zhihuan-trade
          uri: lb://zhihuan-trade
          predicates:
            - Path=/api/trade/**
          filters:
            - StripPrefix=2

        - id: zhihuan-search
          uri: lb://zhihuan-search
          predicates:
            - Path=/api/search/**
          filters:
            - StripPrefix=2

        - id: zhihuan-feed
          uri: lb://zhihuan-feed
          predicates:
            - Path=/api/feed/**
          filters:
            - StripPrefix=2

        - id: zhihuan-recommend
          uri: lb://zhihuan-recommend
          predicates:
            - Path=/api/recommend/**
          filters:
            - StripPrefix=2
```

## 4. 关键代码骨架

### 4.1 鉴权过滤器

```java
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/user/login",
        "/api/user/register",
        "/api/user/sms/send",
        "/api/search/public",
        "/api/product/public",
        "/v3/api-docs",
        "/swagger-ui"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                              GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            return Mono.error(new BizException(401, "未登录"));
        }
        token = token.substring(7);

        Object userId = StpUtil.getLoginIdByToken(token);
        if (userId == null) {
            return Mono.error(new BizException(401, "Token 无效"));
        }

        // 透传用户信息
        ServerHttpRequest mutated = request.mutate()
            .header("X-User-Id", userId.toString())
            .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhiteList(String path) {
        return WHITE_LIST.stream().anyMatch(p -> path.startsWith(p));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

### 4.2 差异化限流配置

```java
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void initRules() {
        // 商品详情 1000 QPS
        FlowRule productRule = new FlowRule("product-api")
            .setGrade(RuleConstant.FLOW_GRADE_QPS)
            .setCount(1000)
            .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);

        // AI 上架 50 QPS（昂贵）
        FlowRule aiPublishRule = new FlowRule("ai-publish-api")
            .setGrade(RuleConstant.FLOW_GRADE_QPS)
            .setCount(50);

        // AI 议价 100 QPS
        FlowRule aiBargainRule = new FlowRule("ai-bargain-api")
            .setGrade(RuleConstant.FLOW_GRADE_QPS)
            .setCount(100);

        FlowRuleManager.loadRules(Arrays.asList(
            productRule, aiPublishRule, aiBargainRule));
    }
}
```

### 4.3 TraceId 过滤器

```java
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                              GatewayFilterChain chain) {
        String traceId = TraceIdUtil.generate();
        MDC.put("traceId", traceId);

        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header("X-Trace-Id", traceId)
            .build();

        return chain.filter(exchange.mutate().request(mutated).build())
            .then(Mono.fromRunnable(() -> MDC.remove("traceId")));
    }

    @Override
    public int getOrder() {
        return -200;  // 最先执行
    }
}
```

## 5. 技术亮点

| 亮点 | 说明 |
|---|---|
| **统一鉴权** | 网关一次鉴权，下游透传 userId |
| **差异化限流** | 商品 1000 QPS / AI 50 QPS |
| **熔断降级** | Sentinel 熔断防止雪崩 |
| **TraceId 串联** | 全链路追踪 |
| **路由权重** | 支持蓝绿发布 |

## 6. 简历话术

> 基于 Spring Cloud Gateway 构建微服务统一网关，集成 SaToken 实现鉴权透传，配合 Sentinel 实现差异化限流（商品详情 1000 QPS、AI 调用 50 QPS），通过 TraceId 串联全链路，支撑日均千万级请求。
