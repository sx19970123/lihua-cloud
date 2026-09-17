package com.lihua.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/**
 * RPC 客户端超时与连接池配置（prefix=rpc，与服务间签名配置同段——见 lihua-common.yaml rpc 段）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rpc")
public class ClientProperties {

    /**
     * 访问超时时间
     */
    private Duration responseTimeout = Duration.ofSeconds(30);

    /**
     * 连接超时时间
     */
    private Duration connectTimeout = Duration.ofSeconds(3);

    /**
     * 等待从连接池租借连接的超时时间（不计入 responseTimeout，并发排队超此值快速失败而非傻等）
     */
    private Duration connectionRequestTimeout = Duration.ofSeconds(3);

    /**
     * 连接池最大总连接数
     */
    private Integer maxConnTotal = 200;

    /**
     * 连接池单路由（单下游服务）最大连接数，须不大于 maxConnTotal
     */
    private Integer maxConnPerRoute = 50;
}
