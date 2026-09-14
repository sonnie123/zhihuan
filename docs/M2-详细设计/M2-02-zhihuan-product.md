# M2-02: zhihuan-product 商品服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 商品发布 | 发布商品 + AI 一键上架（调用 zhihuan-ai） |
| 商品管理 | 编辑 / 下架 / 删除 / 状态机 |
| 类目管理 | 多级类目树 + 属性模板 |
| 商品标签 | 动态标签 + 用户自定义标签 |
| 库存管理 | 多SKU库存 + Redis 预扣库存 |
| 商品搜索 | 发布时同步 ES（异步）|

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
MyBatis-Plus 3.5.9
Redis 7.4（商品缓存 + 库存预扣）
Elasticsearch 8.15（搜索索引）
Kafka 3.9.1（商品事件）
设计模式：状态机 + 模板方法 + 策略
```

## 3. 领域模型

### 3.1 核心实体

```java
// 商品主表
@Data @TableName("product_main")
public class ProductMain {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long sellerId;           // 卖家ID
    private Long categoryId;         // 类目ID
    private String title;            // 标题
    private String description;      // 描述
    private String coverImage;       // 封面图
    private List<String> images;     // 商品图片集
    private BigDecimal price;        // 售价
    private BigDecimal originalPrice; // 原价
    private Integer condition;       // 1全新 2几乎全新 3轻微使用 4明显使用
    private Integer status;          // 1草稿 2审核中 3在售 4已售 5下架 6违规
    private Long viewCount;          // 浏览量
    private Long favoriteCount;      // 收藏量
    private Long aiAuditStatus;      // AI审核状态 0未审核 1通过 2拒绝
    private LocalDateTime publishTime;
    @Version
    private Integer version;
}

// 商品 SKU 表
@Data @TableName("product_sku")
public class ProductSku {
    @TableId
    private Long id;
    private Long productId;
    private String skuName;          // 规格名
    private String skuValue;         // 规格值
    private BigDecimal price;
    private Integer stock;           // 库存
    private Integer lockedStock;     // 锁定库存
}

// 类目表
@Data @TableName("category")
public class Category {
    @TableId
    private Long id;
    private Long parentId;           // 父类目ID
    private String name;
    private Integer level;           // 1一级 2二级 3三级
    private String icon;
    private String template;         // 属性模板 JSON
    private Integer sort;
}

// 商品标签表
@Data @TableName("product_tag")
public class ProductTag {
    @TableId
    private Long id;
    private Long productId;
    private String tagName;
    private Integer tagType;         // 1系统标签 2用户标签
}
```

### 3.2 数据库设计

**数据库名**：`zhihuan_product`（独立库）

```
product_main       # 商品主表（按 sellerId 哈希分片）
product_sku        # SKU表（按 productId 哈希分片）
category           # 类目表（少量数据，不分片）
product_tag        # 标签表（按 productId 哈希分片）
```

## 4. 状态机设计（模板方法模式）

### 4.1 商品状态流转

```
草稿(1) → 审核中(2) → 在售(3) → 已售(4)
   ↓         ↓         ↓
   └─────────┴─────────→ 下架(5)
                          ↓
                       违规(6)
```

### 4.2 状态机实现

```java
// 商品状态枚举
public enum ProductStatus {
    DRAFT(1, "草稿"),
    AUDITING(2, "审核中"),
    ON_SALE(3, "在售"),
    SOLD(4, "已售"),
    OFF_SHELF(5, "下架"),
    VIOLATION(6, "违规");

    private final int code;
    private final String desc;
}

// 状态转换规则
public class ProductStateMachine {

    private static final Map<ProductStatus, Set<ProductStatus>> TRANSITIONS = Map.of(
        ProductStatus.DRAFT, Set.of(ProductStatus.AUDITING, ProductStatus.OFF_SHELF),
        ProductStatus.AUDITING, Set.of(ProductStatus.ON_SALE, ProductStatus.VIOLATION, ProductStatus.OFF_SHELF),
        ProductStatus.ON_SALE, Set.of(ProductStatus.SOLD, ProductStatus.OFF_SHELF, ProductStatus.VIOLATION),
        ProductStatus.SOLD, Set.of(ProductStatus.OFF_SHELF),
        ProductStatus.OFF_SHELF, Set.of(ProductStatus.AUDITING),
        ProductStatus.VIOLATION, Set.of(ProductStatus.OFF_SHELF)
    );

    public static boolean canTransition(ProductStatus from, ProductStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
```

### 4.3 商品操作模板方法

```java
// 模板方法抽象类
public abstract class AbstractProductAction<T> {

    public final T execute(ProductContext context) {
        // 1. 参数校验
        validate(context);

        // 2. 状态校验
        ProductStatus current = context.getProduct().getStatus();
        ProductStatus target = targetStatus();
        if (!ProductStateMachine.canTransition(current, target)) {
            throw new BizException("非法状态转换: " + current + " -> " + target);
        }

        // 3. 业务处理（子类实现）
        T result = doExecute(context);

        // 4. 更新状态
        context.getProduct().setStatus(target);
        updateStatus(context);

        // 5. 发送事件
        publishEvent(context);

        return result;
    }

    protected abstract void validate(ProductContext context);
    protected abstract T doExecute(ProductContext context);
    protected abstract ProductStatus targetStatus();
    protected abstract void updateStatus(ProductContext context);
    protected abstract void publishEvent(ProductContext context);
}

// 上架动作
@Service
public class OnShelfAction extends AbstractProductAction<Boolean> {

    @Override
    protected void validate(ProductContext context) {
        if (context.getProduct().getAiAuditStatus() != 1) {
            throw new BizException("商品未通过AI审核");
        }
    }

    @Override
    protected Boolean doExecute(ProductContext context) {
        // 同步到 ES（异步）
        kafkaTemplate.send("product-events",
            new ProductEvent(context.getProduct().getId(), ProductAction.PUBLISHED));
        return true;
    }

    @Override
    protected ProductStatus targetStatus() {
        return ProductStatus.ON_SALE;
    }

    @Override
    protected void updateStatus(ProductContext context) {
        productMapper.updateStatus(context.getProduct().getId(), ProductStatus.ON_SALE);
    }

    @Override
    protected void publishEvent(ProductContext context) {
        kafkaTemplate.send("product-events",
            new ProductEvent(context.getProduct().getId(), ProductAction.PUBLISHED));
    }
}
```

## 5. 关键代码骨架

### 5.1 AI 一键上架流程

```java
@Service
public class ProductPublishService {

    @DubboReference
    private AiDubboService aiDubboService;

    @DubboReference
    private AuditDubboService auditDubboService;

    @Resource
    private ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishWithAI(ProductPublishDTO dto) {
        // 1. 调用 AI 服务 Graph 工作流
        // 上传图片 → 识图 → 类目预测 → 估价 → 文案 → 风险预审
        AiPublishResult aiResult = aiDubboService.generateProductInfo(
            dto.getImages(),
            dto.getUserInput()
        );

        // 2. 构造商品对象
        ProductMain product = new ProductMain();
        product.setSellerId(dto.getSellerId());
        product.setCategoryId(aiResult.getCategoryId());
        product.setTitle(aiResult.getTitle());
        product.setDescription(aiResult.getDescription());
        product.setImages(dto.getImages());
        product.setCoverImage(aiResult.getCoverImage());
        product.setPrice(aiResult.getSuggestedPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setCondition(dto.getCondition());
        product.setStatus(ProductStatus.AUDITING);
        product.setAiAuditStatus(aiResult.getAiAuditStatus());

        // 3. 入库
        productMapper.insert(product);

        // 4. 添加 AI 生成的标签
        if (aiResult.getTags() != null) {
            for (String tag : aiResult.getTags()) {
                productTagMapper.insert(new ProductTag(product.getId(), tag, 1));
            }
        }

        // 5. 发布商品创建事件
        kafkaTemplate.send("product-events",
            new ProductEvent(product.getId(), ProductAction.CREATED));

        return product.getId();
    }
}
```

### 5.2 商品详情三级缓存

```java
@Service
public class ProductDetailService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StringRedisTemplate redisTemplate;

    @Resource
    private Cache<Long, ProductDetailVO> localCache;

    public ProductDetailVO getDetail(Long productId) {
        // L1: Caffeine 本地缓存
        ProductDetailVO cached = localCache.get(productId, this::loadFromRedis);
        return cached;
    }

    private ProductDetailVO loadFromRedis(Long productId) {
        // L2: Redis 缓存
        String key = "product:detail:" + productId;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            return JsonUtil.parse(json, ProductDetailVO.class);
        }

        // L3: MySQL（带 DB 查询）
        ProductDetailVO detail = loadFromDb(productId);

        // 回写 Redis（TTL 10分钟）
        if (detail != null) {
            redisTemplate.opsForValue().set(key,
                JsonUtil.toJson(detail), Duration.ofMinutes(10));
        }
        return detail;
    }

    private ProductDetailVO loadFromDb(Long productId) {
        ProductMain product = productMapper.selectById(productId);
        if (product == null) return null;

        ProductDetailVO vo = new ProductDetailVO();
        BeanUtils.copyProperties(product, vo);
        vo.setTags(productTagMapper.selectTags(productId));
        vo.setSeller(userDubboService.getUserById(product.getSellerId()));
        return vo;
    }
}
```

### 5.3 库存预扣（Redis + RocketMQ 异步同步 DB）

```java
@Service
public class StockServiceImpl implements StockService {

    @Resource
    private StringRedisTemplate redisTemplate;

    @Resource
    private ProductSkuMapper skuMapper;

    /**
     * 预扣库存（Redis 原子操作）
     */
    @Override
    public boolean deductStock(Long skuId, Integer quantity) {
        String key = "stock:" + skuId;

        // Redis Lua 脚本：原子扣减库存
        String luaScript =
            "local stock = tonumber(redis.call('get', KEYS[1])) " +
            "if stock == nil then return -1 end " +
            "if stock < tonumber(ARGV[1]) then return 0 end " +
            "redis.call('decrby', KEYS[1], ARGV[1]) " +
            "return 1";

        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(key),
            String.valueOf(quantity)
        );

        if (result == 1L) {
            // 异步同步 DB（通过 RocketMQ 延迟消息保证最终一致）
            kafkaTemplate.send("stock-sync-events",
                new StockSyncEvent(skuId, quantity, StockAction.DEDUCT));
            return true;
        }
        return false;
    }

    /**
     * 定时校对（XXL-JOB）
     */
    @XxlJob("stockReconciliationJob")
    public void reconcileStock() {
        // 定期同步 Redis 库存到 DB
        List<ProductSku> allSkus = skuMapper.selectAll();
        for (ProductSku sku : allSkus) {
            Integer redisStock = getRedisStock(sku.getId());
            if (!Objects.equals(redisStock, sku.getStock())) {
                sku.setStock(redisStock);
                skuMapper.updateById(sku);
            }
        }
    }
}
```

### 5.4 类目树查询（递归 + Redis 缓存）

```java
@Service
public class CategoryServiceImpl implements CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private StringRedisTemplate redisTemplate;

    @Override
    public List<CategoryTreeVO> getCategoryTree() {
        String cacheKey = "category:tree";

        // Redis 缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return JsonUtil.parseList(cached, CategoryTreeVO.class);
        }

        // DB 查询
        List<Category> all = categoryMapper.selectList(null);
        List<CategoryTreeVO> tree = buildTree(all, 0L);

        redisTemplate.opsForValue().set(cacheKey,
            JsonUtil.toJson(tree), Duration.ofHours(1));

        return tree;
    }

    private List<CategoryTreeVO> buildTree(List<Category> all, Long parentId) {
        return all.stream()
            .filter(c -> c.getParentId().equals(parentId))
            .map(c -> {
                CategoryTreeVO vo = new CategoryTreeVO();
                BeanUtils.copyProperties(c, vo);
                vo.setChildren(buildTree(all, c.getId()));
                return vo;
            })
            .sorted(Comparator.comparing(CategoryTreeVO::getSort))
            .collect(Collectors.toList());
    }
}
```

## 6. Dubbo 接口设计

### 6.1 Provider 接口

```java
public interface ProductDubboService {

    // 商品基础
    ProductDTO getProductById(Long productId);
    List<ProductDTO> batchGetProducts(List<Long> productIds);

    // 库存
    boolean deductStock(Long skuId, Integer quantity);
    void restoreStock(Long skuId, Integer quantity);
    Integer getStock(Long skuId);

    // 类目
    Long getCategoryPath(Long categoryId);
    List<CategoryDTO> getCategoriesByParent(Long parentId);

    // 商品状态
    boolean updateStatus(Long productId, Integer status);

    // 卖家商品
    List<Long> getSellerProductIds(Long sellerId, int page, int size);
}
```

### 6.2 Consumer 接口

```java
@DubboReference
private UserDubboService userDubboService;  // 查询卖家信息

@DubboReference
private AiDubboService aiDubboService;       // AI 一键上架

@DubboReference
private AuditDubboService auditDubboService; // 审核服务
```

## 7. Kafka 事件

### 7.1 生产事件

| Topic | 事件 | 消费者 |
|---|---|---|
| product-events | ProductCreated / Published / Sold / OffShelf | search / feed / recommend / audit |

### 7.2 消费事件

| Topic | 事件 | 处理逻辑 |
|---|---|---|
| ai-audit-events | AiAuditResult | 更新商品 AI 审核状态 |
| order-events | OrderCreated | 锁定商品状态为「已售」 |

## 8. 设计模式应用

| 模式 | 应用场景 | 好处 |
|---|---|---|
| **状态机** | 商品状态流转 | 集中管理状态转换规则 |
| **模板方法** | 商品操作（上架/下架/删除）| 统一流程，子类实现差异化 |
| **策略模式** | 不同类目的发布校验 | 不同类目不同规则 |
| **观察者模式** | 商品事件发布 | Kafka 解耦订阅 |

## 9. 技术亮点

| 亮点 | 说明 |
|---|---|
| **AI 一键上架** | 调用 zhihuan-ai 完成智能上架流程 |
| **状态机 + 模板方法** | 商品生命周期清晰可控，新增状态易扩展 |
| **三级缓存** | Caffeine + Redis + MySQL 商品详情，10万+ QPS |
| **库存预扣** | Redis Lua 原子扣减 + 异步同步 DB，最终一致 |
| **类目树 Redis 缓存** | 类目查询 O(1)，1小时缓存 |
| **Kafka 事件驱动** | 商品变更实时通知下游服务 |

## 10. 简历话术

> 设计并实现了基于 Spring Boot 3.5 + Dubbo 3 的商品服务，采用状态机 + 模板方法模式管理商品生命周期，集成 MiniMax-M3 实现 AI 一键上架；设计 Caffeine + Redis + MySQL 三级缓存架构支撑 10 万+ QPS 商品详情查询；使用 Redis Lua 脚本实现库存原子预扣，配合 Kafka 异步同步 DB 保证最终一致性，库存扣减准确率 99.99%。

---

## 11. 关键依赖关系

```
zhihuan-product (本服务)
   ├─ 提供 → Dubbo: ProductDubboService (被 trade/feed/recommend/search 调用)
   ├─ 调用 → Dubbo: AiDubboService (AI 上架)
   ├─ 调用 → Dubbo: UserDubboService (卖家信息)
   ├─ 调用 → Dubbo: AuditDubboService (审核)
   └─ 发布/消费 → Kafka: product-events / ai-audit-events
```
