package com.zhihuan.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品保存请求（创建/编辑草稿）
 */
@Data
public class ProductSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "类目不能为空")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    private String coverImage;

    /** 图片URL数组（存 JSON） */
    private List<String> images;

    @NotNull(message = "售价不能为空")
    @Min(value = 0, message = "售价不能为负")
    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 成色 1全新 2几乎全新 3轻微使用 4明显使用 */
    @NotNull(message = "成色不能为空")
    private Integer condition;

    @NotEmpty(message = "SKU 不能为空")
    private List<SkuItemDTO> skus;

    /** 用户标签 */
    private List<String> tags;
}