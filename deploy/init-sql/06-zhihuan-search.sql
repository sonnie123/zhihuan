-- =====================================================================
-- 智换（Zhihuan）搜索域数据库表结构
-- 数据库：zhihuan_search（由 01-init-databases.sql 创建）
-- 说明：
--   1. 商品全文/向量检索数据在 Elasticsearch（product_index）与 Milvus
--      （product_vectors），本库仅存统计与日志类数据，单库不分片。
--   2. search_hot_word 由 Kafka Stream 统计 + XXL-JOB 定时持久化。
-- =====================================================================

USE `zhihuan_search`;

-- ---------------------------------------------------------------------
-- 1. 搜索热词表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `search_hot_word` (
    `id`           BIGINT      NOT NULL COMMENT '主键ID',
    `keyword`      VARCHAR(64) NOT NULL COMMENT '搜索关键词',
    `search_count` BIGINT      NOT NULL DEFAULT 0 COMMENT '搜索次数',
    `stat_date`    DATE        NOT NULL COMMENT '统计日期',
    `window_start` DATETIME    DEFAULT NULL COMMENT '统计窗口开始时间',
    `window_end`   DATETIME    DEFAULT NULL COMMENT '统计窗口结束时间',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keyword_date` (`keyword`, `stat_date`),
    KEY `idx_date_count` (`stat_date`, `search_count` DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '搜索热词表';

-- ---------------------------------------------------------------------
-- 2. 搜索日志表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `search_log` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID',
    `user_id`      BIGINT       DEFAULT NULL COMMENT '搜索用户ID（未登录为空）',
    `keyword`      VARCHAR(128) NOT NULL COMMENT '搜索关键词',
    `category_id`  BIGINT       DEFAULT NULL COMMENT '类目筛选',
    `min_price`    DECIMAL(10, 2) DEFAULT NULL COMMENT '价格区间下限',
    `max_price`    DECIMAL(10, 2) DEFAULT NULL COMMENT '价格区间上限',
    `condition`    TINYINT      DEFAULT NULL COMMENT '成色筛选',
    `result_count` INT          NOT NULL DEFAULT 0 COMMENT '结果数量',
    `ip`           VARCHAR(64)  DEFAULT NULL COMMENT '客户端IP',
    `device`       VARCHAR(64)  DEFAULT NULL COMMENT '设备信息',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_keyword_time` (`keyword`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '搜索日志表';
