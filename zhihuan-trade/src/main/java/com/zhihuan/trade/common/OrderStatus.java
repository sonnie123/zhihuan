package com.zhihuan.trade.common;

/**
 * 订单状态
 */
public final class OrderStatus {

    private OrderStatus() {}

    /** 待支付 */
    public static final int PENDING_PAY = 1;
    /** 已支付 */
    public static final int PAID = 2;
    /** 已发货 */
    public static final int SHIPPED = 3;
    /** 已完成 */
    public static final int COMPLETED = 4;
    /** 已评价 */
    public static final int REVIEWED = 5;
    /** 退款中 */
    public static final int REFUNDING = 91;
    /** 已退款 */
    public static final int REFUNDED = 92;
    /** 已取消 */
    public static final int CANCELLED = 99;

    /** 是否可支付的终态校验：仅待支付可支付 */
    public static boolean payable(int status) {
        return status == PENDING_PAY;
    }

    /** 仅待支付可取消 */
    public static boolean cancellable(int status) {
        return status == PENDING_PAY;
    }
}