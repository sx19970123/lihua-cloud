package com.lihua.client.facade;

import com.lihua.client.client.SysLogClient;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.log.client.LogClient;
import com.lihua.log.model.LogModel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 系统日志相关远程调用
 */
@Component
public class SysLogClientFacade implements LogClient {

    @Resource
    private SysLogClient sysLogClient;

    /**
     * 保存操作日志
     */
    @Override
    @CircuitBreaker(name = "sysLog", fallbackMethod = "logFallback")
    public Mono<ApiResponseModel<String>> insertOperate(LogModel logModel) {
        return sysLogClient.insertOperate(logModel);
    }

    /**
     * 保存登录日志
     */
    @Override
    @CircuitBreaker(name = "sysLog", fallbackMethod = "logFallback")
    public Mono<ApiResponseModel<String>> insertLogin(LogModel logModel) {
        return sysLogClient.insertLogin(logModel);
    }


    /**
     * 日志落库为 fire-and-forget 链路：失败留痕统一由消费端 subscribe 的 onError 回调负责，
     * 此处仅透传异常，勿在此补日志（会与消费端重复）
     */
    public Mono<ApiResponseModel<String>> logFallback(LogModel logModel, Throwable throwable) {
        return Mono.error(throwable);
    }
}
