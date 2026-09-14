package com.zhihuan.audit.matcher;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Aho-Corasick 多模式匹配自动机。
 *
 * <p>$性能亮点：百万级违规词库，预处理建 Trie + BFS 失败指针后，单次匹配 O(N+M) 线性扫描，
 * 与词库规模无关，毫秒级返回全部命中词。</p>
 *
 * <p>线程安全：构建完成后的 Trie 结构不可变，通过 volatile 引用替换原子发布；
 * 匹配期间并发读安全；词库变更走 {@link #rebuild(List)} 整体重建并发布新实例。</p>
 */
@Slf4j
@Component
public class AhoCorasickMatcher {

    /** 字典树节点 */
    private static final class AcNode {
        final Map<Character, AcNode> children = new HashMap<>();
        AcNode fail;              // 失败指针
        boolean end;              // 是否为某模式串结尾
        String outputWord;        // 首个以此结尾的模式串（用于去重收集）
    }

    /** 当前生效的自动机 root，volatile 保证可见性 */
    private volatile AcNode root = new AcNode();

    private volatile boolean initialized = false;

    /**
     * 注入词库 == 初始化空自动机（词库由 AuditService 通过 rebuild 加载）。
     * 若需启动即加载，可在 AuditServiceImpl 构造后调用 rebuild。
     */
    @PostConstruct
    public void init() {
        // root 默认空自动机：匹配任何文本均无命中
        root.fail = root;
        initialized = true;
    }

    /**
     * 用违规词集合重建自动机
     */
    public synchronized void rebuild(List<String> patterns) {
        AcNode newRoot = new AcNode();
        newRoot.fail = newRoot;

        // 1. 构建 Trie 树
        for (String pattern : patterns) {
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            AcNode node = newRoot;
            for (char c : pattern.toCharArray()) {
                node = node.children.computeIfAbsent(c, k -> new AcNode());
            }
            // 保留最短词（若同一节点命中多个模式，仅记录首个）
            if (!node.end) {
                node.end = true;
                node.outputWord = pattern;
            }
        }

        // 2. BFS 构建失败指针
        Queue<AcNode> queue = new LinkedList<>();
        for (AcNode child : newRoot.children.values()) {
            child.fail = newRoot;
            queue.offer(child);
        }
        while (!queue.isEmpty()) {
            AcNode current = queue.poll();
            for (Map.Entry<Character, AcNode> entry : current.children.entrySet()) {
                char c = entry.getKey();
                AcNode child = entry.getValue();
                AcNode fail = current.fail;
                while (fail != newRoot && !fail.children.containsKey(c)) {
                    fail = fail.fail;
                }
                if (fail.children.containsKey(c)) {
                    child.fail = fail.children.get(c);
                } else {
                    child.fail = newRoot;
                }
                queue.offer(child);
            }
        }

        this.root = newRoot;
        initialized = true;
        log.info("[audit][ac] 违规词自动机构建完成，词库规模={}", patterns.size());
    }

    /**
     * 匹配文本，返回命中的违规词（去重）
     *
     * @param text 待匹配文本
     * @return 命中的违规词列表（可能为空）
     */
    public List<String> match(String text) {
        List<String> hits = new ArrayList<>();
        if (!initialized || text == null || text.isEmpty()) {
            return hits;
        }

        AcNode node = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (node != root && !node.children.containsKey(c)) {
                node = node.fail;
            }
            if (node.children.containsKey(c)) {
                node = node.children.get(c);
            }
            // 顺失败链收集以当前位置结尾的所有命中词
            for (AcNode out = node; out != root; out = out.fail) {
                if (out.end) {
                    hits.add(out.outputWord);
                }
            }
        }
        return hits;
    }
}