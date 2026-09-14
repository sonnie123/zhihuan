package com.zhihuan.trade.common;

/**
 * 资金流水类型
 */
public final class FlowType {

    private FlowType() {}

    /** 冻结 */
    public static final int FREEZE = 1;
    /** 解冻 */
    public static final int UNFREEZE = 2;
    /** 收款 */
    public static final int COLLECT = 3;
    /** 退款 */
    public static final int REFUND = 4;
}