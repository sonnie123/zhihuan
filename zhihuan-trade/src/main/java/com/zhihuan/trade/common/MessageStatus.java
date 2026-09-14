package com.zhihuan.trade.common;

/**
 * 本地消息表状态
 */
public final class MessageStatus {

    private MessageStatus() {}

    /** 待发送 */
    public static final int PENDING = 0;
    /** 已发送 */
    public static final int SENT = 1;
    /** 失败 */
    public static final int FAILED = 2;
}