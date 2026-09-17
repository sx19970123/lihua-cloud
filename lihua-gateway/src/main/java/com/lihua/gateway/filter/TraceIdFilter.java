package com.lihua.gateway.filter;

import com.lihua.common.enums.CustomHttpHeader;
import com.lihua.common.utils.trace.TraceIdUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 链路追踪过滤器：请求入口一律生成 traceId，经 mutate 覆写注入请求头随路由转发（外部携带的同名头被覆写——下游服务读到的必为网关产物），
 * 并回写响应头供前端排障定位
 */
@Order(-110)
@Component
public class TraceIdFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = TraceIdUtils.generateTraceId();
        exchange.getResponse().getHeaders().set(CustomHttpHeader.TRACE_ID.getValue(), traceId);
        ServerWebExchange newExchange = exchange.mutate()
                .request(builder -> builder.header(CustomHttpHeader.TRACE_ID.getValue(), traceId))
                .build();
        return chain.filter(newExchange);
    }
}
