package com.lihua.gateway.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

/**
 * 简单 JWT 验签工具（密钥由调用方传入——来自配置 token.tokenSecret，与 auth 服务签发密钥一致）
 */
public class JwtUtils {

    /**
     * 验证 jwt 是否合法
     */
    public static void verify(String jwtToken, String secret) {
        JWT
        .require(Algorithm.HMAC256(secret))
        .build()
        .verify(jwtToken);
    }
}
