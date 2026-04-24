package com.lihua.gateway.filter;

import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.enums.TokenEnum;
import com.lihua.common.model.response.response.StrResponse;
import com.lihua.gateway.utils.JwtUtils;
import com.lihua.gateway.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 校验token合法性
 */
@Component
@Slf4j
public class RequestTokenFilter implements GlobalFilter {

    @NullMarked
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<String> targetHeaders = exchange.getRequest().getHeaders().get(TokenEnum.TOKEN_KEY.getValue());

        if (targetHeaders == null || targetHeaders.isEmpty()) {
            return chain.filter(exchange);
        }

        String token = targetHeaders.get(0);

        try {
            JwtUtils.verify(token.replace(TokenEnum.TOKEN_PREFIX.getValue(), ""));
        } catch (Exception e) {
            log.error("非法Token {}", e.getMessage(), e);
            return WebUtils.renderJson(StrResponse.error(ResultCodeEnum.AUTHENTICATION_EXPIRED, "非法令牌"), exchange.getResponse());
        }

        return chain.filter(exchange);
    }
}
