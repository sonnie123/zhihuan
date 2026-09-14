package com.zhihuan.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详情 VO
 */
@Data
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long buyerId;

    private Long sellerId;

    private Long productId;

    private Long skuId;

    private String productTitle;

    private String productImage;

    private BigDecimal productPrice;

    private BigDecimal shippingFee;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    /** 状态 1待支付 2已支付 3已发货 4已完成 5已评价 91退款中 92已退款 99已取消 */
    private Integer status;

    /** 状态文案 */
    private String statusText;

    private String cancelReason;

    private String addressSnapshot;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime confirmTime;

    private LocalDateTime finishTime;
}