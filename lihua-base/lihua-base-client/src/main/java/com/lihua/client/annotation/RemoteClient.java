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
     * 响应等待超时默认值（秒）
     */
    int TIMEOUT_DEFAULT = 5;

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

    /**
     * 响应等待超时时间（秒）——响应等待是接口特征，归接口声明处声明；
     * 超时参数为启动期一次性构建（改值需重启），与配置键等价，故不设全局键。
     * 连接/等池超时为网络环境特征，统一走 rpc.connectTimeout / rpc.connectionRequestTimeout 全局配置
     */
    int timeout() default TIMEOUT_DEFAULT;
}
