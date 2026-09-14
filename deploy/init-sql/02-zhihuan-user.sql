-- =====================================================================
-- 智换（Zhihuan）用户域数据库表结构
-- 数据库：zhihuan_user（由 01-init-databases.sql 创建）
-- 说明：
--   1. user_main / user_auth / user_profile / user_follow / user_realname
--      后续按 userId 哈希水平分片（8 库 16 表，ShardingSphere INLINE），
--      当前单库阶段按本 DDL 建表即可。
--   2. feed_main / im_message 分别服务于 feed / im 服务，因当前无独立库，
--      暂存于用户库（与关注/用户关系同域），水平扩展时建议拆分为独立库。
--   3. admin_* 系列服务于 zhihuan-admin 运营后台的账号/权限体系。
-- =====================================================================

USE `zhihuan_user`;

-- ---------------------------------------------------------------------
-- 1. 用户主表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_main` (
    `id`              BIGINT       NOT NULL COMMENT '用户ID（雪花算法）',
    `username`        VARCHAR(64)  DEFAULT NULL COMMENT '用户名',
    `nickname`        VARCHAR(64)  NOT NULL COMMENT '昵称',
    `avatar`          VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `phone`           VARCHAR(128) DEFAULT NULL COMMENT '手机号（AES加密）',
    `email`           VARCHAR(128) DEFAULT NULL COMMENT '邮箱（AES加密）',
    `gender`          TINYINT      NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    `user_type`       TINYINT      NOT NULL DEFAULT 1 COMMENT '用户类型 1个人 2商家',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1正常 2封禁 3注销',
    `real_name_status` TINYINT     NOT NULL DEFAULT 0 COMMENT '实名状态 0未实名 1已实名',
    `credit_score`    INT          NOT NULL DEFAULT 80 COMMENT '信用分 0-100',
    `register_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `last_login_time` DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_phone` (`phone`),
    KEY `idx_register_time` (`register_time`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户主表';

-- ---------------------------------------------------------------------
-- 2. 用户认证表（账号密码 / 手机号 / 第三方登录）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_auth` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `identity_type` VARCHAR(16)  NOT NULL COMMENT '认证类型 PASSWORD/PHONE/WECHAT',
    `identifier`    VARCHAR(128) NOT NULL COMMENT '账号/手机号/openid',
    `credential`    VARCHAR(128) DEFAULT NULL COMMENT '密码hash/加密openid',
    `salt`          VARCHAR(32)  DEFAULT NULL COMMENT '密码盐',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_identity` (`user_id`, `identity_type`),
    KEY `idx_identifier` (`identifier`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户认证表';

-- ---------------------------------------------------------------------
-- 3. 用户画像表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_profile` (
    `user_id`         BIGINT   NOT NULL COMMENT '用户ID',
    `tags`            JSON     DEFAULT NULL COMMENT '标签数组',
    `preferences`     JSON     DEFAULT NULL COMMENT '偏好JSON',
    `category_weights` JSON    DEFAULT NULL COMMENT '类目权重JSON（推荐用）',
    `behavior_vector` JSON     DEFAULT NULL COMMENT '行为向量JSON（推荐用）',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户画像表';

-- ---------------------------------------------------------------------
-- 4. 关注关系表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_follow` (
    `id`            BIGINT   NOT NULL COMMENT '主键ID',
    `user_id`       BIGINT   NOT NULL COMMENT '关注者ID',
    `target_user_id` BIGINT  NOT NULL COMMENT '被关注者ID',
    `status`        TINYINT  NOT NULL DEFAULT 1 COMMENT '关系状态 1关注 0取消',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_user_id`),
    KEY `idx_target_user` (`target_user_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '关注关系表';

-- ---------------------------------------------------------------------
-- 5. 用户实名认证表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_realname` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `real_name`     VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名（AES加密）',
    `id_card`       VARCHAR(64)  DEFAULT NULL COMMENT '身份证号（AES加密）',
    `id_card_front` VARCHAR(255) DEFAULT NULL COMMENT '身份证正面图URL',
    `id_card_back`  VARCHAR(255) DEFAULT NULL COMMENT '身份证反面图URL',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待审核 1通过 2拒绝',
    `audit_time`    DATETIME     DEFAULT NULL COMMENT '审核时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户实名认证表';

-- ---------------------------------------------------------------------
-- 6. 用户黑名单表（平台封禁/拉黑记录）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_blacklist` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `user_id`     BIGINT       NOT NULL COMMENT '被拉黑用户ID',
    `reason`      VARCHAR(255) DEFAULT NULL COMMENT '拉黑原因',
    `operator`    VARCHAR(64)  DEFAULT NULL COMMENT '操作人（运营/系统）',
    `expire_time` DATETIME     DEFAULT NULL COMMENT '过期时间，NULL为永久',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1生效 0失效',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户黑名单表';

-- ---------------------------------------------------------------------
-- 7. 设备黑名单表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_device_blacklist` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `device_id`   VARCHAR(128) NOT NULL COMMENT '设备标识（设备指纹）',
    `user_id`     BIGINT       DEFAULT NULL COMMENT '关联用户ID（可选）',
    `reason`      VARCHAR(255) DEFAULT NULL COMMENT '拉黑原因',
    `expire_time` DATETIME     DEFAULT NULL COMMENT '过期时间，NULL为永久',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1生效 0失效',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_device_id` (`device_id`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '设备黑名单表';

-- ---------------------------------------------------------------------
-- 8. 收货地址表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_address` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `receiver_name` VARCHAR(32)  NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(32) NOT NULL COMMENT '收货人手机号（AES加密）',
    `province`      VARCHAR(32)  DEFAULT NULL COMMENT '省',
    `city`          VARCHAR(32)  DEFAULT NULL COMMENT '市',
    `district`      VARCHAR(32)  DEFAULT NULL COMMENT '区/县',
    `detail`        VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认地址 1是 0否',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '收货地址表';

-- =====================================================================
-- 9. Feed 流主表（zhihuan-feed 服务读扩散持久化，暂存用户库）
--    写扩散走 Redis Inbox，本表用于不活跃用户的读扩散兜底 + 数据回溯
-- =====================================================================
CREATE TABLE IF NOT EXISTS `feed_main` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID',
    `publisher_id` BIGINT       NOT NULL COMMENT '发布者用户ID',
    `product_id`   BIGINT       DEFAULT NULL COMMENT '关联商品ID',
    `feed_type`    TINYINT      NOT NULL DEFAULT 1 COMMENT 'Feed类型 1商品发布 2用户动态',
    `title`        VARCHAR(128) DEFAULT NULL COMMENT '标题',
    `image`        VARCHAR(255) DEFAULT NULL COMMENT '封面图',
    `price`        DECIMAL(10, 2) DEFAULT NULL COMMENT '价格',
    `content`      VARCHAR(1024) DEFAULT NULL COMMENT '内容摘要',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1有效 0删除',
    `publish_time` DATETIME     NOT NULL COMMENT '发布时间（Feed排序用）',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_publisher_time` (`publisher_id`, `publish_time` DESC),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_publish_time` (`publish_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Feed流主表';

-- =====================================================================
-- 10. IM 消息表（zhihuan-im 服务消息持久化，暂存用户库）
--     与 Redis 双写；AI 议价/客服消息 sender_id = 0 表示 AI 助手
-- =====================================================================
CREATE TABLE IF NOT EXISTS `im_message` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `sender_id`   BIGINT       NOT NULL COMMENT '发送者ID（0=AI助手）',
    `receiver_id` BIGINT       NOT NULL COMMENT '接收者ID',
    `msg_type`    TINYINT      NOT NULL DEFAULT 1 COMMENT '消息类型 1文本 2图片 3系统 4AI议价 5AI客服',
    `content`     TEXT         NOT NULL COMMENT '消息内容',
    `extra_data`  JSON         DEFAULT NULL COMMENT '扩展数据（议价结果等）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '消息状态 1已发送 2已读',
    `send_time`   DATETIME     NOT NULL COMMENT '发送时间',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation` (`sender_id`, `receiver_id`, `send_time`),
    KEY `idx_receiver_status` (`receiver_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'IM消息表';

-- =====================================================================
-- 11. 运营后台账号表（zhihuan-admin 服务使用，暂存用户库）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `admin_user` (
    `id`              BIGINT       NOT NULL COMMENT '主键ID',
    `username`        VARCHAR(64)  NOT NULL COMMENT '登录账号',
    `password`        VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name`       VARCHAR(32)  DEFAULT NULL COMMENT '姓名',
    `phone`           VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    `email`           VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
    `avatar`          VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `last_login_time` DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '运营后台账号表';

CREATE TABLE IF NOT EXISTS `admin_role` (
    `id`          BIGINT      NOT NULL COMMENT '主键ID',
    `role_name`   VARCHAR(32) NOT NULL COMMENT '角色名称',
    `role_code`   VARCHAR(32) NOT NULL COMMENT '角色编码',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '运营后台角色表';

CREATE TABLE IF NOT EXISTS `admin_permission` (
    `id`         BIGINT       NOT NULL COMMENT '主键ID',
    `parent_id`  BIGINT       NOT NULL DEFAULT 0 COMMENT '父权限ID（菜单树）',
    `perm_name`  VARCHAR(64)  NOT NULL COMMENT '权限名称',
    `perm_code`  VARCHAR(64)  NOT NULL COMMENT '权限编码（如 product:audit）',
    `perm_type`  TINYINT      NOT NULL DEFAULT 1 COMMENT '权限类型 1菜单 2按钮',
    `path`       VARCHAR(255) DEFAULT NULL COMMENT '前端路由路径',
    `icon`       VARCHAR(64)  DEFAULT NULL COMMENT '菜单图标',
    `sort`       INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_code` (`perm_code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '运营后台权限表';

CREATE TABLE IF NOT EXISTS `admin_user_role` (
    `id`           BIGINT NOT NULL COMMENT '主键ID',
    `admin_user_id` BIGINT NOT NULL COMMENT '账号ID',
    `role_id`      BIGINT NOT NULL COMMENT '角色ID',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`admin_user_id`, `role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '后台账号角色关联表';

CREATE TABLE IF NOT EXISTS `admin_role_permission` (
    `id`            BIGINT NOT NULL COMMENT '主键ID',
    `role_id`       BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '后台角色权限关联表';
