package com.zhihuan.trade.service;

import com.zhihuan.trade.dto.OrderCreateDTO;
import com.zhihuan.trade.dto.ReviewDTO;
import com.zhihuan.trade.vo.OrderVO;

/**
 * 订单核心交易服务
 */
public interface OrderService {

    /** 下单：幂等 + 锁库存 + 本地消息表 */
    Long createOrder(OrderCreateDTO dto);

    /** 支付：扣库存(锁定正式化) 由 Seata 全局事务 + 冻结资金 */
    void payOrder(Long orderId, Long buyerId);

    /** 取消（仅待支付）：释放库存 + 状态流转 */
    void cancelOrder(Long orderId, Long buyerId, String reason);

    /** 卖家发货 */
    void shipOrder(Long orderId, Long sellerId, String company, String trackingNo);

    /** 确认收货 + 放款 */
    void confirmReceipt(Long orderId, Long buyerId);

    /** 评价 */
    void review(ReviewDTO dto);

    /** 订单详情 */
    OrderVO getById(Long orderId);
}