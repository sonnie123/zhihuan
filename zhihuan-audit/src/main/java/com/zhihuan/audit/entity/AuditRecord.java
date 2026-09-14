package com.zhihuan.audit.entity;

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
 * 审核记录表（规则 + AI 双引擎审核结果落库）
 */
@Data
@TableName("audit_record")
public class AuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 被审核内容ID */
    private Long targetId;

    /** 内容类型 1商品 2评论 3昵称 */
    private Integer targetType;

    /** 内容快照 */
    private String content;

    /** 审核渠道 1规则引擎 2AI引擎 3人工 */
    private Integer auditChannel;

    /** 审核结果 1通过 2拒绝 3转人工 */
    private Integer result;

    /** 命中违规词数组 JSON */
    private String hitWords;

    /** AI审核理由 */
    private String aiReason;

    /** AI置信度（0-1） */
    private BigDecimal aiConfidence;

    /** 人工审核员ID（人工渠道） */
    private Long auditUserId;

    /** 审核耗时（毫秒） */
    private Integer durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}