package com.lihua.client.facade;

import com.lihua.client.client.SysUserClient;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.model.response.response.ApiResponse;
import com.lihua.security.model.CurrentUser;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SysUserClientFacade {

    @Resource
    private SysUserClient sysUserClient;

    /**
     * 根据用户名获取用户信息
     */
    @CircuitBreaker(name = "sysUser", fallbackMethod = "loginFallback")
    public ApiResponseModel<CurrentUser> loadUserByUsername(String username) {
        return sysUserClient.loadUserByUsername(username);
    }

    public ApiResponseModel<CurrentUser> loginFallback(String username, Throwable throwable) {
        log.error("远程调用异常", throwable);
        return ApiResponse.error(ResultCodeEnum.SERVER_BAD_ERROR);
    }
}
