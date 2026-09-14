package com.zhihuan.product.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情
 */
@Data
public class ProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sellerId;

    private String sellerNickname;

    private Long categoryId;

    private String categoryName;

    private String title;

    private String description;

    private String coverImage;

    private List<String> images;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer condition;

    private Integer status;

    private Long viewCount;

    private Long favoriteCount;

    private LocalDateTime publishTime;

    private List<SkuVO> skus;

    private List<String> tags;

    /** 当前用户是否已收藏 */
    private Boolean favorited;
}