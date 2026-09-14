package com.zhihuan.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),

    SERVER_ERROR(500, "服务器内部错误"),

    REQUEST_LIMIT(429, "请求过于频繁，请稍后再试"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用，请稍后再试"),

    BIZ_ERROR(1000, "业务异常"),
    DATA_NOT_FOUND(1001, "数据不存在"),
    DATA_ALREADY_EXISTS(1002, "数据已存在"),
    STOCK_NOT_ENOUGH(1003, "库存不足"),
    ORDER_STATUS_ERROR(1004, "订单状态错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
