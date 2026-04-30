package com.lihua.api.facade;

import com.lihua.api.client.SysSettingClient;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.model.response.response.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 系统设置相关远程调用
 */
@Component
@Slf4j
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
     * 查询同账号最大登录数
     */
    @CircuitBreaker(name = "sysSetting")
    public ApiResponseModel<Integer> getMaxConcurrentLogins() {
        return sysSettingClient.getMaxConcurrentLogins();
    }

    /**
     * 是否启用验证码
     */
    @CircuitBreaker(name = "sysSetting", fallbackMethod = "captchaFallback")
    public ApiResponseModel<Boolean> enableCaptcha() {
        return sysSettingClient.enableCaptcha();
    }

    public ApiResponseModel<Boolean> captchaFallback(Throwable throwable) {
        log.error("远程调用异常", throwable);
        return ApiResponse.error(ResultCodeEnum.SERVER_BAD_ERROR);
    }

}
