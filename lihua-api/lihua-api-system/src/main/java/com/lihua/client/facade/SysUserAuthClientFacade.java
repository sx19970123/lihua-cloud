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
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SysUserAuthClientFacade {

    @Resource
    private SysUserAuthClient sysUserAuthClient;

    /**
     * 登录
     */
    @CircuitBreaker(name = "sysUser", fallbackMethod = "loginFallback")
    public ApiResponseModel<CurrentUser> loginSelect(String username) {
        return sysUserAuthClient.loginSelect(username);
    }

    /**
     * 获取登录用户全部信息
     */
    @CircuitBreaker(name = "sysUser", fallbackMethod = "queryLoginUserProfileFallback")
    public ApiResponseModel<LoginUserSession> queryLoginUserProfile(LoginUserSession loginUserSession) {
        return sysUserAuthClient.queryLoginUserProfile(loginUserSession);
    }

    /**
     * 用户注册
     */
    @CircuitBreaker(name = "sysUser", fallbackMethod = "registerFallback")
    public ApiResponseModel<String> register(RegisterUserModel registerUserModel) {
        return sysUserAuthClient.register(registerUserModel);
    }

    /**
     * 进入本方法 = 网络失败或下游 500 兜底，均为系统级故障（业务异常经 GlobalExceptionHandle
     * 以 HTTP 200+code 返回，不进此通道；若将来业务异常 handler 改为非 2xx 形态，此处语义需重审）。
     * 消费方为 UserDetailsService 契约：系统级故障以异常形态透出——DaoAuthenticationProvider 将
     * 非 UsernameNotFoundException 异常归入系统通道透传；若返回 error 响应码，消费方只能抛
     * UsernameNotFoundException，经防枚举机制掩盖成 BadCredentials（503 伪装成 401 误导排障）。
     * 留痕在 GlobalExceptionHandle 的终结 handler（勿在此打日志，防双条）
     */
    public ApiResponseModel<CurrentUser> loginFallback(String username, Throwable throwable) {
        throw new InternalAuthenticationServiceException(ResultCodeEnum.SERVER_BAD_ERROR.getDefaultMsg(), throwable);
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
