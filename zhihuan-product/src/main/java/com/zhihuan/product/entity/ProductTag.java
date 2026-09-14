package com.zhihuan.product.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品标签
 */
@Data
@TableName("product_tag")
public class ProductTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long productId;

    private String tagName;

    /** 标签类型 1系统标签 2用户标签 */
    private Integer tagType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}