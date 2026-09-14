package com.zhihuan.trade.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihuan.trade.api.TradeDubboService;
import com.zhihuan.trade.common.OrderStatus;
import com.zhihuan.trade.entity.OrderMain;
import com.zhihuan.trade.mapper.OrderMainMapper;
import com.zhihuan.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * trade 内部 Dubbo 服务实现：供 XXL-JOB 等跨服务远程触发。
 * cancelTimeoutOrders 作为"支付超时自动取消"的 XXL-JOB 双保险，
 * 与 TradeJob 本地 @Scheduled 并存（分片/故障转移由调度中心保证）。
 * 注意：取消走各订单私有本地事务（cancelOrder 含 @Transactional），不可在此包
 * @GlobalTransactional——循环内 try-catch 吞异常会导致 Seata 分支残留脏库。
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class TradeDubboServiceImpl implements TradeDubboService {

    private final OrderMainMapper orderMainMapper;
    private final OrderService orderService;

    @Override
    public int cancelTimeoutOrders() {
        List<OrderMain> expired = orderMainMapper.selectList(
            new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getStatus, OrderStatus.PENDING_PAY)
                .lt(OrderMain::getPayExpireTime, LocalDateTime.now()));
        int success = 0;
        for (OrderMain order : expired) {
            try {
                orderService.cancelOrder(order.getId(), order.getBuyerId(), "支付超时自动取消(XXL-JOB)");
                success++;
                log.info("[trade.dubbo][cancel-timeout] orderId={}, orderNo={}", order.getId(), order.getOrderNo());
            } catch (Exception e) {
                log.error("[trade.dubbo][cancel-timeout failed] orderId={}", order.getId(), e);
            }
        }
        log.info("[trade.dubbo][cancelTimeoutOrders] scanned={}, cancelled={}", expired.size(), success);
        return success;
    }
}