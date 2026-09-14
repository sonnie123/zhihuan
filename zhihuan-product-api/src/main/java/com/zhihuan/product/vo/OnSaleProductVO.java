package com.zhihuan.product.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 在售商品详情（供 trade 下单扣库存等跨服务调用）
 */
@Data
public class OnSaleProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sellerId;

    private Long categoryId;

    private String title;

    private String coverImage;

    private String description;

    private BigDecimal price;

    private Integer condition;

    /** 状态 3 在售才可下单 */
    private Integer status;

    private List<ProductSkuVO> skus;
}