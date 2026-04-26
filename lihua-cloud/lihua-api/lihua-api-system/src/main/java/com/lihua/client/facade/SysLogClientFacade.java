package com.lihua.client.facade;

import com.lihua.client.client.SysLogClient;
import com.lihua.client.model.LogModel;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.model.response.response.ApiResponse;
import com.lihua.security.model.CurrentUser;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 系统日志相关远程调用
 */
@Component
@Slf4j
public class SysLogClientFacade {

    @Resource
    private SysLogClient sysLogClient;

    /**
     * 保存操作日志
     */
    @CircuitBreaker(name = "sysLog", fallbackMethod = "logFallback")
    public ApiResponseModel<String> insertOperate(LogModel logModel) {
        return sysLogClient.insertOperate(logModel);
    }

    /**
     * 保存登录日志
     */
    @CircuitBreaker(name = "sysLog", fallbackMethod = "logFallback")
    public ApiResponseModel<String> insertLogin(LogModel logModel) {
        return sysLogClient.insertLogin(logModel);
    }


    public ApiResponseModel<CurrentUser> logFallback(LogModel logModel, Throwable throwable) {
        log.error("远程调用异常, 请求参数{}", logModel, throwable);
        return ApiResponse.error(ResultCodeEnum.SERVER_BAD_ERROR);
    }
}