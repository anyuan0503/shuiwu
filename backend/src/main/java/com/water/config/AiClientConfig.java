package com.water.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 微服务配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "water.ai")
public class AiClientConfig {

    /** AI 微服务地址 */
    private String baseUrl = "http://127.0.0.1:8000";

    /** 调用超时秒数 */
    private long timeoutSeconds = 5;
}