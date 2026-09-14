package com.zhihuan.product.event;

/**
 * 商品上架事件（供 search 建索引 / audit 复核等订阅；当前以本地事件总线占位，
 * 后续接入 Kafka 时改为发送 product.published 主题）。
 *
 * @param productId 商品ID
 * @param sellerId  卖家ID
 */
public record ProductPublishedEvent(Long productId, Long sellerId) {
}