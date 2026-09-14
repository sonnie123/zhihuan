package com.zhihuan.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 下单入参
 */
@Data
public class OrderCreateDTO {

    /** 幂等键（防重复下单） */
    @NotBlank
    private String idempotentKey;

    /** 买家ID（当前登录用户） */
    @NotNull
    private Long buyerId;

    /** 商品ID */
    @NotNull
    private Long productId;

    /** SKU ID */
    private Long skuId;

    /** 购买数量（担保交易当前每单一件） */
    private Integer quantity;

    /** 收货人 */
    private String receiverName;

    /** 收货人手机号 */
    private String receiverMobile;

    /** 收货地址 */
    private String receiverAddress;
}