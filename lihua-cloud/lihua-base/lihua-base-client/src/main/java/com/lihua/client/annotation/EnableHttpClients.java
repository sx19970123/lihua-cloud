package com.lihua.client.annotation;

import com.lihua.client.registrar.HttpClientRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HttpClient启动扫包注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(HttpClientRegistrar.class)
public @interface EnableHttpClients {
    /**
     * 扫描包名
     */
    String[] packages();
}
