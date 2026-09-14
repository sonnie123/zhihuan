-- =====================================================================
-- 智换（Zhihuan）AI 域数据库表结构
-- 数据库：zhihuan_ai（由 01-init-databases.sql 创建）
-- 说明：
--   1. ai_task / bargain_session 后续按 userId 哈希水平分片
--      （4 库 4 表，ShardingSphere INLINE），当前单库阶段按本 DDL 建表即可。
--   2. 商品向量、FAQ 向量、爆款描述向量存放在 Milvus（product_vectors /
--      faq_vectors / hot_description_vectors），不入 MySQL。
--   3. 违规词库（violation_word）在 zhihuan_audit 库，由 audit 服务维护，
--      ai 服务通过 Dubbo 读取。
-- =====================================================================

USE `zhihuan_ai`;

-- ---------------------------------------------------------------------
-- 1. AI 任务记录表（上架/议价/审核/客服统一任务跟踪）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_task` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `task_type`   VARCHAR(16)  NOT NULL COMMENT '任务类型 PUBLISH/BARGAIN/AUDIT/CUSTOMER',
    `user_id`     BIGINT       DEFAULT NULL COMMENT '发起用户ID',
    `biz_id`      BIGINT       DEFAULT NULL COMMENT '业务对象ID（商品/订单等）',
    `input`       JSON         DEFAULT NULL COMMENT '输入内容',
    `output`      JSON         DEFAULT NULL COMMENT '输出结果',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待执行 1执行中 2成功 3失败',
    `cost_ms`     BIGINT       DEFAULT NULL COMMENT '耗时（毫秒）',
    `token_used`  INT          DEFAULT NULL COMMENT '消耗token数',
    `error_msg`   VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `finish_time` DATETIME     DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_type_status` (`task_type`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI任务记录表';

-- ---------------------------------------------------------------------
-- 2. RAG 知识库表（估价/文案/FAQ/平台规则）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `kb_type`     VARCHAR(16)  NOT NULL COMMENT '知识库类型 PRICE/DESCRIPTION/FAQ/POLICY',
    `title`       VARCHAR(255) NOT NULL COMMENT '标题',
    `content`     TEXT         NOT NULL COMMENT '内容',
    `tags`        JSON         DEFAULT NULL COMMENT '标签数组',
    `source`      VARCHAR(128) DEFAULT NULL COMMENT '来源',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1有效 0下线',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_type` (`kb_type`, `status`),
    KEY `idx_title` (`title`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG知识库表';

-- ---------------------------------------------------------------------
-- 3. AI 议价会话表（Multi-Agent 议价）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bargain_session` (
    `id`             BIGINT        NOT NULL COMMENT '主键ID',
    `product_id`     BIGINT        NOT NULL COMMENT '商品ID',
    `buyer_id`       BIGINT        NOT NULL COMMENT '买家ID',
    `seller_id`      BIGINT        NOT NULL COMMENT '卖家ID',
    `original_price` DECIMAL(10, 2) NOT NULL COMMENT '商品标价',
    `buyer_offer`    DECIMAL(10, 2) DEFAULT NULL COMMENT '买家首次出价',
    `current_price`  DECIMAL(10, 2) DEFAULT NULL COMMENT '当前协商价格',
    `final_price`    DECIMAL(10, 2) DEFAULT NULL COMMENT '最终成交价',
    `max_rounds`     INT           NOT NULL DEFAULT 10 COMMENT '最大议价轮次',
    `current_round`  INT           NOT NULL DEFAULT 0 COMMENT '当前轮次',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1进行中 2达成 3失败',
    `order_id`       BIGINT        DEFAULT NULL COMMENT '成交后生成的订单ID',
    `conversation`   JSON          DEFAULT NULL COMMENT '完整对话记录JSON（冗余快照）',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI议价会话表';

-- ---------------------------------------------------------------------
-- 4. 议价轮次记录表（逐轮明细，供议价策略学习）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bargain_round` (
    `id`         BIGINT        NOT NULL COMMENT '主键ID',
    `session_id` BIGINT        NOT NULL COMMENT '议价会话ID',
    `round_no`   INT           NOT NULL COMMENT '轮次',
    `agent_type` VARCHAR(16)   NOT NULL COMMENT 'Agent类型 BUYER/SELLER/SUPERVISOR',
    `price`      DECIMAL(10, 2) DEFAULT NULL COMMENT '本轮出价',
    `message`    VARCHAR(512)  DEFAULT NULL COMMENT '本轮话术',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`, `round_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '议价轮次记录表';

-- ---------------------------------------------------------------------
-- 5. AI 审核记录表（商品/评论/画像等内容的 AI 审核结果）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_audit_record` (
    `id`          BIGINT        NOT NULL COMMENT '主键ID',
    `target_id`   BIGINT        NOT NULL COMMENT '审核对象ID',
    `target_type` VARCHAR(16)   NOT NULL COMMENT '审核对象类型 PRODUCT/COMMENT/PROFILE',
    `rule_result` TINYINT       DEFAULT NULL COMMENT '规则引擎结果 1通过 2违规',
    `ai_result`   TINYINT       DEFAULT NULL COMMENT 'AI审核结果 1通过 2违规',
    `final_result` TINYINT      NOT NULL COMMENT '最终结果 1通过 2违规 3待人工',
    `hit_rules`   JSON          DEFAULT NULL COMMENT '命中的违规词列表',
    `ai_reason`   VARCHAR(512)  DEFAULT NULL COMMENT 'AI审核原因',
    `confidence`  DECIMAL(5, 2) DEFAULT NULL COMMENT 'AI置信度 0-1',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_id`, `target_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI审核记录表';

-- ---------------------------------------------------------------------
-- 6. Token 用量统计表（LLM Gateway 成本监控）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `token_usage` (
    `id`               BIGINT        NOT NULL COMMENT '主键ID',
    `task_type`        VARCHAR(32)   NOT NULL COMMENT '任务类型（PUBLISH/BARGAIN等）',
    `model`            VARCHAR(64)   DEFAULT NULL COMMENT '模型名称',
    `prompt_tokens`    INT           NOT NULL DEFAULT 0 COMMENT '输入token数',
    `completion_tokens` INT          NOT NULL DEFAULT 0 COMMENT '输出token数',
    `total_tokens`     INT           NOT NULL DEFAULT 0 COMMENT '总token数',
    `duration_ms`      BIGINT        DEFAULT NULL COMMENT '调用耗时（毫秒）',
    `cost`             DECIMAL(10, 4) DEFAULT NULL COMMENT '费用估算',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_type_time` (`task_type`, `create_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Token用量统计表';
