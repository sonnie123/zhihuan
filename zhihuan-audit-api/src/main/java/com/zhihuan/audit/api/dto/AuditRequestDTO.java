package com.zhihuan.audit.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 内容审核请求
 */
@Data
public class AuditRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 被审核内容ID */
    private Long targetId;

    /** 内容类型 1商品 2评论 3昵称 */
    private Integer targetType = 1;

    /** 待审核内容 */
    private String content;
}