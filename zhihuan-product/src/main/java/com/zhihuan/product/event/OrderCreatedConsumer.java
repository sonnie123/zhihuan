package com.zhihuan.product.event;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 订单创建事件消费者（占位）。
 *
 * 订阅 order-events（由 zhihuan-trade 生产 OrderCreated），
 * 未来处理逻辑：将订单内商品标记为「已售」（含锁定/下架等联动），可走 ProductDubboService / ProductService。
 * 当前 trade 服务尚未开发，先以日志占位，消费不落库、不改商品状态。
 */
@Slf4j
@Component
public class OrderCreatedConsumer {

    @KafkaListener(topics = "${zhihuan.kafka.order-events-topic}",
        groupId = "${zhihuan.kafka.order-group}")
    public void onOrderCreated(String message) {
        JSONObject json = JSONUtil.parseObj(message);
        Long orderId = json.getLong("orderId");
        Long productId = json.getLong("productId");
        String eventType = json.getStr("eventType");
        // TODO(trade): 订单创建后联动商品状态为「已售」，注意与库存扣减、订单取消补偿的一致性
        log.info("[kafka.consumed][order] topic=order-events, eventType={}, orderId={}, productId={}, raw={}",
            eventType, orderId, productId, message);
    }
}
