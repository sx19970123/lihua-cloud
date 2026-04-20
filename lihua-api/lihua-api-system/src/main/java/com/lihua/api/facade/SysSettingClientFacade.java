package com.lihua.api.facade;

import com.lihua.api.client.SysSettingClient;
import com.lihua.common.model.response.ApiResponseModel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 系统设置相关远程调用
 */
@Component
public class SysSettingClientFacade {

    @Resource
    SysSettingClient sysSettingClient;

    /**
     * 缓存ip黑名单
     */
    @CircuitBreaker(name = "sysSetting")
    public ApiResponseModel<String> cacheIpBlack() {
        return sysSettingClient.cacheIpBlack();
    }

    /**
     * 是否启用验证码
     */
    @CircuitBreaker(name = "sysSetting")
    public ApiResponseModel<Boolean> enableCaptcha() {
        return sysSettingClient.enableCaptcha();
    }

}
