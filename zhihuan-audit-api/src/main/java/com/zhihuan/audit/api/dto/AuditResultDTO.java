package com.zhihuan.audit.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 内容审核结果
 */
@Data
public class AuditResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int RESULT_PASS = 1;
    public static final int RESULT_REJECT = 2;
    public static final int RESULT_MANUAL = 3;

    /** 审核结果 1通过 2拒绝 3转人工 */
    private Integer result;

    /** 命中违规词 */
    private String hitWords;

    /** 审核说明 */
    private String message;

    /** 审核耗时（毫秒） */
    private Long durationMs;
}