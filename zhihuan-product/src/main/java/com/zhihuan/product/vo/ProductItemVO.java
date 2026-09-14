package com.zhihuan.product.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品列表项
 */
@Data
public class ProductItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sellerId;

    private String sellerNickname;

    private String title;

    private String coverImage;

    private BigDecimal price;

    private Integer condition;

    private Long viewCount;

    private Long favoriteCount;

    private LocalDateTime publishTime;

    private List<String> tags;
}