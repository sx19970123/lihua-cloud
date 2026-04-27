package com.lihua.client.facade;

import com.lihua.client.client.SysUserAuthClient;
import com.lihua.client.model.RegisterUserModel;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.model.response.response.ApiResponse;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class SysUserAuthClientFacade {

    @Resource
    private SysUserAuthClient sysUserAuthClient;

    /**
     * 登录
     */
    @CircuitBreaker(name = "sysUser", fallbackMethod = "loginFallback")
    public Mono<ApiResponseModel<CurrentUser>> loginSelect(String username) {
        return sysUserAuthClient.loginSelect(username);
    }

    /**
     * 获取登录用户全部信息
     */
    @CircuitBreaker(name = "sysUser", fallbackMethod = "queryLoginUserProfileFallback")
    public ApiResponseModel<LoginUserSession> queryLoginUserProfile(LoginUserSession loginUserSession) {
        // return sysUserAuthClient.queryLoginUserProfile(loginUserSession);
        return null;
    }

    /**
     * 用户注册
     */
    @CircuitBreaker(name = "sysUser", fallbackMethod = "registerFallback")
    public ApiResponseModel<String> register(RegisterUserModel registerUserModel) {
        // return sysUserAuthClient.register(registerUserModel);
        return null;
    }

    public Mono<ApiResponseModel<CurrentUser>> loginFallback(String username, Throwable throwable) {
        log.error("远程调用异常, 请求参数{}", username, throwable);
        return Mono.create(sink -> ApiResponse.error(ResultCodeEnum.SERVER_BAD_ERROR));
    }

    public ApiResponseModel<LoginUserSession> queryLoginUserProfileFallback(LoginUserSession loginUserSession, Throwable throwable) {
        log.error("远程调用异常, 请求参数{}", loginUserSession, throwable);
        return ApiResponse.error(ResultCodeEnum.SERVER_BAD_ERROR);
    }

    public ApiResponseModel<CurrentUser> registerFallback(RegisterUserModel registerUserModel, Throwable throwable) {
        log.error("远程调用异常, 请求参数{}", registerUserModel, throwable);
        return ApiResponse.error(ResultCodeEnum.SERVER_BAD_ERROR);
    }
}
