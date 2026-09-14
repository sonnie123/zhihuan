-- =====================================================================
-- 智换（Zhihuan）营销域数据库表结构
-- 数据库：zhihuan_marketing（由 01-init-databases.sql 创建）
-- 说明：
--   1. 营销域数据量相对可控，单库不分片（ShardingSphere 不参与）。
--   2. 秒杀/拼团/砍价库存与防超发以 Redis（Lua）为准，本库表为
--      活动配置与结果落库（Kafka 异步消费写入），保证最终一致。
--   3. coupon_user 记录用户领券/用券/过期状态；promotion_record
--      记录每笔订单的营销优惠明细，供对账与规则引擎追溯。
-- =====================================================================

USE `zhihuan_marketing`;

-- ---------------------------------------------------------------------
-- 1. 优惠券定义表（策略模式：满减 / 折扣 / 无门槛）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `coupon` (
    `id`              BIGINT       NOT NULL COMMENT '优惠券ID',
    `name`            VARCHAR(64)  NOT NULL COMMENT '优惠券名称',
    `type`            TINYINT      NOT NULL COMMENT '类型 1满减 2折扣 3无门槛',
    `amount`          DECIMAL(10, 2) DEFAULT NULL COMMENT '优惠金额（满减/无门槛）',
    `discount_rate`   TINYINT      DEFAULT NULL COMMENT '折扣率（如95表示95折，type=2时有效）',
    `min_amount`      DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '使用门槛金额',
    `total_count`     INT          NOT NULL DEFAULT 0 COMMENT '发行总量（0表示不限）',
    `received_count`  INT          NOT NULL DEFAULT 0 COMMENT '已领取数量',
    `per_user_limit`  INT          NOT NULL DEFAULT 1 COMMENT '每人限领数量',
    `valid_start_time` DATETIME    NOT NULL COMMENT '生效时间',
    `valid_end_time`  DATETIME     NOT NULL COMMENT '失效时间',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_type` (`status`, `type`),
    KEY `idx_valid_time` (`valid_start_time`, `valid_end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '优惠券定义表';

-- ---------------------------------------------------------------------
-- 2. 用户优惠券表（领取 / 使用 / 过期状态机）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `coupon_user` (
    `id`              BIGINT       NOT NULL COMMENT '主键ID',
    `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
    `coupon_id`       BIGINT       NOT NULL COMMENT '优惠券ID',
    `coupon_name`     VARCHAR(64)  NOT NULL COMMENT '优惠券名称快照',
    `coupon_type`     TINYINT      NOT NULL COMMENT '优惠券类型快照',
    `amount`          DECIMAL(10, 2) DEFAULT NULL COMMENT '优惠金额快照',
    `discount_rate`   TINYINT      DEFAULT NULL COMMENT '折扣率快照',
    `min_amount`      DECIMAL(10, 2) DEFAULT NULL COMMENT '使用门槛快照',
    `order_id`        BIGINT       DEFAULT NULL COMMENT '使用时的订单ID',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1未使用 2已使用 3已过期 4已退回',
    `receive_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    `use_time`        DATETIME     DEFAULT NULL COMMENT '使用时间',
    `expire_time`     DATETIME     NOT NULL COMMENT '过期时间',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_coupon` (`user_id`, `coupon_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_order_id` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户优惠券表';

-- ---------------------------------------------------------------------
-- 3. 营销活动定义表（模板方法模式：满减 / 秒杀 / 拼团 / 砍价）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `campaign` (
    `id`              BIGINT       NOT NULL COMMENT '活动ID',
    `name`            VARCHAR(64)  NOT NULL COMMENT '活动名称',
    `type`            TINYINT      NOT NULL COMMENT '活动类型 1满减 2秒杀 3拼团 4砍价',
    `threshold`       DECIMAL(10, 2) DEFAULT NULL COMMENT '满减门槛（type=1）',
    `discount`        DECIMAL(10, 2) DEFAULT NULL COMMENT '满减优惠金额（type=1）',
    `description`     VARCHAR(255) DEFAULT NULL COMMENT '活动说明',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0未开始 1进行中 2已结束 3已停用',
    `start_time`      DATETIME     NOT NULL COMMENT '开始时间',
    `end_time`        DATETIME     NOT NULL COMMENT '结束时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type_status` (`type`, `status`),
    KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '营销活动定义表';

-- ---------------------------------------------------------------------
-- 4. 秒杀活动表（Redis 预扣库存 + Kafka 异步下单）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `flash_sale` (
    `id`              BIGINT       NOT NULL COMMENT '秒杀活动ID',
    `campaign_id`     BIGINT       DEFAULT NULL COMMENT '所属活动ID（可为空）',
    `product_id`      BIGINT       NOT NULL COMMENT '商品ID',
    `flash_price`     DECIMAL(10, 2) NOT NULL COMMENT '秒杀价',
    `original_price`  DECIMAL(10, 2) DEFAULT NULL COMMENT '原价',
    `total_stock`     INT          NOT NULL COMMENT '秒杀总库存',
    `sold_stock`      INT          NOT NULL DEFAULT 0 COMMENT '已秒杀数量',
    `per_user_limit`  INT          NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    `start_time`      DATETIME     NOT NULL COMMENT '开始时间',
    `end_time`        DATETIME     NOT NULL COMMENT '结束时间',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0未开始 1进行中 2已结束 3已停用',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product` (`product_id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '秒杀活动表';

-- ---------------------------------------------------------------------
-- 5. 拼团活动表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `groupon` (
    `id`              BIGINT       NOT NULL COMMENT '拼团活动ID',
    `campaign_id`     BIGINT       DEFAULT NULL COMMENT '所属活动ID（可为空）',
    `product_id`      BIGINT       NOT NULL COMMENT '商品ID',
    `group_price`     DECIMAL(10, 2) NOT NULL COMMENT '拼团价',
    `original_price`  DECIMAL(10, 2) DEFAULT NULL COMMENT '原价',
    `group_size`      INT          NOT NULL DEFAULT 2 COMMENT '几人成团',
    `total_stock`     INT          NOT NULL COMMENT '拼团总库存',
    `sold_stock`      INT          NOT NULL DEFAULT 0 COMMENT '已成团数量',
    `start_time`      DATETIME     NOT NULL COMMENT '开始时间',
    `end_time`        DATETIME     NOT NULL COMMENT '结束时间',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0未开始 1进行中 2已结束 3已停用',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '拼团活动表';

-- ---------------------------------------------------------------------
-- 6. 拼团团单表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `groupon_group` (
    `id`              BIGINT       NOT NULL COMMENT '团单ID',
    `groupon_id`      BIGINT       NOT NULL COMMENT '拼团活动ID',
    `product_id`      BIGINT       NOT NULL COMMENT '商品ID',
    `leader_user_id`  BIGINT       NOT NULL COMMENT '团长用户ID',
    `member_count`    INT          NOT NULL DEFAULT 1 COMMENT '当前参团人数',
    `group_size`      INT          NOT NULL COMMENT '目标成团人数',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1拼团中 2已成团 3已失败',
    `expire_time`     DATETIME     NOT NULL COMMENT '成团截止时间',
    `complete_time`   DATETIME     DEFAULT NULL COMMENT '成团时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_groupon_status` (`groupon_id`, `status`),
    KEY `idx_leader` (`leader_user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '拼团团单表';

-- ---------------------------------------------------------------------
-- 7. 拼团成员表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `groupon_member` (
    `id`              BIGINT       NOT NULL COMMENT '主键ID',
    `group_id`        BIGINT       NOT NULL COMMENT '团单ID',
    `groupon_id`      BIGINT       NOT NULL COMMENT '拼团活动ID',
    `user_id`         BIGINT       NOT NULL COMMENT '参团用户ID',
    `order_id`        BIGINT       DEFAULT NULL COMMENT '对应订单ID',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1已参团 2已退款',
    `join_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '参团时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '拼团成员表';

-- ---------------------------------------------------------------------
-- 8. 砍价活动表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bargain` (
    `id`              BIGINT       NOT NULL COMMENT '砍价活动ID',
    `campaign_id`     BIGINT       DEFAULT NULL COMMENT '所属活动ID（可为空）',
    `product_id`      BIGINT       NOT NULL COMMENT '商品ID',
    `base_price`      DECIMAL(10, 2) NOT NULL COMMENT '原价（砍价起始价）',
    `floor_price`     DECIMAL(10, 2) NOT NULL COMMENT '底价（砍到底后最低成交价）',
    `max_cut_amount`  DECIMAL(10, 2) DEFAULT NULL COMMENT '单次最高可砍金额',
    `start_time`      DATETIME     NOT NULL COMMENT '开始时间',
    `end_time`        DATETIME     NOT NULL COMMENT '结束时间',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0未开始 1进行中 2已结束 3已停用',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '砍价活动表';

-- ---------------------------------------------------------------------
-- 9. 砍价会话表（发起人维度，砍到底自动下单）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bargain_record` (
    `id`               BIGINT       NOT NULL COMMENT '砍价会话ID',
    `bargain_id`       BIGINT       NOT NULL COMMENT '砍价活动ID',
    `product_id`       BIGINT       NOT NULL COMMENT '商品ID',
    `user_id`          BIGINT       NOT NULL COMMENT '发起人用户ID',
    `base_price`       DECIMAL(10, 2) NOT NULL COMMENT '起始价快照',
    `floor_price`      DECIMAL(10, 2) NOT NULL COMMENT '底价快照',
    `current_price`    DECIMAL(10, 2) NOT NULL COMMENT '当前价',
    `total_cut_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '累计砍掉金额',
    `order_id`         BIGINT       DEFAULT NULL COMMENT '砍到底自动创建的订单ID',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1砍价中 2已到底 3已失败 4已下单',
    `expire_time`      DATETIME     NOT NULL COMMENT '砍价截止时间',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_bargain` (`user_id`, `bargain_id`),
    KEY `idx_bargain_status` (`bargain_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '砍价会话表';

-- ---------------------------------------------------------------------
-- 10. 帮砍记录表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bargain_help_record` (
    `id`               BIGINT       NOT NULL COMMENT '主键ID',
    `bargain_record_id` BIGINT      NOT NULL COMMENT '砍价会话ID',
    `bargain_id`       BIGINT       NOT NULL COMMENT '砍价活动ID',
    `helper_id`        BIGINT       NOT NULL COMMENT '帮砍用户ID',
    `cut_amount`       DECIMAL(10, 2) NOT NULL COMMENT '本次砍掉金额',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '帮砍时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_helper` (`bargain_record_id`, `helper_id`),
    KEY `idx_helper` (`helper_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帮砍记录表';

-- ---------------------------------------------------------------------
-- 11. 营销优惠使用记录表（订单维度，责任链引擎计算结果落库）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `promotion_record` (
    `id`              BIGINT       NOT NULL COMMENT '主键ID',
    `order_id`        BIGINT       NOT NULL COMMENT '订单ID',
    `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
    `promotion_type`  TINYINT      NOT NULL COMMENT '优惠类型 1优惠券 2满减 3秒杀 4拼团 5砍价',
    `promotion_id`    BIGINT       NOT NULL COMMENT '优惠对象ID（优惠券/活动ID）',
    `promotion_name`  VARCHAR(64)  DEFAULT NULL COMMENT '优惠名称快照',
    `discount_amount` DECIMAL(10, 2) NOT NULL COMMENT '优惠金额',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_promotion` (`promotion_type`, `promotion_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '营销优惠使用记录表';
