package com.lihua.web.filter;

import com.lihua.common.enums.CustomHttpHeader;
import com.lihua.common.utils.trace.TraceIdUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 链路追踪过滤器：读取上游（网关）注入的 Trace-Id，缺失时自行生成，写入 MDC 供日志 pattern 输出
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(CustomHttpHeader.TRACE_ID.getValue());
        if (!StringUtils.hasText(traceId)) {
            traceId = TraceIdUtils.generateTraceId();
        }
        MDC.put(TraceIdUtils.MDC_KEY, traceId);
        // 响应头回写（直连服务端口绕过网关的场景，前端仍可拿到本次请求的 traceId）
        response.setHeader(CustomHttpHeader.TRACE_ID.getValue(), traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIdUtils.MDC_KEY);
        }
    }
}
