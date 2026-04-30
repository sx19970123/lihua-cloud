package com.lihua.client.config;

import com.lihua.client.annotation.EnableHttpClients;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 @HttpExchange 自动扫描
 */
@Configuration
@EnableHttpClients(packages = "com.lihua")
public class ClientConfig {}
