package com.zhihuan.trade.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihuan.common.exception.BizException;
import com.zhihuan.product.api.ProductDubboService;
import com.zhihuan.trade.common.OrderStatus;
import com.zhihuan.trade.common.RefundStatus;
import com.zhihuan.trade.dto.RefundApplyDTO;
import com.zhihuan.trade.entity.OrderMain;
import com.zhihuan.trade.entity.OrderMessage;
import com.zhihuan.trade.entity.OrderStatusLog;
import com.zhihuan.trade.entity.RefundOrder;
import com.zhihuan.trade.event.EventPublisherService;
import com.zhihuan.trade.mapper.OrderMainMapper;
import com.zhihuan.trade.mapper.OrderStatusLogMapper;
import com.zhihuan.trade.mapper.RefundOrderMapper;
import com.zhihuan.trade.service.AccountService;
import com.zhihuan.trade.service.RefundService;
import com.zhihuan.trade.vo.RefundOrderVO;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 退款服务：申请 -> 卖家审核。
 * 退款入账走 Seata AT（解冻资金 + 回补库存），与订单状态流转保持强一致。
 */
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundOrderMapper refundOrderMapper;
    private final OrderMainMapper orderMainMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final EventPublisherService eventPublisherService;
    private final AccountService accountService;

    @DubboReference(check = false)
    private ProductDubboService productDubboService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyRefund(RefundApplyDTO dto) {
        OrderMain order = requireOrder(dto.getOrderId());
        if (!order.getBuyerId().equals(dto.getBuyerId())) {
            throw new BizException("无权操作该订单");
        }
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.SHIPPED) {
            throw new BizException("当前状态不支持申请退款");
        }
        RefundOrder refund = new RefundOrder();
        refund.setId(IdUtil.getSnowflakeNextId());
        refund.setRefundNo("R" + IdUtil.getSnowflakeNextId());
        refund.setOrderId(order.getId());
        refund.setBuyerId(order.getBuyerId());
        refund.setSellerId(order.getSellerId());
        refund.setRefundAmount(order.getPayAmount());
        refund.setReasonType(dto.getReasonType());
        refund.setReason(dto.getReason());
        refund.setImages(dto.getImages() == null ? null : JSONUtil.toJsonStr(dto.getImages()));
        refund.setSellerApproved(0);
        refund.setStatus(RefundStatus.PENDING);
        refundOrderMapper.insert(refund);

        // 订单流转为退款中
        statusLog(order.getId(), order.getStatus(), OrderStatus.REFUNDING, String.valueOf(dto.getBuyerId()), "申请退款");
        order.setStatus(OrderStatus.REFUNDING);
        orderMainMapper.updateById(order);
        return refund.getId();
    }

    @Override
    @GlobalTransactional(name = "refund-audit", rollbackFor = Exception.class)
    public void sellerAudit(Long refundId, Long sellerId, boolean agree) {
        RefundOrder refund = refundOrderMapper.selectById(refundId);
        if (refund == null) {
            throw new BizException("退款单不存在");
        }
        if (!refund.getSellerId().equals(sellerId)) {
            throw new BizException("无权处理该退款");
        }
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BizException("退款单已处理");
        }
        OrderMain order = requireOrder(refund.getOrderId());

        if (agree) {
            // 退款入账：解冻买家冻结资金退回可用余额
            accountService.refund(order.getBuyerId(), order.getId(), refund.getRefundAmount());
            // 回补库存（按购买数量）
            int count = order.getBuyerCount() == null || order.getBuyerCount() < 1
                ? 1 : order.getBuyerCount();
            productDubboService.unlockStock(order.getSkuId(), count);

            refund.setStatus(RefundStatus.REFUNDED);
            refund.setAuditTime(LocalDateTime.now());
            refund.setRefundTime(LocalDateTime.now());
            refund.setSellerApproved(1);
            refundOrderMapper.updateById(refund);

            statusLog(order.getId(), order.getStatus(), OrderStatus.REFUNDED, String.valueOf(sellerId), "同意退款");
            order.setStatus(OrderStatus.REFUNDED);
            orderMainMapper.updateById(order);
            saveEvent(order, "ORDER_REFUNDED");
        } else {
            refund.setStatus(RefundStatus.REJECTED);
            refund.setSellerApproved(2);
            refund.setAuditTime(LocalDateTime.now());
            refundOrderMapper.updateById(refund);

            statusLog(order.getId(), order.getStatus(), OrderStatus.PAID, String.valueOf(sellerId), "拒绝退款");
            order.setStatus(OrderStatus.PAID);
            orderMainMapper.updateById(order);
        }
    }

    @Override
    public List<RefundOrderVO> listByOrder(Long orderId) {
        List<RefundOrder> list = refundOrderMapper.selectList(
            new LambdaQueryWrapper<RefundOrder>().eq(RefundOrder::getOrderId, orderId));
        return list.stream().map(r -> {
            RefundOrderVO vo = new RefundOrderVO();
            BeanUtils.copyProperties(r, vo);
            vo.setStatusText(statusText(r.getStatus()));
            return vo;
        }).collect(Collectors.toList());
    }

    private OrderMain requireOrder(Long orderId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    private void statusLog(Long orderId, Integer from, Integer to, String operator, String reason) {
        OrderStatusLog log = new OrderStatusLog();
        log.setId(IdUtil.getSnowflakeNextId());
        log.setOrderId(orderId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperator(operator);
        log.setReason(reason);
        statusLogMapper.insert(log);
    }

    private void saveEvent(OrderMain order, String eventType) {
        com.zhihuan.trade.entity.OrderMessage msg = new OrderMessage();
        msg.setId(IdUtil.getSnowflakeNextId());
        msg.setOrderId(order.getId());
        msg.setOrderNo(order.getOrderNo());
        msg.setTopic("order-events");
        msg.setEventType(eventType);
        msg.setPayload(JSONUtil.createObj()
            .set("eventType", eventType)
            .set("orderId", order.getId())
            .set("orderNo", order.getOrderNo())
            .set("buyerId", order.getBuyerId())
            .set("productId", order.getProductId())
            .set("status", order.getStatus())
            .set("ts", System.currentTimeMillis()).toString());
        eventPublisherService.saveAndPublish(msg);
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case RefundStatus.PENDING -> "待审核";
            case RefundStatus.APPROVED -> "卖家同意";
            case RefundStatus.REJECTED -> "已拒绝";
            case RefundStatus.REFUNDED -> "已退款";
            case RefundStatus.CLOSED -> "已关闭";
            default -> "未知";
        };
    }
}