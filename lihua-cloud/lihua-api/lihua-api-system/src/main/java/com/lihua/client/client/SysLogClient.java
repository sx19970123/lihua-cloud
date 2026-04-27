package com.lihua.client.client;

import com.lihua.client.annotation.RemoteClient;
import com.lihua.client.enums.ExecutionModeEnum;
import com.lihua.client.model.LogModel;
import com.lihua.common.model.response.ApiResponseModel;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

@RemoteClient(serverName = "lihua-system", executionMode = ExecutionModeEnum.ASYNC)
@HttpExchange("system/log")
public interface SysLogClient {

    /**
     * 保存操作日志
     */
    @PostExchange("operate/insert")
    Mono<ApiResponseModel<String>> insertOperate(@RequestBody LogModel logModel);

    /**
     * 保存登录日志
     */
    @PostExchange("login/insert")
    Mono< ApiResponseModel<String>> insertLogin(@RequestBody LogModel logModel);
}
