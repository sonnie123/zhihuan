package com.zhihuan.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单表（仅退款 / 退货退款）
 */
@Data
@TableName("refund_order")
public class RefundOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 退款单号（业务唯一） */
    private String refundNo;

    private Long orderId;

    private Long buyerId;

    private Long sellerId;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 退款原因类型 1质量问题 2描述不符 3不想要了 4其他 */
    private Integer reasonType;

    /** 退款原因描述 */
    private String reason;

    /** 退款凭证图片JSON */
    private String images;

    /** 卖家是否同意 0待处理 1同意 2拒绝 */
    private Integer sellerApproved;

    /** 状态 1待审核 2同意 3拒绝 4已退款 5已关闭 */
    private Integer status;

    /** 退款完成时间 */
    private LocalDateTime refundTime;

    /** 审核时间 */
    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;
}