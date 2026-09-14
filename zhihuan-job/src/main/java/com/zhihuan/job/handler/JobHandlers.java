package com.zhihuan.job.handler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.zhihuan.trade.api.TradeDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB 任务集合。
 * 主任务：orderTimeoutScan —— 支付超时自动取消（与 trade 侧 TradeJob 本地 @Scheduled 形成双保险，
 * 由调度中心统一分片/故障转移，避免单点依赖）。
 */
@Slf4j
@Component
public class JobHandlers {

    @DubboReference(check = false)
    private TradeDubboService tradeDubboService;

    /**
     * [主] 订单超时扫描：远程触发 trade 服务的 cancelTimeoutOrders。
     * 由 XXL-JOB admin 定时调度（建议 cron 每分钟）。
     */
    @XxlJob("orderTimeoutScan")
    public void orderTimeoutScan() {
        XxlJobHelper.log("[job][orderTimeoutScan] start");
        try {
            int cancelled = tradeDubboService.cancelTimeoutOrders();
            XxlJobHelper.log("[job][orderTimeoutScan] cancelled={}", cancelled);
            XxlJobHelper.handleSuccess("cancelled=" + cancelled);
        } catch (Exception e) {
            XxlJobHelper.log("[job][orderTimeoutScan] failed", e);
            XxlJobHelper.handleFail(e.getMessage());
        }
    }

    /**
     * [备选，后续实现] 缓存预热：热点商品预加载到 Redis（依赖 product 域，暂标记留待后续）。
     */
    @XxlJob("hotProductCacheWarmup")
    public void hotProductCacheWarmup() {
        // TODO 后续实现：查询浏览量 Top N 商品 → 预热 Redis（复用 product Caffeine/缓存 key 约定）
        log.info("[job][hotProductCacheWarmup] 待实现，依赖商品域热度数据");
        XxlJobHelper.handleSuccess("pending");
    }

    /**
     * [备选，后续实现] 数据清理：归档过期历史订单（依赖 trade 归档能力）。
     */
    @XxlJob("dataCleanup")
    public void dataCleanup() {
        // TODO 后续实现：归档一年前已完结订单（复用 trade 数据保留策略）
        log.info("[job][dataCleanup] 待实现，依赖交易域归档能力");
        XxlJobHelper.handleSuccess("pending");
    }
}