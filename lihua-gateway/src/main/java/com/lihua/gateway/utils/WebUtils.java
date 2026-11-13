package com.lihua.gateway.utils;

import com.lihua.common.model.response.ResponseModel;
import com.lihua.common.utils.spring.SpringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class WebUtils {

    /**
     * 围栏层响应端点路由及模型/状态码 attribute key，与 FenceResponseController 共同构成转发协议
     */
    public static final String FENCE_PATH = "/fence/response";
    public static final String FENCE_MODEL_ATTR = "fenceResponseModel";
    public static final String FENCE_STATUS_ATTR = "fenceResponseStatus";

    /**
     * 将响应模型经 dispatch 转入 FenceResponseController，由消息转换管线完成内容协商写出
     * （与 MVC 侧 WebUtils.renderResponse 同模式；状态码经 attribute 传递以支持兜底分支的 502）
     */
    public static Mono<Void> renderResponse(ResponseModel<?> model, HttpStatus status, ServerWebExchange exchange) {
        ServerWebExchange mutated = exchange.mutate()
                .request(request -> request.path(FENCE_PATH))
                .build();
        mutated.getAttributes().put(FENCE_MODEL_ATTR, model);
        mutated.getAttributes().put(FENCE_STATUS_ATTR, status);
        return SpringUtils.getBean(DispatcherHandler.class).handle(mutated);
    }
}
