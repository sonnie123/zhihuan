package com.zhihuan.common.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档统一配置。
 * 各服务可通过 springdoc 属性覆盖标题描述，或直接使用默认值。
 */
@Configuration
public class Knife4jConfig {

    @Value("${spring.application.name:zhihuan}")
    private String applicationName;

    @Bean
    public OpenAPI zhihuanOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(applicationName + " - 智换 API")
                .description("智换（Zhihuan）AI 驱动的二手交易微服务平台接口文档")
                .version("1.0.0")
                .contact(new Contact().name("zhihuan")));
    }
}
