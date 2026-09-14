package com.zhihuan.common.dubbo.config;

import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dubbo 公共装配：
 * 注册中心（Nacos）与协议（Triple）在各自服务的 application.yml 中配置，
 * 这里统一 Provider/Consumer 的默认超时与重试策略：
 * - 超时 10s
 * - 重试 0 次（避免业务接口重复执行，幂等优先由业务自行保证）
 */
@Configuration
public class DubboConfig {

    @Bean
    public ProviderConfig dubboProviderConfig() {
        ProviderConfig config = new ProviderConfig();
        config.setTimeout(10000);
        config.setRetries(0);
        return config;
    }

    @Bean
    public ConsumerConfig dubboConsumerConfig() {
        ConsumerConfig config = new ConsumerConfig();
        config.setTimeout(10000);
        config.setRetries(0);
        return config;
    }
}
