package com.zhihuan.trade.api;

/**
 * 交易域内部 Dubbo 接口：供跨服务触发交易内部操作。
 * 复用 trade 服务本地同库事务与 Seata 全局事务能力，避免外部直连 DB。
 * 该接口放置于独立 API 模块 zhihuan-trade-api。
 */
public interface TradeDubboService {

    /**
     * 扫描并取消所有已逾期未支付的订单（释放锁定库存 + 状态流转）。
     * 作为 XXL-JOB 调度的"超时取消双保险"，与 TradeJob 本地 @Scheduled 并存。
     *
     * @return 本次成功取消的订单数
     */
    int cancelTimeoutOrders();
}