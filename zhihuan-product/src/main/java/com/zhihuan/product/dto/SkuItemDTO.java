package com.zhihuan.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * SKU 子项
 */
@Data
public class SkuItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 编辑时用于定位已存在 SKU */
    private Long id;

    private String skuName;

    private String skuValue;

    @NotNull(message = "SKU 价格不能为空")
    @Min(value = 0, message = "SKU 价格不能为负")
    private BigDecimal price;

    @NotNull(message = "SKU 库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    private Integer stock;
}