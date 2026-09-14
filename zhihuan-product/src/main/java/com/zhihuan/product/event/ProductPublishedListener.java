package com.zhihuan.product.event;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 商品上架事件监听：发送到 Kafka product-events 主题，
 * 供 search 建索引 / feed / recommend / audit 等下游消费。
 * Kafka 不可用时降级为日志，不阻塞上架主流程。
 */
@Slf4j
@Component
public class ProductPublishedListener {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${zhihuan.kafka.product-events-topic:product-events}")
    private String topic;

    public ProductPublishedListener(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @EventListener
    public void onProductPublished(ProductPublishedEvent event) {
        JSONObject msg = JSONUtil.createObj()
            .set("eventType", "PRODUCT_PUBLISHED")
            .set("productId", event.productId())
            .set("sellerId", event.sellerId())
            .set("ts", System.currentTimeMillis());
        String value = msg.toString();
        try {
            kafkaTemplate.send(topic, String.valueOf(event.productId()), value)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("[product.published][kafka send failed, degraded] topic={}, productId={}, sellerId={}",
                            topic, event.productId(), event.sellerId(), ex);
                    } else {
                        log.info("[product.published][kafka sent] topic={}, partition={}, offset={}, productId={}, sellerId={}",
                            topic, res.getRecordMetadata().partition(), res.getRecordMetadata().offset(),
                            event.productId(), event.sellerId());
                    }
                });
        } catch (Exception e) {
            log.error("[product.published][kafka send rejected] topic={}, productId={}, sellerId={}",
                topic, event.productId(), event.sellerId(), e);
        }
    }
}