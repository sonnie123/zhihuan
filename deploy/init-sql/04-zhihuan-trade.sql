-- =====================================================================
-- 智换（Zhihuan）交易域数据库表结构
-- 数据库：zhihuan_trade（由 01-init-databases.sql 创建）
-- 说明：
--   1. order_main 后续按 buyerId 哈希水平分片（8 库 16 表，ShardingSphere
--      INLINE），order_status_log / fund_flow / refund_order 等关联表
--      跟随订单分片，当前单库阶段按本 DDL 建表即可。
--   2. user_account（用户资金账户）由 trade 服务统一管理（Seata AT 强一致），
--      故归入交易库，与 fund_flow 同域便于对账。
-- =====================================================================

USE `zhihuan_trade`;

-- ---------------------------------------------------------------------
-- 1. 订单主表（担保交易）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_main` (
    `id`               BIGINT        NOT NULL COMMENT '订单ID（雪花算法）',
    `order_no`         VARCHAR(32)   NOT NULL COMMENT '订单号（业务唯一）',
    `buyer_id`         BIGINT        NOT NULL COMMENT '买家ID',
    `seller_id`        BIGINT        NOT NULL COMMENT '卖家ID',
    `product_id`       BIGINT        NOT NULL COMMENT '商品ID',
    `sku_id`           BIGINT        DEFAULT NULL COMMENT 'SKU ID',
    `product_title`    VARCHAR(128)  NOT NULL COMMENT '商品快照标题',
    `product_image`    VARCHAR(255)  DEFAULT NULL COMMENT '商品快照图片',
    `product_price`    DECIMAL(10, 2) NOT NULL COMMENT '商品快照价格',
    `buyer_count`      INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    `shipping_fee`     DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    `total_amount`     DECIMAL(10, 2) NOT NULL COMMENT '总金额',
    `pay_amount`       DECIMAL(10, 2) NOT NULL COMMENT '实付金额',
    `address_snapshot` JSON          DEFAULT NULL COMMENT '收货地址快照JSON',
    `status`           TINYINT       NOT NULL COMMENT '订单状态 1待支付 2已支付 3已发货 4已完成 5已评价 91退款中 92已退款 99已取消',
    `cancel_reason`    VARCHAR(255)  DEFAULT NULL COMMENT '取消原因',
    `pay_expire_time`  DATETIME      DEFAULT NULL COMMENT '支付超时时间',
    `ship_expire_time` DATETIME      DEFAULT NULL COMMENT '发货超时时间',
    `confirm_expire_time` DATETIME   DEFAULT NULL COMMENT '确认收货超时时间',
    `idempotent_key`   VARCHAR(64)   DEFAULT NULL COMMENT '幂等键（防重复下单）',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `pay_time`         DATETIME      DEFAULT NULL COMMENT '支付时间',
    `ship_time`        DATETIME      DEFAULT NULL COMMENT '发货时间',
    `confirm_time`     DATETIME      DEFAULT NULL COMMENT '确认收货时间',
    `finish_time`      DATETIME      DEFAULT NULL COMMENT '完成时间',
    `version`          INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_status_time` (`buyer_id`, `status`, `create_time` DESC),
    KEY `idx_seller_status_time` (`seller_id`, `status`, `create_time` DESC),
    KEY `idx_idempotent_key` (`idempotent_key`),
    KEY `idx_status_pay_expire` (`status`, `pay_expire_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单主表';

-- ---------------------------------------------------------------------
-- 2. 订单状态流转日志表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_status_log` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `order_id`    BIGINT       NOT NULL COMMENT '订单ID',
    `from_status` TINYINT      DEFAULT NULL COMMENT '原状态',
    `to_status`   TINYINT      NOT NULL COMMENT '目标状态',
    `operator`    VARCHAR(64)  DEFAULT NULL COMMENT '操作人（用户ID/系统）',
    `reason`      VARCHAR(255) DEFAULT NULL COMMENT '操作原因',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单状态流转日志表';

-- ---------------------------------------------------------------------
-- 3. 本地消息表（Saga + 本地消息表方案，保证事件可靠投递）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_message` (
    `id`             BIGINT       NOT NULL COMMENT '主键ID',
    `order_id`       BIGINT       NOT NULL COMMENT '订单ID',
    `order_no`       VARCHAR(32)  DEFAULT NULL COMMENT '订单号',
    `topic`          VARCHAR(64)  NOT NULL COMMENT '目标Topic',
    `event_type`     VARCHAR(64)  NOT NULL COMMENT '事件类型（ORDER_CREATED等）',
    `payload`        JSON         DEFAULT NULL COMMENT '事件载荷JSON',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待发送 1已发送 2失败',
    `retry_count`    INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_time` DATETIME    DEFAULT NULL COMMENT '下次重试时间',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '本地消息表';

-- ---------------------------------------------------------------------
-- 4. 资金流水表（所有资金变动可追溯）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `fund_flow` (
    `id`            BIGINT        NOT NULL COMMENT '主键ID',
    `flow_no`       VARCHAR(32)   NOT NULL COMMENT '流水号（业务唯一）',
    `order_id`      BIGINT        NOT NULL COMMENT '订单ID',
    `user_id`       BIGINT        NOT NULL COMMENT '操作人/资金归属人',
    `flow_type`     TINYINT       NOT NULL COMMENT '流水类型 1冻结 2解冻 3收款 4退款',
    `amount`        DECIMAL(10, 2) NOT NULL COMMENT '变动金额',
    `balance_after` DECIMAL(10, 2) DEFAULT NULL COMMENT '变动后可用余额（对账用）',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1成功 2失败',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_no` (`flow_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '资金流水表';

-- ---------------------------------------------------------------------
-- 5. 用户资金账户表（trade 服务管理，Seata AT 强一致）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_account` (
    `id`             BIGINT        NOT NULL COMMENT '主键ID',
    `user_id`        BIGINT        NOT NULL COMMENT '用户ID',
    `balance`        DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
    `frozen_balance` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额（担保交易中）',
    `total_income`   DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '累计收入',
    `total_expense`  DECIMAL(14, 2) NOT NULL DEFAULT 0.00 COMMENT '累计支出',
    `version`        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户资金账户表';

-- ---------------------------------------------------------------------
-- 6. 物流信息表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `shipping_info` (
    `id`              BIGINT       NOT NULL COMMENT '主键ID',
    `order_id`        BIGINT       NOT NULL COMMENT '订单ID',
    `company`         VARCHAR(64)  DEFAULT NULL COMMENT '物流公司',
    `tracking_no`     VARCHAR(64)  DEFAULT NULL COMMENT '物流单号',
    `current_location` VARCHAR(128) DEFAULT NULL COMMENT '当前位置',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1已发货 2运输中 3已签收',
    `ship_time`       DATETIME     DEFAULT NULL COMMENT '发货时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '物流信息表';

-- ---------------------------------------------------------------------
-- 7. 退款单表（仅退款 / 退货退款）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `refund_order` (
    `id`             BIGINT        NOT NULL COMMENT '主键ID',
    `refund_no`      VARCHAR(32)   NOT NULL COMMENT '退款单号（业务唯一）',
    `order_id`       BIGINT        NOT NULL COMMENT '订单ID',
    `buyer_id`       BIGINT        NOT NULL COMMENT '买家ID',
    `seller_id`      BIGINT        NOT NULL COMMENT '卖家ID',
    `refund_amount`  DECIMAL(10, 2) NOT NULL COMMENT '退款金额',
    `reason_type`    TINYINT       DEFAULT NULL COMMENT '退款原因类型 1质量问题 2描述不符 3不想要了 4其他',
    `reason`         VARCHAR(255)  DEFAULT NULL COMMENT '退款原因描述',
    `images`         JSON          DEFAULT NULL COMMENT '退款凭证图片',
    `seller_approved` TINYINT      NOT NULL DEFAULT 0 COMMENT '卖家是否同意 0待处理 1同意 2拒绝',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1待审核 2同意 3拒绝 4已退款 5已关闭',
    `refund_time`    DATETIME      DEFAULT NULL COMMENT '退款完成时间',
    `audit_time`     DATETIME      DEFAULT NULL COMMENT '审核时间',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version`        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '退款单表';

-- ---------------------------------------------------------------------
-- 8. 订单评价表（完成交易后互评，驱动信用分）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_review` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID',
    `order_id`     BIGINT       NOT NULL COMMENT '订单ID',
    `product_id`   BIGINT       NOT NULL COMMENT '商品ID',
    `buyer_id`     BIGINT       NOT NULL COMMENT '评价人（买家）',
    `seller_id`    BIGINT       NOT NULL COMMENT '被评价人（卖家）',
    `rating`       TINYINT      NOT NULL COMMENT '评分 1-5',
    `content`      VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    `images`       JSON         DEFAULT NULL COMMENT '评价图片',
    `is_anonymous` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否匿名 1是 0否',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0删除/违规',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_seller_id` (`seller_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单评价表';

-- =====================================================================
-- Seata AT 事务模式 undo_log 表（分布式事务回滚日志，Seata 必需）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT '分支事务ID',
    `xid`           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log 上下文，如序列化方式',
    `rollback_info` LONGBLOB     NOT NULL COMMENT '回滚信息',
    `log_status`    INT          NOT NULL COMMENT '0 正常状态 1 防御状态',
    `log_created`   DATETIME(6)  NOT NULL COMMENT '创建时间',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT '修改时间',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Seata AT 事务模式 undo 日志表';
