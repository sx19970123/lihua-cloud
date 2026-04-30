package com.lihua.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "http.client")
public class ClientProperties {

    /**
     * 访问超时时间
     */
    private Duration responseTimeout = Duration.ofSeconds(30);

    /**
     * 连接超时时间
     */
    private Duration connectTimeout = Duration.ofSeconds(3);
}
