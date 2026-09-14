package com.zhihuan.audit.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 违规词库表（Aho-Corasick 自动机数据源）
 */
@Data
@TableName("violation_word")
public class ViolationWord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 违规词 */
    private String word;

    /** 违规类别 1色情 2暴力 3政治敏感 4虚假宣传 5违禁品 0其他 */
    private Integer category;

    /** 严重程度 1轻微 2中等 3严重 */
    private Integer level;

    /** 命中次数（统计用） */
    private Long hitCount;

    /** 状态 0停用 1启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}