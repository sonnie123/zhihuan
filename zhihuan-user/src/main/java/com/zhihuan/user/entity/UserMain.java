package com.zhihuan.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户主表 user_main
 */
@Data
@TableName("user_main")
public class UserMain implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID（雪花算法） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号（AES加密） */
    private String phone;

    /** 邮箱（AES加密） */
    private String email;

    /** 性别 0未知 1男 2女 */
    private Integer gender;

    /** 用户类型 1个人 2商家 */
    private Integer userType;

    /** 状态 1正常 2封禁 3注销 */
    private Integer status;

    /** 实名状态 0未实名 1已实名 */
    private Integer realNameStatus;

    /** 信用分 0-100 */
    private Integer creditScore;

    /** 注册时间 */
    private LocalDateTime registerTime;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}