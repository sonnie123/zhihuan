package com.zhihuan.product.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品 SKU 基础信息（供 trade 等跨服务调用，含价格与库存）
 */
@Data
public class ProductSkuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long productId;

    private String skuName;

    private String skuValue;

    private BigDecimal price;

    /** 可用库存 */
    private Integer stock;

    /** 锁定库存 */
    private Integer lockedStock;
}