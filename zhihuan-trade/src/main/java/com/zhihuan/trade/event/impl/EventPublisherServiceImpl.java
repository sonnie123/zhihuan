package com.zhihuan.trade.event.impl;

import cn.hutool.core.util.IdUtil;
import com.zhihuan.trade.common.MessageStatus;
import com.zhihuan.trade.entity.OrderMessage;
import com.zhihuan.trade.event.EventPublisherService;
import com.zhihuan.trade.mapper.OrderMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 基于本地消息表的可靠事件投递：
 * 1. 业务事务内写 order_message(status=PENDING)
 * 2. 事务提交后(afterCommit)异步投递 Kafka
 * 3. 投递成功标记 SENT，失败标记 FAILED，由定时任务补偿
 */
@Slf4j
@Service
public class EventPublisherServiceImpl implements EventPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderMessageMapper messageMapper;

    @Value("${zhihuan.kafka.order-events-topic:order-events}")
    private String defaultTopic;

    public EventPublisherServiceImpl(KafkaTemplate<String, String> kafkaTemplate,
                                     OrderMessageMapper messageMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.messageMapper = messageMapper;
    }

    @Override
    public void saveAndPublish(OrderMessage message) {
        if (message.getId() == null) {
            message.setId(IdUtil.getSnowflakeNextId());
        }
        if (message.getStatus() == null) {
            message.setStatus(MessageStatus.PENDING);
        }
        if (message.getRetryCount() == null) {
            message.setRetryCount(0);
        }
        messageMapper.insert(message);
        log.info("[trade.event][outbox saved] id={}, orderId={}, type={}, status=PENDING，等待定时任务在全局事务提交后投递",
            message.getId(), message.getOrderId(), message.getEventType());

        // 注意：不能在此处用本地事务 afterCommit 直接投递 Kafka。
        // 本方法可能处于 Seata 全局事务内(order_message 是全局分支)。若在全局提交结果出来前
        // 提前发送事件，会造成：①事务回滚时事件已发出(Bug B，违反 outbox 可靠投递)；
        // ②dispatch 里 markSent 更新同一行 order_message.status(PENDING->SENT)发生在全局事务外，
        //    污染 undo_log 脏检查，导致分支回滚 PhaseTwo_RollbackFailed_Unretryable(Bug A)。
        // 正确做法：只落 PENDING，由定时任务 resendPendingMessages 在全局事务提交后(行已持久化)投递并标记 SENT。
    }

    @Override
    public boolean dispatch(OrderMessage message) {
        try {
            kafkaTemplate.send(message.getTopic() == null ? defaultTopic : message.getTopic(),
                    String.valueOf(message.getOrderId()), message.getPayload())
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        fail(message);
                        log.error("[trade.event][kafka send failed] id={}, orderId={}, type={}, topic={}",
                            message.getId(), message.getOrderId(), message.getEventType(),
                            message.getTopic() == null ? defaultTopic : message.getTopic(), ex);
                    } else {
                        markSent(message);
                        log.info("[trade.event][kafka sent] id={}, orderId={}, type={}, topic={}, partition={}, offset={}",
                            message.getId(), message.getOrderId(), message.getEventType(),
                            message.getTopic() == null ? defaultTopic : message.getTopic(),
                            res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
                    }
                });
            return true;
        } catch (Exception e) {
            fail(message);
            log.error("[trade.event][kafka send rejected] id={}, orderId={}, type={}", message.getId(),
                message.getOrderId(), message.getEventType(), e);
            return false;
        }
    }

    private void markSent(OrderMessage m) {
        m.setStatus(MessageStatus.SENT);
        m.setRetryCount((m.getRetryCount() == null ? 0 : m.getRetryCount()) + 1);
        messageMapper.updateById(m);
    }

    private void fail(OrderMessage m) {
        m.setStatus(MessageStatus.FAILED);
        m.setRetryCount((m.getRetryCount() == null ? 0 : m.getRetryCount()) + 1);
        m.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
        messageMapper.updateById(m);
    }
}