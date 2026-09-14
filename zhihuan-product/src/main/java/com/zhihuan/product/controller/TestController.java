package com.zhihuan.product.controller;

import com.zhihuan.common.result.Result;
import com.zhihuan.user.api.HelloDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 临时验证：跨服务 Dubbo 调用（阶段 2 正式开发后移除）
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    @DubboReference
    private HelloDubboService helloDubboService;

    @GetMapping("/dubbo")
    public Result<String> dubbo(@RequestParam(defaultValue = "zhihuan") String name) {
        return Result.success(helloDubboService.sayHello(name));
    }
}
