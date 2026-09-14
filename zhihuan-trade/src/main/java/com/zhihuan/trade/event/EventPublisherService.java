package com.zhihuan.trade.event;

import com.zhihuan.trade.entity.OrderMessage;

/**
 * 本地消息表 + Kafka 事件可靠投递
 */
public interface EventPublisherService {

    /**
     * 事务内写入本地消息表，并在事务提交后投递到 Kafka。
     * Kafka 投递失败仅标记 FAILED，由定时任务补偿重发。
     */
    void saveAndPublish(OrderMessage message);

    /** 立即发送并更新消息状态（供补偿任务调用） */
    boolean dispatch(OrderMessage message);
}