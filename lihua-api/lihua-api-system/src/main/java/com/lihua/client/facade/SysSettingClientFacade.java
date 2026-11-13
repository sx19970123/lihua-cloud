package com.lihua.client.facade;

import com.lihua.client.client.SysSettingClient;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ResponseModel;
import com.lihua.common.model.response.Responses;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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
    @CircuitBreaker(name = "sysSetting", fallbackMethod = "cacheIpBlackFallback")
    public ResponseModel<String> cacheIpBlack() {
        return sysSettingClient.cacheIpBlack();
    }

    public ResponseModel<String> cacheIpBlackFallback(Throwable throwable) {
        log.error("远程调用异常", throwable);
        return Responses.error(ResultCodeEnum.SERVER_BAD_ERROR);
    }

    /**
     * 查询同账号最大登录数
     */
    @CircuitBreaker(name = "sysSetting")
    public ResponseModel<Integer> getMaxConcurrentLogins() {
        return sysSettingClient.getMaxConcurrentLogins();
    }

    /**
     * 是否启用验证码
     */
    @CircuitBreaker(name = "sysSetting", fallbackMethod = "captchaFallback")
    public ResponseModel<Boolean> enableCaptcha() {
        return sysSettingClient.enableCaptcha();
    }

    public ResponseModel<Boolean> captchaFallback(Throwable throwable) {
        log.error("远程调用异常", throwable);
        return Responses.error(ResultCodeEnum.SERVER_BAD_ERROR);
    }

    /**
     * 获取当前请求端验证码随机类型池
     */
    @CircuitBreaker(name = "sysSetting", fallbackMethod = "captchaTypesFallback")
    public ResponseModel<List<String>> getCaptchaTypes() {
        return sysSettingClient.getCaptchaTypes();
    }

    public ResponseModel<List<String>> captchaTypesFallback(Throwable throwable) {
        log.error("远程调用异常", throwable);
        return Responses.error(ResultCodeEnum.SERVER_BAD_ERROR);
    }

}
