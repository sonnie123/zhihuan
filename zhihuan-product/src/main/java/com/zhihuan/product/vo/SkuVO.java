package com.zhihuan.product.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * SKU 视图
 */
@Data
public class SkuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    private String skuName;

    private String skuValue;

    private BigDecimal price;

    private Integer stock;

    private Integer lockedStock;
}