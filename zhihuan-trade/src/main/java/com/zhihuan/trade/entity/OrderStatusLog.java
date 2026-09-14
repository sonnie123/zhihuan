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
 * 订单状态流转日志表
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    /** 原状态 */
    private Integer fromStatus;

    /** 目标状态 */
    private Integer toStatus;

    /** 操作人（用户ID/系统） */
    private String operator;

    /** 操作原因 */
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}