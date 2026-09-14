package com.zhihuan.trade.service;

import com.zhihuan.trade.dto.RefundApplyDTO;
import com.zhihuan.trade.vo.RefundOrderVO;

import java.util.List;

/**
 * 退款服务
 */
public interface RefundService {

    /** 买家申请退款 */
    Long applyRefund(RefundApplyDTO dto);

    /** 卖家审核退款（同意则退款入账 + 回补库存，Seata 全局事务） */
    void sellerAudit(Long refundId, Long sellerId, boolean agree);

    /** 退款单详情列表 */
    List<RefundOrderVO> listByOrder(Long orderId);
}