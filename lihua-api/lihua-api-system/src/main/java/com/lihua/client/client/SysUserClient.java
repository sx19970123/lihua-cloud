package com.lihua.client.client;

import com.lihua.client.annotation.HttpClient;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpClient("lihua-system")
@HttpExchange("system/user")
public interface SysUserClient {

    /**
     * 根据用户名查询用户信息
     */
    @GetExchange("queryUserByUsername/{username}")
    ApiResponseModel<CurrentUser> loadUserByUsername(@PathVariable("username") String username);

    /**
     * 查询登录用户信息
     */
    @GetExchange("queryLoginUserSession")
    ApiResponseModel<LoginUserSession> queryLoginUserSession(@RequestBody LoginUserSession  loginUserSession);
}
