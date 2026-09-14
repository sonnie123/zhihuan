package com.zhihuan.trade.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 订单评价入参
 */
@Data
public class ReviewDTO {

    @NotNull
    private Long orderId;

    @NotNull
    private Long buyerId;

    /** 评分 1-5 */
    @NotNull
    private Integer rating;

    private String content;

    private List<String> images;

    /** 是否匿名 1是 0否 */
    private Integer anonymous;
}