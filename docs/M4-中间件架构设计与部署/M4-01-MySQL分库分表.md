# M4-01: MySQL 数据库分库分表设计

## 1. 分库分表策略

### 1.1 总体原则

**按业务垂直拆分 + 关键表水平拆分**：
- 垂直拆分：按业务能力拆分为 8 个独立库
- 水平拆分：订单、商品、关注等大表按 userId 哈希分片

### 1.2 数据库清单

| 序号 | 数据库名 | 业务域 | 关键表 | 分片策略 |
|---|---|---|---|---|
| 1 | zhihuan_user | 用户域 | user_main, user_auth, user_profile, user_follow, user_realname | user_main 按 userId 哈希 8 库 16 表 |
| 2 | zhihuan_product | 商品域 | product_main, product_sku, category, product_tag | product_main 按 sellerId 哈希 4 库 8 表 |
| 3 | zhihuan_trade | 交易域 | order_main, order_status_log, fund_flow, refund_order | order_main 按 buyerId 哈希 8 库 16 表 |
| 4 | zhihuan_ai | AI 域 | ai_task, knowledge_base, bargain_session | ai_task 按 userId 哈希 4 库 4 表 |
| 5 | zhihuan_search | 搜索域 | search_hot_word, search_log | 单库不分片 |
| 6 | zhihuan_recommend | 推荐域 | user_feature, item_feature | user_feature 按 userId 哈希 4 库 4 表 |
| 7 | zhihuan_marketing | 营销域 | coupon, campaign, flash_sale, groupon | 单库不分片 |
| 8 | zhihuan_audit | 审核域 | audit_record, violation_word | violation_word 单库不分片 |

### 1.3 ShardingSphere 配置

```yaml
spring:
  shardingsphere:
    datasource:
      names: ds-user-0,ds-user-1,ds-user-2,ds-user-3,ds-user-4,ds-user-5,ds-user-6,ds-user-7

      # 8 个用户库配置（相同结构）
      ds-user-0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://mysql-user-0:3306/zhihuan_user_0
        username: zhihuan
        password: zhihuan123
      ds-user-1:
        type: com.zaxxer.hikari.HikariDataSource
        jdbc-url: jdbc:mysql://mysql-user-1:3306/zhihuan_user_1
        username: zhihuan
        password: zhihuan123
      # ... 其他库类似

    rules:
      sharding:
        # 用户表分片规则
        tables:
          user_main:
            actual-data-nodes: ds-user-$->{0..7}.user_main_$->{0..15}
            table-strategy:
              standard:
                sharding-column: id
                sharding-algorithm-name: user-table-inline
            database-strategy:
              standard:
                sharding-column: id
                sharding-algorithm-name: user-db-inline

        # 分片算法
        sharding-algorithms:
          user-db-inline:
            type: INLINE
            props:
              algorithm-expression: ds-user-$->{(id.hashCode() & Integer.MAX_VALUE) % 8}
          user-table-inline:
            type: INLINE
            props:
              algorithm-expression: user_main_$->{((id.hashCode() & Integer.MAX_VALUE) % 16)}
```

## 2. 关键表结构（DDL）

### 2.1 用户主表（user_main）

```sql
CREATE TABLE `user_main` (
    `id` BIGINT NOT NULL COMMENT '用户ID（雪花算法）',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    `nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(128) DEFAULT NULL COMMENT '手机号（AES加密）',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱（AES加密）',
    `gender` TINYINT DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    `user_type` TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型 1个人 2商家',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1正常 2封禁 3注销',
    `real_name_status` TINYINT NOT NULL DEFAULT 0 COMMENT '实名状态 0未实名 1已实名',
    `credit_score` INT NOT NULL DEFAULT 80 COMMENT '信用分 0-100',
    `register_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_login_time` DATETIME DEFAULT NULL,
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_phone` (`phone`),
    KEY `idx_register_time` (`register_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主表';
```

### 2.2 商品主表（product_main）

```sql
CREATE TABLE `product_main` (
    `id` BIGINT NOT NULL COMMENT '商品ID',
    `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
    `category_id` BIGINT NOT NULL COMMENT '类目ID',
    `title` VARCHAR(128) NOT NULL COMMENT '标题',
    `description` TEXT COMMENT '描述',
    `cover_image` VARCHAR(255) NOT NULL COMMENT '封面图',
    `images` TEXT COMMENT '商品图片JSON数组',
    `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `condition` TINYINT NOT NULL COMMENT '成色 1全新 2几乎全新 3轻微使用 4明显使用',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1草稿 2审核中 3在售 4已售 5下架 6违规',
    `view_count` BIGINT NOT NULL DEFAULT 0 COMMENT '浏览量',
    `favorite_count` BIGINT NOT NULL DEFAULT 0 COMMENT '收藏量',
    `ai_audit_status` TINYINT DEFAULT 0 COMMENT 'AI审核 0未审核 1通过 2拒绝',
    `publish_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_seller_status` (`seller_id`, `status`),
    KEY `idx_category_status` (`category_id`, `status`),
    KEY `idx_publish_time` (`publish_time`),
    KEY `idx_status_view_count` (`status`, `view_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品主表';
```

### 2.3 订单主表（order_main）

```sql
CREATE TABLE `order_main` (
    `id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `buyer_id` BIGINT NOT NULL COMMENT '买家ID',
    `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_id` BIGINT DEFAULT NULL,
    `product_title` VARCHAR(128) NOT NULL COMMENT '商品快照标题',
    `product_image` VARCHAR(255) DEFAULT NULL,
    `product_price` DECIMAL(10,2) NOT NULL COMMENT '商品快照价格',
    `shipping_fee` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '运费',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `address_snapshot` TEXT COMMENT '地址快照JSON',
    `status` TINYINT NOT NULL COMMENT '订单状态',
    `pay_expire_time` DATETIME DEFAULT NULL,
    `ship_expire_time` DATETIME DEFAULT NULL,
    `confirm_expire_time` DATETIME DEFAULT NULL,
    `idempotent_key` VARCHAR(64) DEFAULT NULL COMMENT '幂等键',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `pay_time` DATETIME DEFAULT NULL,
    `ship_time` DATETIME DEFAULT NULL,
    `confirm_time` DATETIME DEFAULT NULL,
    `finish_time` DATETIME DEFAULT NULL,
    `version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_status` (`buyer_id`, `status`),
    KEY `idx_seller_status` (`seller_id`, `status`),
    KEY `idx_idempotent_key` (`idempotent_key`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
```

### 2.4 关注关系表（user_follow）

```sql
CREATE TABLE `user_follow` (
    `id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL COMMENT '关注者',
    `target_user_id` BIGINT NOT NULL COMMENT '被关注者',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1关注 0取消',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_user_id`),
    KEY `idx_target_user` (`target_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注关系表';
```

## 3. 索引优化

### 3.1 商品表索引设计

```sql
-- 列表页查询：status + category_id + publish_time DESC
CREATE INDEX idx_status_category_time ON product_main(status, category_id, publish_time DESC);

-- 卖家商品列表
CREATE INDEX idx_seller_status_time ON product_main(seller_id, status, publish_time DESC);

-- 热门商品（排行榜）
CREATE INDEX idx_status_view ON product_main(status, view_count DESC);

-- 搜索结果（全文搜索走 ES，这里只做基础索引）
CREATE INDEX idx_status_time ON product_main(status, create_time DESC);
```

### 3.2 订单表索引设计

```sql
-- 买家订单列表（按状态 + 时间倒序）
CREATE INDEX idx_buyer_status_time ON order_main(buyer_id, status, create_time DESC);

-- 卖家订单列表
CREATE INDEX idx_seller_status_time ON order_main(seller_id, status, create_time DESC);

-- 订单超时扫描
CREATE INDEX idx_status_pay_expire ON order_main(status, pay_expire_time);
```

## 4. 数据迁移方案

### 4.1 分库分表演进路径

**阶段 1：单库（0-10 万用户）**
- 8 个业务库各自独立，无水平分片

**阶段 2：垂直分库后水平分表（10 万-100 万用户）**
- 大表（user_main, product_main, order_main）水平分片

**阶段 3：读写分离（100 万+用户）**
- 每个分片配置主从（一主两从）
- 读请求路由到从库

**阶段 4：分库分表 + 读写分离（1000 万+用户）**
- 完整 ShardingSphere 方案

### 4.2 数据迁移工具

- **ShardingSphere Scaling**：自动迁移
- **Canal + Kafka**：基于 Binlog 的增量同步
- **双写方案**：迁移期间新旧库双写

## 5. 主从复制 + 读写分离

### 5.1 主从架构

```
[应用]
   ↓ (ShardingSphere)
[主库 Master]  →  [从库 Slave-1] (读)
              →  [从库 Slave-2] (读)
```

### 5.2 ShardingSphere 配置

```yaml
spring:
  shardingsphere:
    rules:
      replica-query:
        data-sources:
          ds-user-0:
            primary-data-source-name: ds-user-0-master
            replica-data-source-names:
              - ds-user-0-slave-1
              - ds-user-0-slave-2
            load-balancer-name: round-robin
        load-balancers:
          round-robin:
            type: ROUND_ROBIN
```

## 6. 备份与恢复

### 6.1 备份策略

| 备份类型 | 频率 | 保留时间 | 工具 |
|---|---|---|---|
| 全量备份 | 每天 3:00 | 30 天 | mysqldump / xtrabackup |
| 增量备份 | 每小时 | 7 天 | xtrabackup binlog |
| Binlog 备份 | 实时 | 30 天 | mysqlbinlog |

### 6.2 恢复演练

```bash
# xtrabackup 全量恢复
xtrabackup --prepare --target-dir=/backup/2026-09-13
xtrabackup --copy-back --target-dir=/backup/2026-09-13

# Binlog Point-in-Time 恢复
mysqlbinlog --start-datetime="2026-09-13 10:00:00" \
            --stop-datetime="2026-09-13 11:00:00" \
            /var/log/mysql/binlog.000123 | mysql -uroot -p
```

## 7. 监控指标

| 指标 | 监控项 | 告警阈值 |
|---|---|---|
| QPS | 每秒查询数 | > 5000 告警 |
| 慢查询 | 慢查询率 | > 0.1% 告警 |
| 连接数 | 活跃连接 | > 80% 告警 |
| 主从延迟 | Seconds_Behind_Master | > 5s 告警 |
| 磁盘空间 | 使用率 | > 80% 告警 |
| 死锁 | 死锁次数 | > 0 告警 |

## 8. 简历话术

> 设计 8 个业务库垂直拆分 + 关键表水平分片（ShardingSphere），支撑 1000 万+ 用户数据存储；订单表按 buyerId 哈希 8 库 16 表（128 分片），商品表按 sellerId 哈希 4 库 8 表；引入主从复制 + 读写分离，每个分片一主两从，读请求自动负载均衡；设计每日全量备份 + 每小时增量备份 + Binlog 实时备份的三级备份策略，支持 Point-in-Time 恢复。
