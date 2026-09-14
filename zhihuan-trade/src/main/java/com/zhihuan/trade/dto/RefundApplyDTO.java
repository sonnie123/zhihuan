package com.zhihuan.trade.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 退款申请入参
 */
@Data
public class RefundApplyDTO {

    @NotNull
    private Long orderId;

    @NotNull
    private Long buyerId;

    /** 1质量问题 2描述不符 3不想要了 4其他 */
    private Integer reasonType;

    private String reason;

    /** 凭证图片 URL 列表 */
    private List<String> images;
}