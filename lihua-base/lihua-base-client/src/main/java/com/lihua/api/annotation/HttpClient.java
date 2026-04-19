package com.lihua.api.annotation;

import com.lihua.api.enums.SchemeEnum;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HttpClient 包路径注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpClient {

    /**
     * 服务名称
     */
    String value();

    /**
     * 访问超时时间（秒）
     * -1 使用默认配置
     */
    int readTimeout() default -1;

    /**
     * 请求协议
     */
    SchemeEnum scheme() default SchemeEnum.HTTP;
}
