package com.lihua.client.facade;

import com.lihua.client.client.SysLogClient;
import com.lihua.client.model.LogModel;
import com.lihua.common.model.response.ApiResponseModel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 系统日志相关远程调用
 */
@Component
public class SysLogClientFacade {

    @Resource
    private SysLogClient sysLogClient;

    /**
     * 保存操作日志
     */
    @CircuitBreaker(name = "sysLog")
    public ApiResponseModel<String> insertOperate(LogModel logModel) {
        return sysLogClient.insertOperate(logModel);
    }

    /**
     * 保存登录日志
     */
    @CircuitBreaker(name = "sysLog")
    public ApiResponseModel<String> insertLogin(LogModel logModel) {
        return sysLogClient.insertLogin(logModel);
    }

}
