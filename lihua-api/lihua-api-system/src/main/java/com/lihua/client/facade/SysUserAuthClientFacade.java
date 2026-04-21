package com.lihua.client.facade;

import com.lihua.client.client.SysUserAuthClient;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.model.response.response.ApiResponse;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@Slf4j
public class SysUserAuthClientFacade {

    @Resource
    private SysUserAuthClient sysUserAuthClient;

    /**
     * 根据用户名获取用户信息
     */
    @CircuitBreaker(name = "sysUser", fallbackMethod = "loginFallback")
    public ApiResponseModel<CurrentUser> loginSelect(String username) {
        return sysUserAuthClient.loginSelect(username);
    }

    /**
     * 获取登录用户全部信息
     */
    @CircuitBreaker(name = "sysUser", fallbackMethod = "loginFallback")
    public ApiResponseModel<LoginUserSession> queryLoginUserProfile(@RequestBody LoginUserSession loginUserSession) {
        return sysUserAuthClient.queryLoginUserProfile(loginUserSession);
    }


    public ApiResponseModel<CurrentUser> loginFallback(String username, Throwable throwable) {
        log.error("远程调用异常", throwable);
        return ApiResponse.error(ResultCodeEnum.SERVER_BAD_ERROR);
    }
}
