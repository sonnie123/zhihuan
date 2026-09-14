package com.zhihuan.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户画像表 user_profile
 */
@Data
@TableName("user_profile")
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID（主键） */
    @TableId
    private Long userId;

    /** 标签数组 */
    private String tags;

    /** 偏好JSON */
    private String preferences;

    /** 类目权重JSON（推荐用） */
    private String categoryWeights;

    /** 行为向量JSON（推荐用） */
    private String behaviorVector;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}