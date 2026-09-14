package com.zhihuan.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水表
 */
@Data
@TableName("fund_flow")
public class FundFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 流水号（业务唯一） */
    private String flowNo;

    private Long orderId;

    /** 操作人/资金归属人 */
    private Long userId;

    /** 流水类型 1冻结 2解冻 3收款 4退款 */
    private Integer flowType;

    /** 变动金额 */
    private BigDecimal amount;

    /** 变动后可用余额（对账用） */
    private BigDecimal balanceAfter;

    /** 状态 1成功 2失败 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}