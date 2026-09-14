-- =====================================================================
-- 智换（Zhihuan）商品域数据库表结构
-- 数据库：zhihuan_product（由 01-init-databases.sql 创建）
-- 说明：
--   1. product_main / product_sku / product_tag 后续按 sellerId /
--      productId 哈希水平分片（4 库 8 表，ShardingSphere INLINE），
--      当前单库阶段按本 DDL 建表即可。
--   2. category 为少量低频数据，不分片。
-- =====================================================================

USE `zhihuan_product`;

-- ---------------------------------------------------------------------
-- 1. 商品主表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_main` (
    `id`             BIGINT        NOT NULL COMMENT '商品ID（雪花算法）',
    `seller_id`      BIGINT        NOT NULL COMMENT '卖家ID',
    `category_id`    BIGINT        NOT NULL COMMENT '类目ID',
    `title`          VARCHAR(128)  NOT NULL COMMENT '标题',
    `description`    TEXT          DEFAULT NULL COMMENT '描述',
    `cover_image`    VARCHAR(255)  DEFAULT NULL COMMENT '封面图URL',
    `images`         JSON          DEFAULT NULL COMMENT '商品图片URL数组',
    `price`          DECIMAL(10, 2) NOT NULL COMMENT '售价',
    `original_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '原价',
    `condition`      TINYINT       NOT NULL DEFAULT 1 COMMENT '成色 1全新 2几乎全新 3轻微使用 4明显使用',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1草稿 2审核中 3在售 4已售 5下架 6违规',
    `view_count`     BIGINT        NOT NULL DEFAULT 0 COMMENT '浏览量',
    `favorite_count` BIGINT        NOT NULL DEFAULT 0 COMMENT '收藏量',
    `ai_audit_status` TINYINT      NOT NULL DEFAULT 0 COMMENT 'AI审核状态 0未审核 1通过 2拒绝',
    `publish_time`   DATETIME      DEFAULT NULL COMMENT '上架时间',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version`        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_seller_status` (`seller_id`, `status`),
    KEY `idx_category_status` (`category_id`, `status`),
    KEY `idx_status_category_time` (`status`, `category_id`, `publish_time` DESC),
    KEY `idx_seller_status_time` (`seller_id`, `status`, `publish_time` DESC),
    KEY `idx_status_view_count` (`status`, `view_count` DESC),
    KEY `idx_publish_time` (`publish_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品主表';

-- ---------------------------------------------------------------------
-- 2. 商品 SKU 表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_sku` (
    `id`           BIGINT        NOT NULL COMMENT '主键ID',
    `product_id`   BIGINT        NOT NULL COMMENT '商品ID',
    `sku_name`     VARCHAR(64)   DEFAULT NULL COMMENT '规格名（颜色/内存等）',
    `sku_value`    VARCHAR(64)   DEFAULT NULL COMMENT '规格值',
    `price`        DECIMAL(10, 2) NOT NULL COMMENT 'SKU售价',
    `stock`        INT           NOT NULL DEFAULT 0 COMMENT '可用库存',
    `locked_stock` INT           NOT NULL DEFAULT 0 COMMENT '锁定库存（下单未支付）',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品SKU表';

-- ---------------------------------------------------------------------
-- 3. 商品类目表（多级类目树）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
    `id`        BIGINT       NOT NULL COMMENT '类目ID',
    `parent_id` BIGINT       NOT NULL DEFAULT 0 COMMENT '父类目ID（0为顶级）',
    `name`      VARCHAR(64)  NOT NULL COMMENT '类目名称',
    `level`     TINYINT      NOT NULL COMMENT '层级 1一级 2二级 3三级',
    `icon`      VARCHAR(255) DEFAULT NULL COMMENT '类目图标',
    `template`  JSON         DEFAULT NULL COMMENT '属性模板JSON',
    `sort`      INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`    TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品类目表';

-- ---------------------------------------------------------------------
-- 4. 商品标签表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_tag` (
    `id`         BIGINT      NOT NULL COMMENT '主键ID',
    `product_id` BIGINT      NOT NULL COMMENT '商品ID',
    `tag_name`   VARCHAR(32) NOT NULL COMMENT '标签名称',
    `tag_type`   TINYINT     NOT NULL DEFAULT 1 COMMENT '标签类型 1系统标签（AI生成） 2用户标签',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_tag_name` (`tag_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品标签表';

-- ---------------------------------------------------------------------
-- 5. 商品收藏表（详情页收藏 / 我的收藏列表）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_favorite` (
    `id`          BIGINT   NOT NULL COMMENT '主键ID',
    `user_id`     BIGINT   NOT NULL COMMENT '收藏用户ID',
    `product_id`  BIGINT   NOT NULL COMMENT '商品ID',
    `status`      TINYINT  NOT NULL DEFAULT 1 COMMENT '状态 1收藏 0取消',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品收藏表';
