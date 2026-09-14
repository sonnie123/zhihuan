-- =====================================================================
-- 智换（Zhihuan）内容审核域数据库表结构
-- 数据库：zhihuan_audit（由 01-init-databases.sql 创建）
-- 说明：
--   1. violation_word 违规词库由 audit 服务加载构建 Aho-Corasick
--      自动机（百万级词库毫秒级匹配），ai 服务通过 Dubbo 读取。
--   2. audit_record 记录规则引擎 + AI 双引擎的每次审核结果与命中规则，
--      供运营后台追溯；低置信度内容转 audit_manual_task 人工复审。
--   3. 本域单库不分片（ShardingSphere 不参与）。
-- =====================================================================

USE `zhihuan_audit`;

-- ---------------------------------------------------------------------
-- 1. 违规词库表（Aho-Corasick 自动机数据源）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `violation_word` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID',
    `word`         VARCHAR(128) NOT NULL COMMENT '违规词',
    `category`     TINYINT      NOT NULL DEFAULT 0 COMMENT '违规类别 1色情 2暴力 3政治敏感 4虚假宣传 5违禁品 0其他',
    `level`        TINYINT      NOT NULL DEFAULT 1 COMMENT '严重程度 1轻微 2中等 3严重',
    `hit_count`    BIGINT       NOT NULL DEFAULT 0 COMMENT '命中次数（统计用）',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`),
    KEY `idx_category_status` (`category`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '违规词库表';

-- ---------------------------------------------------------------------
-- 2. 审核记录表（规则 + AI 双引擎审核结果落库）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_record` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `target_id`     BIGINT       NOT NULL COMMENT '被审核内容ID',
    `target_type`   TINYINT      NOT NULL COMMENT '内容类型 1商品 2评论 3昵称',
    `content`       TEXT         COMMENT '内容快照',
    `audit_channel` TINYINT      NOT NULL COMMENT '审核渠道 1规则引擎 2AI引擎 3人工',
    `result`        TINYINT      NOT NULL COMMENT '审核结果 1通过 2拒绝 3转人工',
    `hit_words`     JSON         DEFAULT NULL COMMENT '命中违规词数组',
    `ai_reason`     VARCHAR(255) DEFAULT NULL COMMENT 'AI审核理由',
    `ai_confidence` DECIMAL(5, 4) DEFAULT NULL COMMENT 'AI置信度（0-1）',
    `audit_user_id` BIGINT       DEFAULT NULL COMMENT '人工审核员ID（人工渠道）',
    `duration_ms`   INT          NOT NULL DEFAULT 0 COMMENT '审核耗时（毫秒）',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_channel_time` (`audit_channel`, `create_time`),
    KEY `idx_result_time` (`result`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '审核记录表';

-- ---------------------------------------------------------------------
-- 3. 人工复审任务表（低置信度内容转人工工作流）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_manual_task` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID',
    `audit_record_id` BIGINT     NOT NULL COMMENT '关联审核记录ID',
    `target_id`     BIGINT       NOT NULL COMMENT '被审核内容ID',
    `target_type`   TINYINT      NOT NULL COMMENT '内容类型 1商品 2评论 3昵称',
    `content`       TEXT         COMMENT '内容快照',
    `reason`        VARCHAR(255) DEFAULT NULL COMMENT '转人工原因（如置信度过低）',
    `assignee_id`   BIGINT       DEFAULT NULL COMMENT '处理人ID',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待处理 1已处理 2已驳回',
    `audit_result`  TINYINT      DEFAULT NULL COMMENT '人工审核结论 1通过 2拒绝',
    `audit_user_id` BIGINT       DEFAULT NULL COMMENT '实际处理人ID',
    `audit_time`    DATETIME     DEFAULT NULL COMMENT '处理时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_target` (`target_type`, `target_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '人工复审任务表';

-- ---------------------------------------------------------------------
-- 4. 种子违规词（最小示例，供 AC 自动机 E2E 验证）
--    id 用固定 snowflake 风格值避免与运行时增量冲突
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `violation_word` (`id`, `word`, `category`, `level`, `hit_count`, `status`) VALUES
    (900000000000000001, '违禁品',   5, 3, 0, 1),
    (900000000000000002, '色情低俗', 1, 2, 0, 1),
    (900000000000000003, '暴力恐怖', 2, 3, 0, 1),
    (900000000000000004, '虚假宣传', 4, 2, 0, 1),
    (900000000000000005, '赌博',      5, 3, 0, 1),
    (900000000000000006, '管制刀具',  5, 3, 0, 1);
