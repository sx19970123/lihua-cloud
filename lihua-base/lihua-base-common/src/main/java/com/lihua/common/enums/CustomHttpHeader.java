package com.lihua.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自定义http请求头
 */
@AllArgsConstructor
@Getter
public enum CustomHttpHeader {

    /**
     * 时间戳
     */
    TIMESTAMP("Timestamp"),

    /**
     * IP
     */
    IP("Request-IP"),

    /**
     * 请求类型
     */
    CLIENT_TYPE("Client-Type"),

    /**
     * 链路追踪 id（网关生成、服务间透传；MDC 键名见 TraceIdUtils.MDC_KEY）
     */
    TRACE_ID("Trace-Id"),

    /**
     * 请求验证签名
     */
    SIGN(SignEnum.SIGN_KEY.getValue()),

    /**
     * Token
     */
    TOKEN(TokenEnum.TOKEN_KEY.getValue());

    private final String value;

}
