package com.zhihuan.user.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录结果
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 访问令牌 */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 用户类型 */
    private Integer userType;
}