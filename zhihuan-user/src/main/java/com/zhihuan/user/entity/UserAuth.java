package com.zhihuan.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户认证表 user_auth
 */
@Data
@TableName("user_auth")
public class UserAuth implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（雪花算法） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 认证类型 PASSWORD/PHONE/WECHAT */
    private String identityType;

    /** 账号/手机号/openid */
    private String identifier;

    /** 密码hash/加密openid */
    private String credential;

    /** 密码盐 */
    private String salt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}