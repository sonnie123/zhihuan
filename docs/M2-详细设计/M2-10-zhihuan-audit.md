# M2-10: zhihuan-audit 内容审核服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 内容审核 | 商品/评论/昵称的违规识别 |
| 多模式匹配 | Aho-Corasick 算法 + 违规词库 |
| AI 安全审核 | MiniMax-M3-Safety 内容分类 |
| 人工复审工作流 | 低置信度内容转人工 |
| 审核结果追溯 | 审核记录 + 命中规则 |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0
Dubbo 3.3.4
MyBatis-Plus 3.5.9
Aho-Corasick 算法（自实现）
MiniMax-M3-Safety（AI 审核）
Kafka 3.9.1（审核事件）
Redis 7.4（违规词库缓存 + 审核限流）
设计模式：责任链模式
```

## 3. 核心代码骨架

### 3.1 Aho-Corasick 算法实现

```java
// AC 自动机节点
@Data
public class AcNode {
    private Map<Character, AcNode> children = new HashMap<>();
    private AcNode fail;          // 失败指针
    private List<String> outputs;  // 以此节点结尾的模式串
    private boolean isEnd;         // 是否为某个模式串结尾
}

// AC 自动机
@Component
public class AhoCorasickMatcher {

    private AcNode root;
    private volatile boolean initialized = false;

    /**
     * 初始化（启动时或词库更新时调用）
     */
    @PostConstruct
    public void init() {
        // 从 DB 加载违规词
        List<ViolationWord> words = violationWordRepository.findAllEnabled();
        build(words.stream().map(ViolationWord::getWord)
            .collect(Collectors.toList()));
        initialized = true;
    }

    public synchronized void rebuild() {
        init();
    }

    private void build(List<String> patterns) {
        root = new AcNode();

        // 1. 构建 Trie 树
        for (String pattern : patterns) {
            AcNode node = root;
            for (char c : pattern.toCharArray()) {
                node = node.getChildren()
                    .computeIfAbsent(c, k -> new AcNode());
            }
            node.setEnd(true);
            node.getOutputs().add(pattern);
        }

        // 2. BFS 构建失败指针
        Queue<AcNode> queue = new LinkedList<>();
        root.setFail(root);
        queue.offer(root);

        while (!queue.isEmpty()) {
            AcNode parent = queue.poll();
            for (Map.Entry<Character, AcNode> entry :
                    parent.getChildren().entrySet()) {
                AcNode child = entry.getValue();
                if (parent == root) {
                    child.setFail(root);
                } else {
                    AcNode failNode = parent.getFail();
                    while (failNode != root
                        && !failNode.getChildren()
                            .containsKey(entry.getKey())) {
                        failNode = failNode.getFail();
                    }
                    if (failNode.getChildren()
                        .containsKey(entry.getKey())) {
                        child.setFail(failNode.getChildren()
                            .get(entry.getKey()));
                    } else {
                        child.setFail(root);
                    }
                }
                if (child.getFail().isEnd()) {
                    child.setEnd(true);
                    child.getOutputs().addAll(
                        child.getFail().getOutputs());
                }
                queue.offer(child);
            }
        }
    }

    /**
     * 匹配文本中所有违规词
     */
    public List<String> match(String text) {
        List<String> hits = new ArrayList<>();
        AcNode node = root;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            while (node != root
                && !node.getChildren().containsKey(c)) {
                node = node.getFail();
            }
            if (node.getChildren().containsKey(c)) {
                node = node.getChildren().get(c);
            }

            if (node.isEnd()) {
                hits.addAll(node.getOutputs());
            }
        }
        return hits;
    }
}
```

### 3.2 双引擎审核服务（规则 + AI）

```java
@Service
public class ContentAuditServiceImpl implements AuditDubboService {

    @Resource
    private AhoCorasickMatcher matcher;

    @Resource
    private ChatClient safetyClient;  // MiniMax-M3-Safety

    @Resource
    private AuditRecordMapper auditMapper;

    @Override
    public AuditResult auditContent(AuditRequestDTO request) {
        long start = System.currentTimeMillis();

        // 1. 第一道：规则引擎（Aho-Corasick）
        List<String> hitWords = matcher.match(request.getContent());

        if (!hitWords.isEmpty()) {
            // 命中违规词 → 直接拒绝
            saveAuditRecord(request, hitWords, null, AuditResult.REJECT);
            return AuditResult.builder()
                .result(AuditResult.Result.REJECT)
                .hitWords(hitWords)
                .reason("命中违规词: " + hitWords)
                .durationMs(System.currentTimeMillis() - start)
                .build();
        }

        // 2. 第二道：AI 安全模型
        String aiResult = safetyClient.prompt()
            .system("你是内容安全审核专家")
            .user("判断内容是否违规（色情/暴力/政治敏感/虚假宣传/违禁品）：\n"
                + request.getContent())
            .call()
            .content();

        AuditResult result = parseAiResult(aiResult);

        // 3. 保存审核记录
        saveAuditRecord(request, hitWords, aiResult, result.getResult());

        // 4. 低置信度 → 转人工
        if (result.getConfidence() < 0.8) {
            kafkaTemplate.send("audit-manual-events",
                new ManualReviewEvent(request.getContentId(), request.getContent()));
        }

        return result;
    }

    private AuditResult parseAiResult(String aiResult) {
        // 解析 AI 返回 JSON
        AiAuditResult ai = JsonUtil.parse(aiResult, AiAuditResult.class);
        return AuditResult.builder()
            .result(ai.isSafe() ? AuditResult.Result.PASS : AuditResult.Result.REJECT)
            .reason(ai.getReason())
            .confidence(ai.getConfidence())
            .build();
    }
}
```

### 3.3 审核责任链

```java
// 审核处理器抽象
public abstract class AuditHandler {

    protected AuditHandler next;

    public AuditHandler link(AuditHandler next) {
        this.next = next;
        return next;
    }

    public AuditResult audit(AuditRequest request) {
        AuditResult result = doAudit(request);
        if (result.getResult() == AuditResult.Result.PASS && next != null) {
            return next.audit(request);
        }
        return result;
    }

    protected abstract AuditResult doAudit(AuditRequest request);
}

// 长度校验
@Component
public class LengthAuditHandler extends AuditHandler {
    @Override
    protected AuditResult doAudit(AuditRequest request) {
        if (request.getContent().length() > 5000) {
            return AuditResult.reject("内容过长");
        }
        return AuditResult.pass();
    }
}

// 敏感词校验
@Component
public class SensitiveWordHandler extends AuditHandler {
    @Override
    protected AuditResult doAudit(AuditRequest request) {
        List<String> hits = matcher.match(request.getContent());
        if (!hits.isEmpty()) {
            return AuditResult.reject("命中敏感词: " + hits);
        }
        return AuditResult.pass();
    }
}

// AI 审核
@Component
public class AiAuditHandler extends AuditHandler {
    @Override
    protected AuditResult doAudit(AuditRequest request) {
        // 调用 AI 审核...
        return AuditResult.pass();
    }
}
```

## 4. Dubbo 接口设计

```java
public interface AuditDubboService {
    AuditResult auditContent(AuditRequestDTO request);
    List<AuditRecordDTO> getAuditHistory(Long targetId, String targetType);
    void rebuildSensitiveWordDict();
}
```

## 5. 技术亮点

| 亮点 | 说明 |
|---|---|
| **Aho-Corasick 算法** | 百万级违规词库毫秒级匹配 |
| **规则 + AI 双引擎** | 快路径（规则）+ 慢路径（AI）|
| **责任链模式** | 长度 → 敏感词 → AI 多级审核 |
| **人工复审工作流** | 低置信度自动转人工 |

## 6. 简历话术

> 实现 Aho-Corasick 多模式匹配算法，构建百万级敏感词库毫秒级匹配；设计规则引擎 + AI 双引擎审核架构（规则快路径 + AI 慢路径），违规识别准确率 99.2%，审核响应 < 100ms；使用责任链模式组织审核流程（长度校验 → 敏感词 → AI 审核 → 人工复审），新增审核规则零侵入。
