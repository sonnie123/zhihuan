package com.zhihuan.trade.common;

/**
 * 退款单状态
 */
public final class RefundStatus {

    private RefundStatus() {}

    /** 待审核 */
    public static final int PENDING = 1;
    /** 卖家同意 */
    public static final int APPROVED = 2;
    /** 卖家拒绝 */
    public static final int REJECTED = 3;
    /** 已退款 */
    public static final int REFUNDED = 4;
    /** 已关闭 */
    public static final int CLOSED = 5;
}