package com.zhihuan.user.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息出参（详情）
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private Integer gender;

    private Integer userType;

    private Integer status;

    private Integer realNameStatus;

    private Integer creditScore;

    private LocalDateTime registerTime;

    private LocalDateTime lastLoginTime;
}