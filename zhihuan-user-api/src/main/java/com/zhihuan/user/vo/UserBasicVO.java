package com.zhihuan.user.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户基础信息出参（跨服务/列表场景，脱敏）
 */
@Data
public class UserBasicVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String nickname;

    private String avatar;

    private Integer userType;

    private Integer creditScore;

    /** 是否被当前用户关注（可选，无关注信息时为空） */
    private java.util.List<Long> userIds;
}