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
     * 请求验证签名
     */
    SIGN(SignEnum.SIGN_KEY.getValue()),

    /**
     * Token
     */
    TOKEN(TokenEnum.TOKEN_KEY.getValue());

    private final String value;

}
