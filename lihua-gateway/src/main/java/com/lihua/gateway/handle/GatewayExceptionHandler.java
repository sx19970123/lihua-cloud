package com.lihua.gateway.handle;

import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.exception.BaseException;
import com.lihua.common.model.response.ResponseModel;
import com.lihua.common.model.response.Responses;
import com.lihua.gateway.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
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

        // 不进行处理已提交的响应
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        // 围栏端点自身的异常（如 Accept 协商失败）不再 dispatch 防循环，透传给默认错误处理
        if (exchange.getRequest().getPath().value().equals(WebUtils.FENCE_PATH)) {
            return Mono.error(ex);
        }

        ResponseModel<?> model;
        HttpStatus status = HttpStatus.OK;
        // 触发继承BaseException异常后取出；其余为网关异常最后兜底
        if (ex instanceof BaseException baseException) {
            model = Responses.error(baseException.getResultCodeEnum(), baseException.getMessage(), baseException.getData());
        } else {
            model = Responses.error(ResultCodeEnum.BAD_GATEWAY_ERROR);
            status = HttpStatus.BAD_GATEWAY;
        }

        // 错误响应经 dispatch 转入围栏端点，由消息转换管线按 Accept 协商写出
        return WebUtils.renderResponse(model, status, exchange);
    }
}
