package com.lihua.gateway.handle;

import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ResponseModel;
import com.lihua.common.model.response.Responses;
import com.lihua.gateway.utils.WebUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * 围栏层响应端点：GatewayExceptionHandler 经 WebUtils.renderResponse 将错误模型 dispatch 转入此处，
 * 由消息转换管线按 Accept 协商写出（与 MVC 侧 FenceResponseController 同模式；
 * fence 路径不在网关路由表，GlobalFilter 链不执行，无重入拦截）
 */
@RestController
public class FenceResponseController {

    @RequestMapping(WebUtils.FENCE_PATH)
    public ResponseEntity<ResponseModel<?>> response(ServerWebExchange exchange) {
        ResponseModel<?> model = exchange.getAttribute(WebUtils.FENCE_MODEL_ATTR);
        // 非围栏转入的直接访问，按全局 404 同款响应
        if (model == null) {
            return ResponseEntity.ok(Responses.error(ResultCodeEnum.RESOURCE_NOT_FOUND_ERROR));
        }
        HttpStatus status = exchange.getAttribute(WebUtils.FENCE_STATUS_ATTR);
        return ResponseEntity.status(status != null ? status : HttpStatus.OK).body(model);
    }
}
