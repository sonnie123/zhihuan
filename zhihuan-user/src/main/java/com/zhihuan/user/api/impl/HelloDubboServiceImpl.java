package com.zhihuan.user.api.impl;

import com.zhihuan.user.api.HelloDubboService;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 用户服务 Dubbo 提供方（临时验证用）
 */
@DubboService
public class HelloDubboServiceImpl implements HelloDubboService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name + "! from zhihuan-user";
    }
}
