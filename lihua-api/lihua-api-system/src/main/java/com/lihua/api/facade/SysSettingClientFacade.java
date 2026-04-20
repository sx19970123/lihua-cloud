package com.lihua.api.facade;

import com.lihua.api.client.SysSettingClient;
import com.lihua.common.model.response.ApiResponseModel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

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

}
