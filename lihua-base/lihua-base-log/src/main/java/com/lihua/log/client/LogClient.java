package com.lihua.log.client;

import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.log.model.LogModel;
import reactor.core.publisher.Mono;

/**
 * 日志落库通道：接口属于日志域（消费者），远程调用实现由 api 契约层提供
 */
public interface LogClient {

    /**
     * 保存操作日志
     */
    Mono<ApiResponseModel<String>> insertOperate(LogModel logModel);

    /**
     * 保存登录日志
     */
    Mono<ApiResponseModel<String>> insertLogin(LogModel logModel);
}
