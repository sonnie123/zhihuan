package com.zhihuan.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求
 */
@Data
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账号（用户名/手机号） */
    @NotBlank(message = "账号不能为空")
    private String identifier;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}