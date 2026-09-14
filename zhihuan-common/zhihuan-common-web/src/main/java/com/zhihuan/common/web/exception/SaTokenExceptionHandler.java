package com.zhihuan.common.web.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.zhihuan.common.result.Result;
import com.zhihuan.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 异常处理：未登录访问受保护接口时返回统一的 401 信封，
 * 而不是落入兜底 Exception 处理器返回 500。
 * Order 最高优先级，确保先于 GlobalExceptionHandler 的 Exception 兜底。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SaTokenExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("[NotLoginException] type={}", e.getType());
        return Result.fail(ResultCode.UNAUTHORIZED);
    }
}
