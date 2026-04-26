package com.lihua.web.config;

import com.lihua.web.interceptor.InternalRequestInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InternalRequestConfig implements WebMvcConfigurer {

    @Resource
    private InternalRequestInterceptor internalRequestInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器，拦截所有路径
        registry.addInterceptor(internalRequestInterceptor).addPathPatterns("/**");
    }
}