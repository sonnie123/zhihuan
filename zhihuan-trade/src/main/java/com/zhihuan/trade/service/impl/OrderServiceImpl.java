package com.zhihuan.trade.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihuan.common.exception.BizException;
import com.zhihuan.product.api.ProductDubboService;
import com.zhihuan.product.api.vo.OnSaleProductVO;
import com.zhihuan.product.api.vo.ProductSkuVO;
import com.zhihuan.trade.common.OrderStatus;
import com.zhihuan.trade.dto.OrderCreateDTO;
import com.zhihuan.trade.dto.ReviewDTO;
import com.zhihuan.trade.entity.OrderMain;
import com.zhihuan.trade.entity.OrderMessage;
import com.zhihuan.trade.entity.OrderReview;
import com.zhihuan.trade.entity.OrderStatusLog;
import com.zhihuan.trade.entity.ShippingInfo;
import com.zhihuan.trade.event.EventPublisherService;
import com.zhihuan.trade.mapper.OrderMainMapper;
import com.zhihuan.trade.mapper.OrderReviewMapper;
import com.zhihuan.trade.mapper.OrderStatusLogMapper;
import com.zhihuan.trade.mapper.ShippingInfoMapper;
import com.zhihuan.trade.service.AccountService;
import com.zhihuan.trade.service.OrderService;
import com.zhihuan.trade.vo.OrderVO;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单核心交易服务。
 * 分布式事务策略：
 *  - 下单/取消/发货/评价：本地事务 + 本地消息表（库存用 Dubbo 补偿）
 *  - 支付/确认收货/退款：Seata AT 全局事务（资金强一致）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMainMapper orderMainMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final OrderReviewMapper orderReviewMapper;
    private final ShippingInfoMapper shippingInfoMapper;
    private final EventPublisherService eventPublisherService;
    private final AccountService accountService;

    @DubboReference(check = false)
    private ProductDubboService productDubboService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateDTO dto) {
        // 1. 幂等检查
        if (StringUtils.hasText(dto.getIdempotentKey())) {
            OrderMain exist = orderMainMapper.selectOne(
                new LambdaQueryWrapper<OrderMain>().eq(OrderMain::getIdempotentKey, dto.getIdempotentKey()));
            if (exist != null) {
                return exist.getId();
            }
        }

        // 2. 校验商品在售
        OnSaleProductVO product = productDubboService.getOnSaleById(dto.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 3) {
            throw new BizException("商品不可购买");
        }

        // 3. 锁定 SKU 库存
        int quantity = dto.getQuantity() == null || dto.getQuantity() < 1 ? 1 : dto.getQuantity();
        ProductSkuVO sku = null;
        if (dto.getSkuId() != null) {
            sku = productDubboService.getSkuById(dto.getSkuId());
        } else if (product.getSkus() != null && !product.getSkus().isEmpty()) {
            sku = productDubboService.getSkuById(product.getSkus().get(0).getId());
        }
        Long skuId = sku != null ? sku.getId() : dto.getSkuId();
        if (skuId == null) {
            throw new BizException("商品缺少可售 SKU");
        }
        if (!productDubboService.lockStock(skuId, quantity)) {
            throw new BizException("库存不足");
        }

        // 4. 构造订单
        OrderMain order = new OrderMain();
        order.setId(IdUtil.getSnowflakeNextId());
        order.setOrderNo(buildOrderNo());
        order.setBuyerId(dto.getBuyerId());
        order.setSellerId(product.getSellerId());
        order.setProductId(product.getId());
        order.setSkuId(skuId);
        order.setBuyerCount(quantity);
        order.setProductTitle(product.getTitle());
        order.setProductImage(product.getCoverImage());
        BigDecimal unitPrice = sku != null && sku.getPrice() != null ? sku.getPrice() : product.getPrice();
        BigDecimal productAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal shippingFee = BigDecimal.ZERO;
        order.setProductPrice(productAmount);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(productAmount);
        order.setPayAmount(productAmount);
        order.setAddressSnapshot(buildAddressSnapshot(dto));
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(15));
        order.setIdempotentKey(dto.getIdempotentKey());
        orderMainMapper.insert(order);

        // 5. 状态日志
        statusLog(order.getId(), null, OrderStatus.PENDING_PAY, String.valueOf(dto.getBuyerId()), "下单");

        // 6. 本地消息表 ORDER_CREATED（事务提交后投递）
        saveEvent(order, "ORDER_CREATED");

        return order.getId();
    }

    @Override
    @GlobalTransactional(name = "pay-order", rollbackFor = Exception.class)
    public void payOrder(Long orderId, Long buyerId) {
        OrderMain order = require(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BizException("无权操作该订单");
        }
        if (!OrderStatus.payable(order.getStatus())) {
            throw new BizException("订单状态不允许支付");
        }
        // 冻结买家资金（支付不改变库存，锁定保留至确认收货）
        accountService.ensureAccount(buyerId);
        accountService.freeze(buyerId, orderId, order.getPayAmount());

        updateStatus(order, OrderStatus.PAID, String.valueOf(buyerId), "支付");
        order.setPayTime(LocalDateTime.now());
        order.setShipExpireTime(LocalDateTime.now().plusHours(24));
        orderMainMapper.updateById(order);
        saveEvent(order, "ORDER_PAID");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long buyerId, String reason) {
        OrderMain order = require(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BizException("无权操作该订单");
        }
        if (!OrderStatus.cancellable(order.getStatus())) {
            throw new BizException("当前状态不可取消");
        }
        // 释放已锁定库存（按购买数量）
        productDubboService.unlockStock(order.getSkuId(), countOf(order));
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        orderMainMapper.updateById(order);
        statusLog(order.getId(), OrderStatus.PENDING_PAY, OrderStatus.CANCELLED,
            String.valueOf(buyerId), "取消：" + (reason == null ? "" : reason));
        saveEvent(order, "ORDER_CANCELLED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, Long sellerId, String company, String trackingNo) {
        OrderMain order = require(orderId);
        if (!order.getSellerId().equals(sellerId)) {
            throw new BizException("无权操作该订单");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BizException("订单状态不允许发货");
        }
        if (!StringUtils.hasText(trackingNo)) {
            throw new BizException("请填写物流单号");
        }
        ShippingInfo ship = new ShippingInfo();
        ship.setId(IdUtil.getSnowflakeNextId());
        ship.setOrderId(orderId);
        ship.setCompany(StringUtils.hasText(company) ? company : "默认物流");
        ship.setTrackingNo(trackingNo);
        ship.setStatus(1);
        ship.setShipTime(LocalDateTime.now());
        shippingInfoMapper.insert(ship);

        updateStatus(order, OrderStatus.SHIPPED, String.valueOf(sellerId), "发货");
        order.setShipTime(LocalDateTime.now());
        order.setConfirmExpireTime(LocalDateTime.now().plusDays(7));
        orderMainMapper.updateById(order);
        saveEvent(order, "ORDER_SHIPPED");
    }

    @Override
    @GlobalTransactional(name = "confirm-receipt", rollbackFor = Exception.class)
    public void confirmReceipt(Long orderId, Long buyerId) {
        OrderMain order = require(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BizException("无权操作该订单");
        }
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BizException("订单状态不允许确认收货");
        }
        // 放款给卖家（资金强一致）
        accountService.collect(order.getBuyerId(), order.getSellerId(), orderId, order.getPayAmount());
        // 商品正式售出：清除锁定库存（按购买数量）
        productDubboService.reduceStock(order.getSkuId(), countOf(order));

        updateStatus(order, OrderStatus.COMPLETED, String.valueOf(buyerId), "确认收货");
        order.setConfirmTime(LocalDateTime.now());
        order.setFinishTime(LocalDateTime.now());
        orderMainMapper.updateById(order);
        saveEvent(order, "ORDER_COMPLETED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(ReviewDTO dto) {
        OrderMain order = require(dto.getOrderId());
        if (!order.getBuyerId().equals(dto.getBuyerId())) {
            throw new BizException("无权操作该订单");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BizException("仅已完成的订单可评价");
        }
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BizException("评分必须为 1~5");
        }
        Long exist = orderReviewMapper.selectCount(
            new LambdaQueryWrapper<OrderReview>().eq(OrderReview::getOrderId, order.getId()));
        if (exist != null && exist > 0) {
            throw new BizException("该订单已评价");
        }
        OrderReview review = new OrderReview();
        review.setId(IdUtil.getSnowflakeNextId());
        review.setOrderId(order.getId());
        review.setProductId(order.getProductId());
        review.setBuyerId(order.getBuyerId());
        review.setSellerId(order.getSellerId());
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setImages(dto.getImages() == null ? null : JSONUtil.toJsonStr(dto.getImages()));
        review.setIsAnonymous(dto.getAnonymous() == null ? 0 : dto.getAnonymous());
        review.setStatus(1);
        orderReviewMapper.insert(review);

        updateStatus(order, OrderStatus.REVIEWED, String.valueOf(dto.getBuyerId()), "评价");
        order.setStatus(OrderStatus.REVIEWED);
        orderMainMapper.updateById(order);
    }

    @Override
    public OrderVO getById(Long orderId) {
        OrderMain order = require(orderId);
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusText(statusText(order.getStatus()));
        return vo;
    }

    // ---------- 辅助 ----------

    private OrderMain require(Long orderId) {
        OrderMain order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    private void updateStatus(OrderMain order, int toStatus, String operator, String reason) {
        int from = order.getStatus();
        order.setStatus(toStatus);
        statusLog(order.getId(), from, toStatus, operator, reason);
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

    private String buildOrderNo() {
        return "ZH" + IdUtil.getSnowflakeNextId();
    }

    /** 订单购买数量（缺省为 1，用于库存扣减补偿） */
    private int countOf(OrderMain order) {
        return order.getBuyerCount() == null || order.getBuyerCount() < 1
            ? 1 : order.getBuyerCount();
    }

    private String buildAddressSnapshot(OrderCreateDTO dto) {
        JSONObject addr = JSONUtil.createObj()
            .set("receiverName", dto.getReceiverName())
            .set("receiverMobile", dto.getReceiverMobile())
            .set("receiverAddress", dto.getReceiverAddress());
        return addr.toString();
    }

    private void saveEvent(OrderMain order, String eventType) {
        JSONObject payload = JSONUtil.createObj()
            .set("eventType", eventType)
            .set("orderId", order.getId())
            .set("orderNo", order.getOrderNo())
            .set("buyerId", order.getBuyerId())
            .set("sellerId", order.getSellerId())
            .set("productId", order.getProductId())
            .set("skuId", order.getSkuId())
            .set("payAmount", order.getPayAmount())
            .set("status", order.getStatus())
            .set("ts", System.currentTimeMillis());
        OrderMessage msg = new OrderMessage();
        msg.setId(IdUtil.getSnowflakeNextId());
        msg.setOrderId(order.getId());
        msg.setOrderNo(order.getOrderNo());
        msg.setTopic("order-events");
        msg.setEventType(eventType);
        msg.setPayload(payload.toString());
        eventPublisherService.saveAndPublish(msg);
    }

    public static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case OrderStatus.PENDING_PAY -> "待支付";
            case OrderStatus.PAID -> "已支付";
            case OrderStatus.SHIPPED -> "已发货";
            case OrderStatus.COMPLETED -> "已完成";
            case OrderStatus.REVIEWED -> "已评价";
            case OrderStatus.REFUNDING -> "退款中";
            case OrderStatus.REFUNDED -> "已退款";
            case OrderStatus.CANCELLED -> "已取消";
            default -> "未知";
        };
    }
}