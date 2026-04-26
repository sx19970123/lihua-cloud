package com.lihua.gateway.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.lihua.common.enums.TokenEnum;
/**
 * 简单 JWT 加密解密工具类
 */
public class JwtUtils {

    /**
     * 验证 jwt 是否合法
     */
    public static void verify(String jwtToken) {
        JWT
        .require(Algorithm.HMAC256(TokenEnum.JWT_TOKEN_SECRET.getValue()))
        .build()
        .verify(jwtToken);
    }
}
