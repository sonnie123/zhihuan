package com.zhihuan.product.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品主表
 */
@Data
@TableName("product_main")
public class ProductMain implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 卖家ID */
    private Long sellerId;

    /** 类目ID */
    private Long categoryId;

    private String title;

    private String description;

    private String coverImage;

    /** 商品图片URL数组 JSON */
    private String images;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 成色 1全新 2几乎全新 3轻微使用 4明显使用（condition为MySQL保留字，需转义列名） */
    @TableField("`condition`")
    private Integer condition;

    /** 状态 1草稿 2审核中 3在售 4已售 5下架 6违规 */
    private Integer status;

    private Long viewCount;

    private Long favoriteCount;

    /** AI审核状态 0未审核 1通过 2拒绝 */
    private Integer aiAuditStatus;

    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;
}