package com.lihua.gateway.utils;

import com.lihua.common.model.response.ResponseModel;
import com.lihua.common.utils.json.JsonUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class WebUtils {

    /**
     * 将响应模型以 json 写入响应
     */
    public static Mono<Void> renderJson(ResponseModel<?> model, ServerHttpResponse response) {
        return renderJson(HttpStatus.OK, model, response);
    }

    /**
     * 将响应模型以 json 写入响应
     */
    public static Mono<Void> renderJson(HttpStatus httpStatus, ResponseModel<?> model, ServerHttpResponse response) {
        if (response == null) return Mono.empty();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory()
                .wrap(JsonUtils.toJsonIgnoreNulls(model).getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
