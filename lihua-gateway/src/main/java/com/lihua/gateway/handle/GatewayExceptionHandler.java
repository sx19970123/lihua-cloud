package com.lihua.gateway.handle;

import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.exception.BaseException;
import com.lihua.common.model.response.response.StrResponse;
import com.lihua.gateway.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@Order(-1)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    @NullMarked
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("网关层出现异常：{}", ex.getMessage(), ex);

        ServerHttpResponse response = exchange.getResponse();
        // 不进行处理已提交的响应
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 触发继承BaseException异常后取出
        if (ex instanceof BaseException baseException) {
            String respJson = StrResponse.error(baseException.getResultCodeEnum(), baseException.getMessage(), baseException.getData());
            return WebUtils.renderJson(respJson, response);
        } else {
            // 网关异常最后兜底
            return WebUtils.renderJson(HttpStatus.BAD_GATEWAY, StrResponse.error(ResultCodeEnum.BAD_GATEWAY_ERROR), response);
        }
    }
}
