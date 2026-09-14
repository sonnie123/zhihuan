package com.zhihuan.user.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 鉴权配置：
 * - /auth/login /auth/register 匿名访问
 * - 其余 /user/** /address/** /follow/** 需登录
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录校验拦截器
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter
                .match("/user/**", StpUtil::checkLogin)
                .match("/address/**", StpUtil::checkLogin)
                .match("/follow/**", StpUtil::checkLogin)))
            .addPathPatterns("/**");
    }
}