package com.lihua.api.client;

import com.lihua.api.annotation.RemoteClient;
import com.lihua.api.model.RegisterUserModel;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@RemoteClient(serverName = "lihua-system")
@HttpExchange("system/user/auth")
public interface SysUserAuthClient {

    /**
     * 查询登录的用户信息
     */
    @GetExchange("loginSelect/{username}")
    ApiResponseModel<CurrentUser> loginSelect(@PathVariable("username") String username);

    /**
     * 查询登录用户信息
     */
    @GetExchange("queryLoginUserProfile")
    ApiResponseModel<LoginUserSession> queryLoginUserProfile(@RequestBody LoginUserSession loginUserSession);

    /**
     * 用户注册
     */
    @PostExchange("register")
    ApiResponseModel<String> register(@RequestBody RegisterUserModel registerUserModel);
}
