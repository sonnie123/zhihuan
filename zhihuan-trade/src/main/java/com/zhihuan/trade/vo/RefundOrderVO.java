package com.zhihuan.trade.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单 VO
 */
@Data
public class RefundOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String refundNo;

    private Long orderId;

    private Long buyerId;

    private Long sellerId;

    private BigDecimal refundAmount;

    private Integer reasonType;

    private String reason;

    /** 状态 1待审核 2同意 3拒绝 4已退款 5已关闭 */
    private Integer status;

    private String statusText;

    private LocalDateTime createTime;

    private LocalDateTime refundTime;
}