-- =====================================================================
-- 智换（Zhihuan）推荐域数据库表结构
-- 数据库：zhihuan_recommend（由 01-init-databases.sql 创建）
-- 说明：
--   1. user_feature 后续按 userId 哈希水平分片（4 库 4 表，ShardingSphere
--      INLINE），当前单库阶段按本 DDL 建表即可。
--   2. 实时特征以 Redis 为准（Kafka Stream 实时更新），本库表用于
--      离线特征计算（XXL-JOB）与协同过滤训练数据。
--   3. user_behavior 为埋点行为明细，供离线训练/审计回溯。
-- =====================================================================

USE `zhihuan_recommend`;

-- ---------------------------------------------------------------------
-- 1. 用户特征表（离线画像，供 CTR 预估 / 向量召回）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_feature` (
    `user_id`          BIGINT   NOT NULL COMMENT '用户ID',
    `preferred_categories` JSON  DEFAULT NULL COMMENT '偏好类目权重JSON',
    `preferred_brands` JSON     DEFAULT NULL COMMENT '偏好品牌权重JSON',
    `preferred_price_range` JSON DEFAULT NULL COMMENT '偏好价格区间JSON',
    `behavior_vector`  JSON     DEFAULT NULL COMMENT '行为向量JSON（Embedding）',
    `item_cf_matrix`   JSON     DEFAULT NULL COMMENT '协同过滤相似度矩阵JSON',
    `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户特征表';

-- ---------------------------------------------------------------------
-- 2. 物品特征表（商品侧画像，供相似商品推荐）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `item_feature` (
    `product_id`   BIGINT        NOT NULL COMMENT '商品ID',
    `category_id`  BIGINT        DEFAULT NULL COMMENT '类目ID',
    `seller_id`    BIGINT        DEFAULT NULL COMMENT '卖家ID',
    `brand`        VARCHAR(64)   DEFAULT NULL COMMENT '品牌',
    `title`        VARCHAR(128)  DEFAULT NULL COMMENT '商品标题',
    `price`        DECIMAL(10, 2) DEFAULT NULL COMMENT '价格',
    `tags`         JSON          DEFAULT NULL COMMENT '标签数组',
    `view_count`   BIGINT        NOT NULL DEFAULT 0 COMMENT '浏览量',
    `favorite_count` BIGINT      NOT NULL DEFAULT 0 COMMENT '收藏量',
    `order_count`  BIGINT        NOT NULL DEFAULT 0 COMMENT '成交数',
    `publish_time` DATETIME      DEFAULT NULL COMMENT '上架时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`product_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_seller_id` (`seller_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '物品特征表';

-- ---------------------------------------------------------------------
-- 3. 用户行为明细表（浏览/收藏/下单/搜索埋点，离线训练数据源）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_behavior` (
    `id`            BIGINT        NOT NULL COMMENT '主键ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `behavior_type` VARCHAR(16)   NOT NULL COMMENT '行为类型 VIEW/FAVORITE/ORDER/SEARCH',
    `product_id`    BIGINT        DEFAULT NULL COMMENT '商品ID',
    `category_id`   BIGINT        DEFAULT NULL COMMENT '类目ID',
    `brand`         VARCHAR(64)   DEFAULT NULL COMMENT '品牌',
    `price`         DECIMAL(10, 2) DEFAULT NULL COMMENT '商品价格',
    `device`        VARCHAR(64)   DEFAULT NULL COMMENT '设备信息',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_type_product` (`behavior_type`, `product_id`),
    KEY `idx_category_time` (`category_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户行为明细表';
