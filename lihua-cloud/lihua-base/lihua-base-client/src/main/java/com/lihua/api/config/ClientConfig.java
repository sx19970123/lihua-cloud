package com.lihua.api.config;

import com.lihua.api.annotation.EnableHttpClients;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 @HttpExchange 自动扫描
 */
@Configuration
@EnableHttpClients(packages = "com.lihua")
public class ClientConfig {}
