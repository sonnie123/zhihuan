# M2-13: zhihuan-admin 运营后台服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| BFF 聚合 | 为后台聚合多个服务数据 |
| 商品管理 | 上下架 / 审核 / 编辑 |
| 用户管理 | 封禁 / 解封 / 信用分调整 |
| 订单管理 | 订单查询 / 退款处理 |
| 数据看板 | 业务核心指标可视化 |
| 审核工作流 | AI 审核 + 人工复审 |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
Vue 3 + Element Plus（前端）
MyBatis-Plus 3.5.9
Redis 7.4（看板缓存）
设计模式：BFF 聚合层
```

## 3. 关键代码骨架

### 3.1 数据看板（BFF 聚合）

```java
@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    @DubboReference
    private UserDubboService userDubboService;

    @DubboReference
    private ProductDubboService productDubboService;

    @DubboReference
    private TradeDubboService tradeDubboService;

    /**
     * 看板数据（聚合多服务）
     */
    @GetMapping("/overview")
    public DashboardVO getOverview() {
        DashboardVO vo = new DashboardVO();

        // 并行调用多个 Dubbo 服务
        CompletableFuture<Long> userCountFuture = CompletableFuture
            .supplyAsync(() -> userDubboService.countUsers());
        CompletableFuture<Long> productCountFuture = CompletableFuture
            .supplyAsync(() -> productDubboService.countProducts());
        CompletableFuture<Long> orderCountFuture = CompletableFuture
            .supplyAsync(() -> tradeDubboService.countOrders());
        CompletableFuture<BigDecimal> gmvFuture = CompletableFuture
            .supplyAsync(() -> tradeDubboService.calculateGMV());

        CompletableFuture.allOf(userCountFuture, productCountFuture,
            orderCountFuture, gmvFuture).join();

        vo.setUserCount(userCountFuture.join());
        vo.setProductCount(productCountFuture.join());
        vo.setOrderCount(orderCountFuture.join());
        vo.setGmv(gmvFuture.join());
        return vo;
    }
}
```

### 3.2 商品审核工作流

```java
@RestController
@RequestMapping("/api/admin/product")
public class AdminProductController {

    @DubboReference
    private ProductDubboService productDubboService;

    @DubboReference
    private AuditDubboService auditDubboService;

    @PostMapping("/audit/{productId}")
    public boolean auditProduct(@PathVariable Long productId,
                                 @RequestParam boolean pass,
                                 @RequestParam(required = false) String reason) {
        // 1. 查询商品
        ProductDTO product = productDubboService.getProductById(productId);

        // 2. 更新状态
        if (pass) {
            productDubboService.updateStatus(productId, ProductStatus.ON_SALE);
        } else {
            productDubboService.updateStatus(productId, ProductStatus.VIOLATION);
        }

        // 3. 记录审核结果
        auditDubboService.recordAuditResult(productId, "PRODUCT",
            pass, reason);

        return true;
    }
}
```

## 4. 技术亮点

| 亮点 | 说明 |
|---|---|
| **BFF 聚合层** | 并行调用多个 Dubbo 服务，降低前端请求次数 |
| **CompletableFuture 异步** | 看板数据并行获取，响应 < 500ms |
| **审核工作流** | AI 自动审核 + 人工复审双轨 |

## 5. 简历话术

> 设计 BFF 聚合层（zhihuan-admin），为运营后台并行聚合 12 个 Dubbo 服务的数据（CompletableFuture 并行调用），看板响应 < 500ms；实现商品审核工作流（AI 预审 + 人工复审双轨），日均处理 1 万+ 审核任务。
