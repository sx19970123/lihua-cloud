package com.lihua.client.facade;

import com.lihua.client.client.SysUserClient;
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

    /**
     * 获取登录用户全部信息
     */
    @CircuitBreaker(name = "sysUser", fallbackMethod = "loginFallback")
    public ApiResponseModel<LoginUserSession> queryLoginUserSession(@RequestBody LoginUserSession loginUserSession) {
        return sysUserClient.queryLoginUserSession(loginUserSession);
    }


    public ApiResponseModel<CurrentUser> loginFallback(String username, Throwable throwable) {
        log.error("远程调用异常", throwable);
        return ApiResponse.error(ResultCodeEnum.SERVER_BAD_ERROR);
    }
}
