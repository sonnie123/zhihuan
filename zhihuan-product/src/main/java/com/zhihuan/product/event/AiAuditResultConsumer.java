package com.zhihuan.product.event;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * AI 审核结果消费者（占位）。
 *
 * 订阅 ai-audit-events（由 zhihuan-ai 生产 AiAuditResult），
 * 未来处理逻辑：按审核结果更新商品 AI 审核状态（product_main.ai_audit_status）。
 * 当前 ai 服务尚未开发，先以日志占位，消费不落库、不改商品状态。
 */
@Slf4j
@Component
public class AiAuditResultConsumer {

    @KafkaListener(topics = "${zhihuan.kafka.ai-audit-events-topic}",
        groupId = "${zhihuan.kafka.ai-audit-group}")
    public void onAiAuditResult(String message) {
        JSONObject json = JSONUtil.parseObj(message);
        Long productId = json.getLong("productId");
        String eventType = json.getStr("eventType");
        Integer aiAuditResult = json.getInt("aiAuditResult");
        // TODO(ai): 按 aiAuditResult 更新 product_main 的 AI 审核状态，可走 ProductService.updateStatus / 独立字段
        log.info("[kafka.consumed][ai-audit] topic=ai-audit-events, eventType={}, productId={}, aiAuditResult={}, raw={}",
            eventType, productId, aiAuditResult, message);
    }
}
