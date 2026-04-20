package com.lihua.api.facade;

import com.lihua.api.client.SysLogClient;
import com.lihua.api.model.LogModel;
import com.lihua.common.model.response.ApiResponseModel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

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
    public ApiResponseModel<String> insertOperate(@RequestBody LogModel logModel) {
        return sysLogClient.insertOperate(logModel);
    }

    /**
     * 保存登录日志
     */
    @CircuitBreaker(name = "sysLog")
    public ApiResponseModel<String> insertLogin(@RequestBody LogModel logModel) {
        return sysLogClient.insertLogin(logModel);
    }

}
