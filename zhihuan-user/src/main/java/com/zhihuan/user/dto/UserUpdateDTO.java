package com.zhihuan.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户资料更新请求
 */
@Data
public class UserUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 昵称 */
    @Size(max = 64, message = "昵称不能超过 64 字符")
    private String nickname;

    /** 头像URL */
    @Size(max = 255, message = "头像URL过长")
    private String avatar;

    /** 性别 0未知 1男 2女 */
    private Integer gender;

    /** 用户类型 1个人 2商家 */
    private Integer userType;
}