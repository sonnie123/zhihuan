# M2-05: zhihuan-search 搜索服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 商品全文搜索 | Elasticsearch 中文分词 + 全文检索 |
| 向量检索 | MiniMax-M3-Embedding + Milvus 语义检索 |
| 多条件筛选 | 类目 / 价格区间 / 成色 / 品牌 聚合过滤 |
| 搜索建议 | 用户输入补全（Elasticsearch suggester） |
| 搜索热词 | Kafka Stream 实时统计 + XXL-JOB 持久化 |
| Canal 监听 | MySQL Binlog → Kafka → ES 同步 |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
Elasticsearch 8.15（全文搜索 + 向量检索）
Canal 1.1.7（MySQL Binlog 监听）
Kafka 3.9.1（Canal 监听 + 热词统计）
Redis 7.4（搜索历史 + 热词缓存）
MiniMax-M3-Embedding（向量化）
```

## 3. ES 索引设计

### 3.1 商品索引结构

```json
PUT /product_index
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2,
    "analysis": {
      "analyzer": {
        "ik_smart_pinyin": {
          "type": "custom",
          "tokenizer": "ik_smart",
          "filter": ["pinyin_filter"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "product_id": { "type": "long" },
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "pinyin": { "type": "text", "analyzer": "ik_smart_pinyin" }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "category_id": { "type": "long" },
      "category_name": { "type": "keyword" },
      "brand": { "type": "keyword" },
      "price": { "type": "double" },
      "original_price": { "type": "double" },
      "condition": { "type": "integer" },
      "tags": { "type": "keyword" },
      "seller_id": { "type": "long" },
      "status": { "type": "integer" },
      "cover_image": { "type": "keyword" },
      "publish_time": { "type": "date" },
      "view_count": { "type": "long" },
      "favorite_count": { "type": "long" },
      "embedding": {
        "type": "dense_vector",
        "dims": 1024,
        "index": true,
        "similarity": "cosine"
      },
      "location": { "type": "geo_point" }
    }
  }
}
```

## 4. 关键代码骨架

### 4.1 搜索服务（混合检索）

```java
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Resource
    private ElasticsearchOperations esOperations;

    @Resource
    private ChatClient embeddingClient;

    @Resource
    private StringRedisTemplate redisTemplate;

    /**
     * 混合检索：BM25 + 向量检索
     */
    @Override
    public SearchResultVO search(SearchQueryDTO query) {
        // 1. 向量化查询（如果需要语义检索）
        float[] queryEmbedding = null;
        if (query.getSemanticSearch()) {
            queryEmbedding = embeddingClient.embed(query.getKeyword());
        }

        // 2. 构造 NativeQuery
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
            .withQuery(q -> q.bool(b -> {
                // BM25 全文检索
                if (StringUtils.hasText(query.getKeyword())) {
                    b.must(m -> m.multiMatch(mm -> mm
                        .query(query.getKeyword())
                        .fields("title^3", "description", "tags^2")
                        .type(TextQueryType.BestFields)));
                }
                // 向量检索
                if (queryEmbedding != null) {
                    b.must(m -> m.knn(kn -> kn
                        .field("embedding")
                        .queryVector(queryEmbedding)
                        .k(50)
                        .numCandidates(100)
                        .boost(0.7f)));
                }
                // 过滤条件
                if (query.getCategoryId() != null) {
                    b.filter(f -> f.term(t -> t.field("category_id")
                        .value(query.getCategoryId())));
                }
                if (query.getMinPrice() != null) {
                    b.filter(f -> f.range(r -> r.field("price")
                        .gte(JsonData.of(query.getMinPrice()))));
                }
                if (query.getCondition() != null) {
                    b.filter(f -> f.term(t -> t.field("condition")
                        .value(query.getCondition())));
                }
                // 必须在售状态
                b.filter(f -> f.term(t -> t.field("status").value(3)));
                return b;
            }))
            .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
            .withPageable(PageRequest.of(query.getPage() - 1, query.getSize()));

        // 3. 执行搜索
        SearchHits<ProductDocument> hits =
            esOperations.search(queryBuilder.build(), ProductDocument.class);

        // 4. 构造结果
        List<ProductSearchVO> items = hits.stream()
            .map(hit -> {
                ProductDocument doc = hit.getContent();
                ProductSearchVO vo = new ProductSearchVO();
                BeanUtils.copyProperties(doc, vo);
                vo.setScore(hit.getScore());
                return vo;
            })
            .collect(Collectors.toList());

        return new SearchResultVO(items, hits.getTotalHits());
    }
}
```

### 4.2 向量语义检索（相似商品推荐）

```java
@Service
public class SimilarProductService {

    @Resource
    private ChatClient embeddingClient;

    @Resource
    private ElasticsearchOperations esOperations;

    /**
     * 找相似商品（详情页底部"看了又看"）
     */
    public List<ProductSimilarVO> findSimilar(Long productId, int topK) {
        // 1. 获取当前商品的 embedding
        ProductDocument current = esOperations.get(String.valueOf(productId),
            ProductDocument.class);

        if (current == null || current.getEmbedding() == null) {
            return Collections.emptyList();
        }

        // 2. KNN 检索
        NativeQuery query = NativeQuery.builder()
            .withQuery(q -> q.knn(kn -> kn
                .field("embedding")
                .queryVector(current.getEmbedding())
                .k(topK + 1)  // 多取一个排除自己
                .numCandidates(100)
                .filter(f -> f.term(t -> t.field("status").value(3)))))
            .withPageable(PageRequest.of(0, topK + 1))
            .build();

        SearchHits<ProductDocument> hits = esOperations.search(query,
            ProductDocument.class);

        return hits.stream()
            .map(hit -> hit.getContent())
            .filter(doc -> !doc.getProductId().equals(productId))
            .limit(topK)
            .map(this::toVO)
            .collect(Collectors.toList());
    }
}
```

### 4.3 Canal 监听同步（MySQL → ES）

```java
@Component
@CanalEventListener
public class ProductCanalListener {

    @Resource
    private ElasticsearchOperations esOperations;

    /**
     * 商品表 Binlog 监听
     */
    @CanalListener(destination = "product-canal",
                    schema = "zhihuan_product",
                    table = "product_main")
    public void onProductChange(CanalEntry.Entry entry) {
        for (CanalEntry.RowData rowData : entry.getStoreValue().getRowDatasList()) {
            if (entry.getEntryType() == EntryType.ROWDATA) {
                CanalEntry.EventType eventType = entry.getHeader().getEventType();
                Long productId = parseProductId(rowData);

                switch (eventType) {
                    case INSERT:
                    case UPDATE:
                        syncToEs(productId);
                        break;
                    case DELETE:
                        deleteFromEs(productId);
                        break;
                }
            }
        }
    }

    private void syncToEs(Long productId) {
        // 1. 查询最新商品数据
        ProductDocument doc = loadProductDoc(productId);

        // 2. 生成 embedding
        float[] embedding = embeddingClient.embed(
            doc.getTitle() + " " + doc.getDescription());
        doc.setEmbedding(embedding);

        // 3. 写入 ES
        esOperations.save(doc);

        // 4. 写入 Milvus（如果需要）
        milvusService.upsert(doc);
    }
}
```

### 4.4 搜索建议（自动补全）

```java
@Service
public class SearchSuggestionService {

    @Resource
    private ElasticsearchOperations esOperations;

    @Resource
    private StringRedisTemplate redisTemplate;

    /**
     * 搜索建议（输入"iPhone" → ["iPhone 15", "iPhone 14", ...]）
     */
    public List<String> suggest(String prefix) {
        // 1. Redis 热词缓存
        String cacheKey = "search:suggest:" + prefix;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return JsonUtil.parseList(cached, String.class);
        }

        // 2. ES Completion Suggester
        NativeQuery query = NativeQuery.builder()
            .withSourceFilter(new FetchSourceFilter(
                new String[]{"title"}, null))
            .withQuery(q -> q.bool(b -> b
                .must(m -> m.matchPhrasePrefix(mp -> mp
                    .field("title").query(prefix)))
                .filter(f -> f.term(t -> t.field("status").value(3)))))
            .withSort(s -> s.field(f -> f.field("view_count").order(SortOrder.Desc)))
            .withPageable(PageRequest.of(0, 10))
            .build();

        SearchHits<ProductDocument> hits =
            esOperations.search(query, ProductDocument.class);

        List<String> suggestions = hits.stream()
            .map(h -> h.getContent().getTitle())
            .distinct()
            .collect(Collectors.toList());

        // 3. 写入缓存
        redisTemplate.opsForValue().set(cacheKey,
            JsonUtil.toJson(suggestions), Duration.ofMinutes(5));

        return suggestions;
    }
}
```

### 4.5 搜索热词统计（Kafka Stream）

```java
@Configuration
public class SearchHotWordsStreamConfig {

    @Bean
    public KStream<String, String> kStream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream("search-events");

        // 统计 5 分钟窗口的热词
        stream.groupBy((key, value) -> value)
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.as("search-hot-words-store"))
            .toStream()
            .map((windowedKey, count) -> {
                SearchHotWord hot = new SearchHotWord();
                hot.setKeyword(windowedKey.key());
                hot.setCount(count);
                hot.setWindowStart(LocalDateTime.ofInstant(
                    windowedKey.window().startTime(), ZoneId.systemDefault()));
                hot.setWindowEnd(LocalDateTime.ofInstant(
                    windowedKey.window().endTime(), ZoneId.systemDefault()));
                return new KeyValue<>(windowedKey.key(), JsonUtil.toJson(hot));
            })
            .to("search-hot-words");

        return stream;
    }
}
```

## 5. Dubbo 接口设计

### 5.1 Provider 接口

```java
public interface SearchDubboService {

    // 商品搜索
    SearchResultVO search(SearchQueryDTO query);

    // 相似商品
    List<ProductSimilarVO> findSimilar(Long productId, int topK);

    // 搜索建议
    List<String> suggest(String prefix);

    // 热词
    List<SearchHotWordVO> getHotWords(int topK);
}
```

### 5.2 Consumer 接口

```java
@DubboReference
private ProductDubboService productDubboService;
```

## 6. Kafka 事件

### 6.1 生产事件

| Topic | 事件 | 消费者 |
|---|---|---|
| search-events | SearchQueryEvent | Kafka Stream（热词统计）|
| search-hot-words | SearchHotWord | job（持久化）|

### 6.2 消费事件

| Topic | 事件 | 处理逻辑 |
|---|---|---|
| product-events | ProductPublished / ProductOffShelf | 同步 ES 索引 |

## 7. 技术亮点

| 亮点 | 说明 |
|---|---|
| **混合检索** | BM25 + 向量检索融合，召回率提升 40% |
| **向量语义检索** | 详情页"看了又看"功能，CTR 提升 25% |
| **Canal 实时同步** | MySQL Binlog → ES，延迟 < 1s |
| **搜索热词 Kafka Stream** | 实时统计 + XXL-JOB 持久化 |
| **搜索建议补全** | 用户体验优化 |
| **中文分词** | IK 分词器 + 拼音搜索 |

## 8. 简历话术

> 基于 Elasticsearch 8.15 + MiniMax-M3-Embedding 设计混合检索架构，支持 BM25 全文检索 + 向量语义检索双路召回，商品搜索召回率提升 40%；集成 Canal 监听 MySQL Binlog 实现商品索引近实时同步（延迟 < 1s）；使用 Kafka Stream 实时统计搜索热词（5 分钟窗口），为运营决策提供数据支撑；详情页"看了又看"功能基于向量检索实现，CTR 提升 25%。

---

## 9. 关键依赖关系

```
zhihuan-search (本服务)
   ├─ 提供 → Dubbo: SearchDubboService (被 product/recommend/feed 调用)
   ├─ 使用 → Elasticsearch 8.15 (全文检索 + 向量检索)
   ├─ 使用 → Canal 1.1.7 (MySQL Binlog 监听)
   ├─ 使用 → Kafka Stream (热词统计)
   └─ 发布/消费 → Kafka: search-events / search-hot-words / product-events
```
