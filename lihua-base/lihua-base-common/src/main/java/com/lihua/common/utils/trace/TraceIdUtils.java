package com.lihua.common.utils.trace;

import java.util.UUID;

/**
 * 链路追踪 id 工具（头名见 CustomHttpHeader.TRACE_ID；生成点：网关过滤器与服务入口过滤器，服务间经 RPC 拦截器透传）
 */
public class TraceIdUtils {

    /**
     * MDC 中 traceId 的键名（日志 pattern 经 %X{traceId} 输出）
     */
    public static final String MDC_KEY = "traceId";

    private TraceIdUtils() {
    }

    /**
     * 生成 16 位 hex 随机 traceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
