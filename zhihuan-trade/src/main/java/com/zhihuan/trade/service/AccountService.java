package com.zhihuan.trade.service;

import java.math.BigDecimal;

/**
 * 用户资金账户服务（Seata AT 分布式事务能力由上层业务方法持有）
 */
public interface AccountService {

    /** 确保账户存在（不存在则创建） */
    void ensureAccount(Long userId);

    /** 充值（演示/测试用） */
    void deposit(Long userId, BigDecimal amount);

    /** 冻结买家资金 */
    void freeze(Long userId, Long orderId, BigDecimal amount);

    /** 解冻买家资金 */
    void unfreeze(Long userId, Long orderId, BigDecimal amount);

    /** 确认收货放款：解冻买家 + 收款给卖家 */
    void collect(Long buyerId, Long sellerId, Long orderId, BigDecimal amount);

    /** 退款：将冻结资金退回买家可用余额 */
    void refund(Long userId, Long orderId, BigDecimal amount);
}