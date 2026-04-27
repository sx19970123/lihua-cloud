package com.lihua.client.annotation;

import com.lihua.client.enums.ExecutionModeEnum;
import com.lihua.client.enums.SchemeEnum;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RemoteClient 包路径注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteClient {

    /**
     * 服务名称
     */
    String serverName();

    /**
     * 请求协议
     */
    SchemeEnum scheme() default SchemeEnum.HTTP;

    /**
     * 执行模式
     * SYNC：同步执行
     * ASYNC：异步执行
     */
    ExecutionModeEnum executionMode() default ExecutionModeEnum.SYNC;
}
