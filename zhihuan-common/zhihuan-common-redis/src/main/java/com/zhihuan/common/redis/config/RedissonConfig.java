package com.zhihuan.common.redis.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 客户端配置，绑定 spring.data.redis.* 前缀（与 Spring Data Redis 共用一套配置）。
 */
@Configuration
public class RedissonConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.data.redis")
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties props) {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + props.getHost() + ":" + props.getPort())
            .setPassword(StringUtils.hasText(props.getPassword()) ? props.getPassword() : null)
            .setDatabase(props.getDatabase());
        return Redisson.create(config);
    }

    @Data
    public static class RedisProperties {
        private String host = "127.0.0.1";
        private int port = 6379;
        private String password;
        private int database = 0;
    }
}
