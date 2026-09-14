package com.zhihuan.product.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品列表查询条件
 */
@Data
public class ProductQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码 */
    private long pageNum = 1;

    /** 每页条数 */
    private long pageSize = 20;

    private String keyword;

    private Long categoryId;

    private Integer condition;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    /** 排序 1最新 2价格升 3价格降 4销量(浏览) */
    private Integer sort = 1;
}