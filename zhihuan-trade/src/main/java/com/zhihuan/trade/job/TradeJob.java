package com.zhihuan.trade.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihuan.trade.common.MessageStatus;
import com.zhihuan.trade.common.OrderStatus;
import com.zhihuan.trade.entity.OrderMain;
import com.zhihuan.trade.entity.OrderMessage;
import com.zhihuan.trade.event.EventPublisherService;
import com.zhihuan.trade.mapper.OrderMainMapper;
import com.zhihuan.trade.mapper.OrderMessageMapper;
import com.zhihuan.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易定时任务：
 *  1. 逾期未支付订单自动取消（释放库存）
 *  2. 本地消息表补偿投递（FAILED/待发送重发到 Kafka）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeJob {

    private final OrderMainMapper orderMainMapper;
    private final OrderMessageMapper orderMessageMapper;
    private final OrderService orderService;
    private final EventPublisherService eventPublisherService;

    /** 每分钟扫描一次支付超时订单并自动取消 */
    @Scheduled(fixedDelay = 60_000)
    public void autoCancelExpiredOrders() {
        List<OrderMain> expired = orderMainMapper.selectList(
            new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getStatus, OrderStatus.PENDING_PAY)
                .lt(OrderMain::getPayExpireTime, LocalDateTime.now()));
        for (OrderMain order : expired) {
            try {
                orderService.cancelOrder(order.getId(), order.getBuyerId(), "支付超时自动取消");
                log.info("[trade.job][auto-cancel] orderId={}, orderNo={}", order.getId(), order.getOrderNo());
            } catch (Exception e) {
                log.error("[trade.job][auto-cancel failed] orderId={}", order.getId(), e);
            }
        }
    }

    /** 每 2 分钟扫描一次发货后超时未确认收货的订单，自动确认收货并放款给卖家 */
    @Scheduled(fixedDelay = 120_000)
    public void autoConfirmExpiredOrders() {
        List<OrderMain> expired = orderMainMapper.selectList(
            new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getStatus, OrderStatus.SHIPPED)
                .lt(OrderMain::getConfirmExpireTime, LocalDateTime.now()));
        for (OrderMain order : expired) {
            try {
                orderService.confirmReceipt(order.getId(), order.getBuyerId());
                log.info("[trade.job][auto-confirm] orderId={}, orderNo={}", order.getId(), order.getOrderNo());
            } catch (Exception e) {
                log.error("[trade.job][auto-confirm failed] orderId={}", order.getId(), e);
            }
        }
    }

    /** 每 30 秒补偿投递本地消息表中待发送/失败的消息 */
    @Scheduled(fixedDelay = 30_000)
    public void resendPendingMessages() {
        List<OrderMessage> pending = orderMessageMapper.selectList(
            new LambdaQueryWrapper<OrderMessage>()
                .in(OrderMessage::getStatus, MessageStatus.PENDING, MessageStatus.FAILED)
                .and(w -> w.isNull(OrderMessage::getNextRetryTime)
                    .or().le(OrderMessage::getNextRetryTime, LocalDateTime.now())));
        for (OrderMessage msg : pending) {
            try {
                eventPublisherService.dispatch(msg);
            } catch (Exception e) {
                log.error("[trade.job][resend failed] id={}, orderId={}, type={}", msg.getId(),
                    msg.getOrderId(), msg.getEventType(), e);
            }
        }
    }
}