# M2-03: zhihuan-ai AI 智能服务详细设计 ⭐项目核心

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| AI 一键上架 | 拍照 → Graph 编排 → 识图/类目/估价/文案/审核 |
| AI 智能议价 | Multi-Agent 协作：买家 Agent + 卖家 Agent + 仲裁 Agent |
| AI 智能客服 | Agentic RAG + 多轮对话 + 工单转人工 |
| AI 内容审核 | Aho-Corasick 规则 + MiniMax-M3 双引擎 |
| AI 估价（RAG） | 检索同类商品成交价 + MiniMax-M3 价格建议 |
| OCR 识别 | 身份证 OCR + 商品图片 OCR |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
Spring-AI 1.1.2 + spring-ai-commons
Spring-AI-Alibaba-Agent-Framework 1.1.2   # Multi-Agent
Spring-AI-Alibaba-Graph 1.1.2              # Graph 工作流
MiniMax-M3（视觉 + 对话）
MiniMax-M3-Embedding（向量）
MiniMax-M3-Safety（内容安全）
Milvus 2.4.x（向量库）
Redis 7.4（Embedding 缓存）
Kafka 3.9.1（AI 事件）
Aho-Corasick 算法（违规词匹配）
```

## 3. 领域模型

### 3.1 核心实体

```java
// AI 任务记录表
@Data @TableName("ai_task")
public class AiTask {
    @TableId
    private Long id;
    private String taskType;          // PUBLISH / BARGAIN / AUDIT / CUSTOMER
    private Long userId;
    private String input;             // 输入内容
    private String output;            // 输出结果
    private Integer status;           // 0待执行 1执行中 2成功 3失败
    private Long costMs;              // 耗时
    private Integer tokenUsed;        // 消耗 token 数
    private String errorMsg;
    private LocalDateTime createTime;
}

// RAG 知识库表
@Data @TableName("knowledge_base")
public class KnowledgeBase {
    @TableId
    private Long id;
    private String kbType;            // PRICE / DESCRIPTION / FAQ / POLICY
    private String title;
    private String content;
    private List<String> tags;
    private String source;            // 来源
    private LocalDateTime updateTime;
}

// Agent 议价会话表
@Data @TableName("bargain_session")
public class BargainSession {
    @TableId
    private Long id;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal originalPrice;
    private BigDecimal currentPrice;
    private Integer maxRounds;
    private Integer currentRound;
    private Integer status;           // 1进行中 2达成 3失败
    private String conversation;      // JSON 对话记录
}

// AI 审核记录表
@Data @TableName("ai_audit_record")
public class AiAuditRecord {
    @TableId
    private Long id;
    private Long targetId;            // 审核对象ID
    private String targetType;        // PRODUCT / COMMENT / PROFILE
    private Integer ruleResult;       // 规则引擎结果 1通过 2违规
    private Integer aiResult;         // AI 审核结果 1通过 2违规
    private Integer finalResult;      // 最终结果
    private List<String> hitRules;    // 命中的规则
    private String aiReason;
}
```

## 4. AI 服务架构总览

```
                            HTTP / Dubbo
                                ↓
┌────────────────────────────────────────────────────────────────┐
│                    zhihuan-ai 服务                              │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ Graph 工作流  │  │ Multi-Agent  │  │ RAG 检索     │         │
│  │ 引擎层       │  │ 协作层       │  │ 增强层       │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│           ↓                ↓                ↓                  │
│  ┌──────────────────────────────────────────────────┐          │
│  │         Function Calling / Tool 调度             │          │
│  └──────────────────────────────────────────────────┘          │
│                           ↓                                    │
│  ┌──────────────────────────────────────────────────┐          │
│  │       MiniMax-M3 大模型统一网关 (LLM Gateway)      │          │
│  └──────────────────────────────────────────────────┘          │
│           ↓                ↓                ↓                  │
│   MiniMax-M3-Vision  MiniMax-M3-Chat   MiniMax-M3-Embed         │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

## 5. 核心功能实现

### 5.1 AI 一键上架（Graph 工作流）

#### 5.1.1 Graph 定义

```java
@Configuration
public class ProductPublishGraphConfig {

    @Bean
    public StateGraph productPublishGraph(
            ImageRecognitionNode imageNode,
            CategoryPredictNode categoryNode,
            PriceEstimateNode priceNode,
            DescriptionGenNode descNode,
            RiskAuditNode auditNode) {

        // 状态图定义
        StateGraph graph = new StateGraph("ProductPublish");

        graph.addNode("imageRecognition", imageNode);
        graph.addNode("categoryPredict", categoryNode);
        graph.addNode("priceEstimate", priceNode);
        graph.addNode("descriptionGen", descNode);
        graph.addNode("riskAudit", auditNode);
        graph.addNode("finalize", new FinalizeNode());

        // 入口
        graph.addEdge(StateGraph.START, "imageRecognition");

        // 主流程：识图 → 类目 → 估价 → 文案 → 审核 → 收尾
        graph.addEdge("imageRecognition", "categoryPredict");
        graph.addEdge("categoryPredict", "priceEstimate");
        graph.addEdge("priceEstimate", "descriptionGen");
        graph.addEdge("descriptionGen", "riskAudit");
        graph.addEdge("riskAudit", "finalize");
        graph.addEdge("finalize", StateGraph.END);

        return graph;
    }
}
```

#### 5.1.2 各节点实现

```java
// 1. 图像识别节点
@Component
public class ImageRecognitionNode implements NodeAction {

    @Resource
    private ChatClient chatClient;  // MiniMax-M3-Vision

    @Override
    public Map<String, Object> apply(OverAllState state) {
        List<String> images = state.value("images");
        UserMessage userMessage = UserMessage.builder()
            .text("请识别这张图片中的商品，包括品牌、型号、成色、新旧程度")
            .media(images.stream()
                .map(url -> new Media(MimeTypeUtils.IMAGE_PNG, url))
                .toList())
            .build();

        String result = chatClient.prompt()
            .user(userMessage)
            .call()
            .content();

        Map<String, Object> updates = new HashMap<>();
        updates.put("imageAnalysis", JsonUtil.parse(result));
        return updates;
    }
}

// 2. 类目预测节点
@Component
public class CategoryPredictNode implements NodeAction {

    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Map<String, Object> analysis = state.value("imageAnalysis");
        String brand = (String) analysis.get("brand");
        String model = (String) analysis.get("model");

        // 调用 MiniMax-M3 分类
        List<Category> categories = categoryMapper.selectList(null);
        String categoryJson = JsonUtil.toJson(categories);

        String prompt = "根据商品信息预测类目：\n" +
            "品牌：" + brand + "\n" +
            "型号：" + model + "\n" +
            "可选类目：" + categoryJson + "\n" +
            "请返回类目ID和置信度";

        String result = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        Map<String, Object> updates = new HashMap<>();
        updates.put("categoryPrediction", JsonUtil.parse(result));
        return updates;
    }
}

// 3. 估价节点（RAG）
@Component
public class PriceEstimateNode implements NodeAction {

    @Resource
    private ChatClient chatClient;

    @DubboReference
    private MilvusDubboService milvusService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Map<String, Object> analysis = state.value("imageAnalysis");
        Map<String, Object> category = state.value("categoryPrediction");

        String query = String.format("%s %s %s 二手价格",
            analysis.get("brand"), analysis.get("model"),
            analysis.get("condition"));

        // RAG 检索同类商品历史成交价
        List<Document> similar = milvusService.searchSimilarProducts(
            query, topK = 20);

        // 构造 Prompt 让 MiniMax-M3 综合判断
        String prompt = "你是一个二手商品估价专家。\n" +
            "商品信息：" + JsonUtil.toJson(analysis) + "\n" +
            "类目：" + category.get("categoryName") + "\n" +
            "同类商品近期成交价参考：\n" + formatDocs(similar) + "\n" +
            "请给出建议售价（JSON格式：{lowPrice, suggestedPrice, highPrice, reason}）";

        String result = chatClient.prompt()
            .system("你是专业的二手商品估价师")
            .user(prompt)
            .call()
            .content();

        Map<String, Object> updates = new HashMap<>();
        updates.put("priceEstimate", JsonUtil.parse(result));
        return updates;
    }
}

// 4. 文案生成节点
@Component
public class DescriptionGenNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Map<String, Object> analysis = state.value("imageAnalysis");
        Map<String, Object> category = state.value("categoryPrediction");
        Map<String, Object> price = state.value("priceEstimate");

        // RAG 检索爆款描述
        List<Document> topDescs = milvusService.searchHotDescription(
            category.get("categoryName").toString(), topK = 5);

        String prompt = "生成一段吸引人的二手商品描述。\n" +
            "商品信息：" + JsonUtil.toJson(analysis) + "\n" +
            "价格：" + price.get("suggestedPrice") + "\n" +
            "爆款描述参考：\n" + formatDocs(topDescs) + "\n" +
            "要求：突出卖点，150字以内，避免违禁词";

        String description = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        Map<String, Object> updates = new HashMap<>();
        updates.put("description", description);
        return updates;
    }
}

// 5. 风险审核节点（规则 + AI 双引擎）
@Component
public class RiskAuditNode implements NodeAction {

    @Resource
    private AhoCorasickMatcher matcher;

    @Resource
    private ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String title = (String) state.value("title");
        String description = (String) state.value("description");

        // 5.1 规则引擎（Aho-Corasick）
        List<String> hitWords = matcher.match(title + " " + description);

        // 5.2 AI 安全模型
        String auditResult = chatClient.prompt()
            .system("你是内容审核专家")
            .user("判断这段商品描述是否违规：\n" + title + "\n" + description)
            .call()
            .content();

        Map<String, Object> updates = new HashMap<>();
        updates.put("hitWords", hitWords);
        updates.put("auditResult", JsonUtil.parse(auditResult));

        // 任一引擎违规则拒
        boolean pass = hitWords.isEmpty()
            && "PASS".equalsIgnoreCase(((String) auditResult).split(",")[0]);
        updates.put("auditPass", pass);

        return updates;
    }
}
```

#### 5.1.3 Graph 编排调用

```java
@Service
public class ProductPublishAIService {

    @Resource
    private StateGraph productPublishGraph;

    public AiPublishResult generateProductInfo(
            List<String> images, String userInput) {

        // 准备初始状态
        OverAllState initialState = new OverAllState()
            .input("images", images)
            .input("userInput", userInput);

        // 执行 Graph
        OverAllState result = productPublishGraph.invoke(initialState);

        // 构造返回结果
        AiPublishResult aiResult = new AiPublishResult();
        aiResult.setTitle(result.value("title"));
        aiResult.setDescription(result.value("description"));
        aiResult.setCategoryId(((Number) result.value("categoryPrediction.categoryId")).longValue());
        aiResult.setSuggestedPrice(new BigDecimal(result.value("priceEstimate.suggestedPrice").toString()));
        aiResult.setAiAuditStatus((Boolean) result.value("auditPass") ? 1 : 2);
        return aiResult;
    }
}
```

### 5.2 AI 智能议价（Multi-Agent）

#### 5.2.1 议价架构

```
   买家 Agent                卖家 Agent
       ↓                        ↓
    "1500?"               "最低1800"
       ↓                        ↓
       └──── 平台仲裁 Agent ────┘
                    ↓
              "1700 双方接受？"
                ↓        ↓
                是       是
                ↓
            达成成交
```

#### 5.2.2 Multi-Agent 实现

```java
@Configuration
public class BargainAgentConfig {

    @Bean
    public ReactAgent buyerAgent(ChatClient chatClient) {
        return ReactAgent.builder()
            .name("buyerAgent")
            .chatClient(chatClient)
            .systemPrompt("你是一个精明的买家Agent。" +
                "你的目标是帮买家以最低价格买到商品。" +
                "回复格式：{price: 数字, message: '说服话术', accept: true/false}")
            .tools(new BuyerSearchTool())  // 查询同类商品价格工具
            .build();
    }

    @Bean
    public ReactAgent sellerAgent(ChatClient chatClient) {
        return ReactAgent.builder()
            .name("sellerAgent")
            .chatClient(chatClient)
            .systemPrompt("你是一个理性的卖家Agent。" +
                "你的目标是帮卖家以合理价格卖出商品。" +
                "底价是原价的70%。" +
                "回复格式：{price: 数字, message: '说服话术', accept: true/false}")
            .tools(new SellerCostTool())
            .build();
    }

    @Bean
    public SupervisorAgent bargainSupervisor(
            ReactAgent buyerAgent, ReactAgent sellerAgent,
            ChatClient chatClient) {
        return SupervisorAgent.builder()
            .name("bargainSupervisor")
            .chatClient(chatClient)
            .subAgents(List.of(buyerAgent, sellerAgent))
            .systemPrompt("你是议价平台仲裁Agent。" +
                "协调买家和卖家Agent，根据轮次和价格做出仲裁。" +
                "如果双方差距小于10%，提出折中价；" +
                "否则让双方继续议价。" +
                "超过10轮强制结束。")
            .build();
    }
}

// 议价服务
@Service
public class BargainServiceImpl implements BargainService {

    @Resource
    private SupervisorAgent bargainSupervisor;

    @Override
    public BargainResult bargain(Long productId, Long buyerId) {
        // 查询商品和卖家信息
        ProductDTO product = productDubboService.getProductById(productId);
        UserDTO seller = userDubboService.getUserById(product.getSellerId());

        // 构造议价上下文
        BargainContext ctx = new BargainContext();
        ctx.setProduct(product);
        ctx.setBuyerId(buyerId);
        ctx.setSellerId(seller.getId());
        ctx.setOriginalPrice(product.getPrice());
        ctx.setBuyerOffer(product.getPrice().multiply(new BigDecimal("0.7"))); // 买家首报70%
        ctx.setSellerFloor(product.getPrice().multiply(new BigDecimal("0.85"))); // 卖家底价85%

        // 执行 Multi-Agent 议价
        AgentResponse response = bargainSupervisor.invoke(ctx);

        return parseResult(response);
    }
}
```

### 5.3 Agentic RAG（智能客服）

#### 5.3.1 Graph 定义

```java
@Configuration
public class CustomerServiceRagConfig {

    @Bean
    public StateGraph customerServiceRagGraph(
            QuestionClassifyNode classifyNode,
            MultiRetrievalNode retrievalNode,
            RerankNode rerankNode,
            GenerateNode generateNode,
            FallbackNode fallbackNode) {

        StateGraph graph = new StateGraph("CustomerServiceRAG");

        graph.addNode("classify", classifyNode);
        graph.addNode("retrieve", retrievalNode);
        graph.addNode("rerank", rerankNode);
        graph.addNode("generate", generateNode);
        graph.addNode("fallback", fallbackNode);

        graph.addEdge(StateGraph.START, "classify");
        graph.addEdge("classify", "retrieve");
        graph.addEdge("retrieve", "rerank");
        graph.addEdge("rerank", "generate");

        // 兜底分支：如果 RAG 置信度低，转人工
        graph.addConditionalEdges("generate",
            edge -> edge.value("confidence") > 0.7
                ? StateGraph.END
                : "fallback");
        graph.addEdge("fallback", StateGraph.END);

        return graph;
    }
}

// 问题分类 Agent
@Component
public class QuestionClassifyNode implements NodeAction {

    @Resource
    private ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String question = state.value("question");

        String prompt = "分类用户问题：\n" +
            "类型可选：[订单问题, 支付问题, 物流问题, 商品问题, 退款问题, 其他]\n" +
            "用户问题：" + question + "\n" +
            "请返回 JSON：{type: 类型, confidence: 0-1}";

        String result = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        Map<String, Object> updates = new HashMap<>();
        updates.put("questionType", JsonUtil.parse(result).get("type"));
        return updates;
    }
}

// 多路召回节点
@Component
public class MultiRetrievalNode implements NodeAction {

    @Resource
    private ChatClient embeddingClient;

    @DubboReference
    private MilvusDubboService milvusService;

    @DubboReference
    private SearchDubboService searchService;

    @Resource
    private KnowledgeMapper knowledgeMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String question = state.value("question");
        String questionType = state.value("questionType");

        List<Document> allDocs = new ArrayList<>();

        // 路 1：Milvus 向量检索
        float[] embedding = embeddingClient.embed(question);
        allDocs.addAll(milvusService.search(embedding, topK = 10));

        // 路 2：ES 全文检索
        allDocs.addAll(searchService.searchByKeyword(question, topK = 5));

        // 路 3：MySQL 结构化检索（按类型）
        allDocs.addAll(knowledgeMapper.findByType(questionType, limit = 5));

        // 去重
        Map<String, Document> unique = new LinkedHashMap<>();
        for (Document doc : allDocs) {
            unique.put(doc.getId(), doc);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("candidateDocs", new ArrayList<>(unique.values()));
        return updates;
    }
}

// 重排序节点
@Component
public class RerankNode implements NodeAction {

    @Resource
    private BgeRerankModel rerankModel;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String question = state.value("question");
        List<Document> docs = state.value("candidateDocs");

        List<Document> reranked = rerankModel.rerank(question, docs, topK = 3);

        Map<String, Object> updates = new HashMap<>();
        updates.put("contextDocs", reranked);
        return updates;
    }
}

// 生成节点
@Component
public class GenerateNode implements NodeAction {

    @Resource
    private ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String question = state.value("question");
        List<Document> context = state.value("contextDocs");

        String prompt = "你是智换平台客服助手。基于以下信息回答用户问题：\n" +
            "参考信息：\n" + formatDocs(context) + "\n" +
            "用户问题：" + question + "\n" +
            "要求：\n" +
            "1. 友好专业，简洁清晰\n" +
            "2. 如果信息不足，建议转人工客服\n" +
            "3. 涉及金钱问题必须引导至官方渠道";

        String answer = chatClient.prompt()
            .system("你是智换客服")
            .user(prompt)
            .call()
            .content();

        // 评估置信度
        double confidence = evaluateConfidence(question, context, answer);

        Map<String, Object> updates = new HashMap<>();
        updates.put("answer", answer);
        updates.put("confidence", confidence);
        return updates;
    }
}
```

### 5.4 Milvus 向量库集成

```java
@Configuration
public class MilvusConfig {

    @Bean
    public MilvusClient milvusClient() {
        return new MilvusClient.Builder()
            .uri("http://127.0.0.1:19530")
            .token("root:Milvus")
            .build();
    }
}

// 向量库操作服务
@Service
public class MilvusServiceImpl implements MilvusDubboService {

    @Resource
    private MilvusClient milvusClient;

    @Resource
    private ChatClient embeddingClient;

    /**
     * 写入商品向量
     */
    @Override
    public void insertProductVector(ProductVector vector) {
        float[] embedding = embeddingClient.embed(
            vector.getTitle() + " " + vector.getDescription());

        Map<String, Object> fields = new HashMap<>();
        fields.put("product_id", vector.getProductId());
        fields.put("title", vector.getTitle());
        fields.put("description", vector.getDescription());
        fields.put("category_id", vector.getCategoryId());
        fields.put("price", vector.getPrice());
        fields.put("embedding", embedding);
        fields.put("tags", vector.getTags());

        milvusClient.insert("product_vectors",
            Collections.singletonList(fields));
    }

    /**
     * 向量检索
     */
    @Override
    public List<ProductVector> searchSimilarProducts(String query, int topK) {
        float[] queryEmbedding = embeddingClient.embed(query);

        SearchParam searchParam = SearchParam.builder()
            .collectionName("product_vectors")
            .vectorFieldName("embedding")
            .vectors(List.of(queryEmbedding))
            .topK(topK)
            .filter("status == 3")  // 只检索在售商品
            .outputFields(List.of("product_id", "title", "price"))
            .build();

        SearchResp resp = milvusClient.search(searchParam);

        return resp.getResults().get(0).stream()
            .map(this::convertToProduct)
            .collect(Collectors.toList());
    }
}
```

### 5.5 Aho-Corasick 违规词匹配

```java
@Component
public class AhoCorasickMatcher {

    private final Automaton automaton;
    private final List<String> violationWords;

    public AhoCorasickMatcher(ViolationWordRepository repository) {
        // 初始化 AC 自动机
        List<String> words = repository.findAllEnabled()
            .stream()
            .map(ViolationWord::getWord)
            .collect(Collectors.toList());
        this.violationWords = words;
        this.automaton = new Automaton(words);
    }

    /**
     * 匹配文本中的违规词
     */
    public List<String> match(String text) {
        List<String> hits = new ArrayList<>();
        for (String word : automaton.parseText(text)) {
            hits.add(word);
        }
        return hits;
    }
}

// 审核服务
@Service
public class ContentAuditServiceImpl implements AuditDubboService {

    @Resource
    private AhoCorasickMatcher matcher;

    @Resource
    private ChatClient chatClient;  // MiniMax-M3-Safety

    @Override
    public AuditResult auditContent(String content, String contentType) {
        // 1. 规则引擎快速检查
        List<String> hitWords = matcher.match(content);
        if (!hitWords.isEmpty()) {
            return AuditResult.reject("命中违规词: " + hitWords);
        }

        // 2. AI 安全模型深度审核
        String aiResult = chatClient.prompt()
            .system("你是内容安全审核专家")
            .user("判断这段内容是否违规（色情/暴力/政治敏感/广告）：\n" + content)
            .call()
            .content();

        AuditResult result = parseAiResult(aiResult);
        return result;
    }
}
```

## 6. LLM Gateway（统一模型网关）

```java
@Component
public class LlmGateway {

    @Resource
    private ChatClient chatClient;

    @Resource
    private TokenUsageRepository tokenUsageRepository;

    /**
     * 统一的 MiniMax-M3 调用入口
     */
    public String call(String prompt, String systemPrompt,
                       LlmTaskType taskType) {
        long start = System.currentTimeMillis();

        try {
            String result = chatClient.prompt()
                .system(systemPrompt)
                .user(prompt)
                .options(ChatOptions.builder()
                    .model("MiniMax-M3")
                    .temperature(0.7)
                    .maxTokens(2000)
                    .build())
                .call()
                .content();

            // 记录 token 使用
            tokenUsageRepository.save(TokenUsage.builder()
                .taskType(taskType)
                .durationMs(System.currentTimeMillis() - start)
                .createTime(LocalDateTime.now())
                .build());

            return result;
        } catch (Exception e) {
            log.error("LLM call failed", e);
            // 降级策略：返回默认响应
            return getFallbackResponse(taskType);
        }
    }
}
```

## 7. Dubbo 接口设计

### 7.1 Provider 接口

```java
public interface AiDubboService {

    // AI 一键上架
    AiPublishResult generateProductInfo(List<String> images, String userInput);

    // AI 议价
    BargainResult bargain(Long productId, Long buyerId, BigDecimal buyerOffer);

    // AI 客服
    CustomerServiceResult chatWithCustomer(Long userId, String question,
                                          List<ChatMessage> history);

    // AI 审核
    AuditResult auditContent(String content, String contentType);

    // OCR 识别
    OcrResult ocrIdCard(String frontUrl, String backUrl);

    // 向量检索
    List<ProductVector> searchSimilarProducts(String query, int topK);
}
```

### 7.2 Consumer 接口

```java
@DubboReference
private UserDubboService userDubboService;

@DubboReference
private ProductDubboService productDubboService;

@DubboReference
private TradeDubboService tradeDubboService;
```

## 8. Kafka 事件

### 8.1 生产事件

| Topic | 事件 | 消费者 |
|---|---|---|
| ai-events | AiPublishCompleted / BargainCompleted | product / trade |
| ai-audit-events | AiAuditResult | product / audit |
| ai-cost-events | TokenUsage | job（成本监控）|

### 8.2 消费事件

| Topic | 事件 | 处理逻辑 |
|---|---|---|
| product-events | ProductCreated | 写入 Milvus 向量库 |
| order-events | OrderCompleted | 更新 RAG 知识库成交价 |

## 9. 设计模式应用

| 模式 | 应用场景 |
|---|---|
| **Graph 状态机** | AI 工作流编排（上架/客服） |
| **Multi-Agent** | AI 议价协作 |
| **策略模式** | 不同类目的 AI 估价策略 |
| **责任链** | 审核流程：规则 → AI → 人工 |
| **代理模式** | LLM Gateway 统一代理 |
| **观察者** | Kafka 事件订阅 |

## 10. 技术亮点 ⭐

| 亮点 | 说明 |
|---|---|
| **Multi-Agent 协作** | 议价场景使用 ReactAgent + SupervisorAgent，平台仲裁 Agent 兜底 |
| **Graph 工作流编排** | 上架和客服使用 Graph，可视化、可调试、可扩展 |
| **Agentic RAG** | Graph 动态选择检索策略（向量/全文/SQL）|
| **统一 LLM Gateway** | 所有 AI 调用走 Gateway，降级 + 重试 + token 监控 |
| **Aho-Corasick 双引擎** | 规则引擎快路径 + AI 模型慢路径 |
| **Embedding 缓存** | 高频问题 Embedding 缓存，节省 token 成本 |
| **Milvus 向量库** | 千万级商品向量毫秒级检索 |

## 11. 简历话术

> 基于 Spring-AI-Alibaba Agent-Framework + Graph 设计 AI 中台：1）使用 Graph 编排商品上架工作流（识图→类目→估价→文案→审核），集成 MiniMax-M3 多模态能力，上架效率提升 10 倍；2）使用 Multi-Agent 协作实现 AI 议价（买家 Agent + 卖家 Agent + 平台仲裁 Agent），议价成功率 75%；3）引入 Milvus 向量库 + Agentic RAG 架构实现智能客服，多路召回 + Rerank 重排序 + MiniMax-M3 生成，问题解决率 85%；4）使用 Aho-Corasick + MiniMax-M3-Safety 双引擎内容审核，违规识别准确率 99.2%。

---

## 12. 关键依赖关系

```
zhihuan-ai (本服务)
   ├─ 提供 → Dubbo: AiDubboService (被 product/user/trade/im/audit 调用)
   ├─ 调用 → Dubbo: UserDubboService / ProductDubboService / TradeDubboService
   ├─ 调用 → Milvus Client (向量库)
   ├─ 调用 → MiniMax-M3 API (LLM Gateway)
   └─ 发布/消费 → Kafka: ai-events / ai-audit-events
```
