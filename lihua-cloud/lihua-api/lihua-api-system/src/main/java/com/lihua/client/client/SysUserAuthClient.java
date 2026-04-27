package com.lihua.client.client;

import com.lihua.client.annotation.HttpClient;
import com.lihua.client.enums.ExecutionModeEnum;
import com.lihua.client.model.RegisterUserModel;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@HttpClient(value = "lihua-system", executionMode = ExecutionModeEnum.ASYNC)
@HttpExchange("system/user/auth")
public interface SysUserAuthClient {

    /**
     * 查询登录的用户信息
     */
    @GetExchange("loginSelect/{username}")
    Mono<ApiResponseModel<CurrentUser>> loginSelect(@PathVariable("username") String username);

    /**
     * 查询登录用户信息
     */
//    @GetExchange("queryLoginUserProfile")
//    ApiResponseModel<LoginUserSession> queryLoginUserProfile(@RequestBody LoginUserSession loginUserSession);

    /**
     * 用户注册
     */
//    @PostExchange("register")
//    ApiResponseModel<String> register(@RequestBody RegisterUserModel registerUserModel);
}
