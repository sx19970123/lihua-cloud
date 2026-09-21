package com.lihua.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记内部 RPC 端点（由 InternalRequestInterceptor 验签放行；签名密钥配置见 lihua-common.yaml rpc 段）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalOnly {
}
