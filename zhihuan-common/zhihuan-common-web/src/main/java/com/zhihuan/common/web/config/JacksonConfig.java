package com.zhihuan.common.web.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 统一序列化配置：
 * 1. Long -> String（防止前端 JS 精度丢失）
 * 2. 时间类型统一格式
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Long / long -> String
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            // 时间格式化（simpleDateFormat 需使用 SimpleDateFormat 兼容的模式字符串，
            // 不能传 DateTimeFormatter 的 toString()）
            builder.simpleDateFormat(DATE_TIME_PATTERN);
            builder.serializerByType(LocalDateTime.class,
                new LocalDateTimeSerializer(DATE_TIME));
            builder.deserializerByType(LocalDateTime.class,
                new LocalDateTimeDeserializer(DATE_TIME));
            builder.serializerByType(LocalDate.class,
                new LocalDateSerializer(DATE));
            builder.deserializerByType(LocalDate.class,
                new LocalDateDeserializer(DATE));
            builder.serializerByType(LocalTime.class,
                new LocalTimeSerializer(TIME));
            builder.deserializerByType(LocalTime.class,
                new LocalTimeDeserializer(TIME));
        };
    }
}
