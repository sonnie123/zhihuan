package com.zhihuan.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流信息表
 */
@Data
@TableName("shipping_info")
public class ShippingInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    /** 物流公司 */
    private String company;

    /** 物流单号 */
    private String trackingNo;

    /** 当前位置 */
    private String currentLocation;

    /** 状态 1已发货 2运输中 3已签收 */
    private Integer status;

    /** 发货时间 */
    private LocalDateTime shipTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}