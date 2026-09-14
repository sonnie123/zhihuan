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
 * 订单主表（担保交易）
 */
@Data
@TableName("order_main")
public class OrderMain implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单号（业务唯一） */
    private String orderNo;

    /** 买家ID */
    private Long buyerId;

    /** 卖家ID */
    private Long sellerId;

    /** 商品ID */
    private Long productId;

    /** SKU ID */
    private Long skuId;

    /** 商品快照标题 */
    private String productTitle;

    /** 商品快照图片 */
    private String productImage;

    /** 商品快照价格（单价） */
    private BigDecimal productPrice;

    /** 购买数量 */
    private Integer buyerCount;

    /** 运费 */
    private BigDecimal shippingFee;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 收货地址快照JSON */
    private String addressSnapshot;

    /** 状态 1待支付 2已支付 3已发货 4已完成 5已评价 91退款中 92已退款 99已取消 */
    private Integer status;

    /** 取消原因 */
    private String cancelReason;

    /** 支付超时时间 */
    private LocalDateTime payExpireTime;

    /** 发货超时时间 */
    private LocalDateTime shipExpireTime;

    /** 确认收货超时时间 */
    private LocalDateTime confirmExpireTime;

    /** 幂等键（防重复下单） */
    private String idempotentKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime confirmTime;

    private LocalDateTime finishTime;

    @Version
    private Integer version;
}