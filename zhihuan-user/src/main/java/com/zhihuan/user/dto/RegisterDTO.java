package com.zhihuan.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 注册请求
 */
@Data
public class RegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 认证类型 默认 PASSWORD */
    private String identityType = "PASSWORD";

    /** 账号（用户名或手机号） */
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 64, message = "账号长度需在 3-64 之间")
    private String identifier;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间")
    private String password;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称不能超过 64 字符")
    private String nickname;

    /** 手机号（可选） */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    /** 用户类型 1个人 2商家 */
    private Integer userType = 1;
}