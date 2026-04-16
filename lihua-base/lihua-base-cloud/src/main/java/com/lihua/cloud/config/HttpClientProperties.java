package com.lihua.cloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "http.client")
public class HttpClientProperties {

    /**
     * 连接超时时间
     */
    private Duration connectTimeout;

    /**
     * 访问超时时间
     */
    private Duration readTimeout;
}
