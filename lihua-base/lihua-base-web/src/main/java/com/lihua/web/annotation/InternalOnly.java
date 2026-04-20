package com.lihua.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalOnly {

    /**
     * 超时时间
     * 验证内部请求时间，超过 timeout 后则拒绝请求
     */
    int timeout() default 10000;
}
