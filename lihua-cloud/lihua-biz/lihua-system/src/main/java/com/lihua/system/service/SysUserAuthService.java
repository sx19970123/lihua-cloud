package com.lihua.system.service;

import com.lihua.api.model.RegisterUserModel;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;

public interface SysUserAuthService {

    /**
     * 根据username查询用户信息
     * 用于登录接口
     */
    CurrentUser loginSelect(String username);

    /**
     * 查询登录用户全部信息
     */
    LoginUserSession queryLoginUserProfile(LoginUserSession loginUserSession);

    /**
     * 用户注册
     */
    String register(RegisterUserModel registerUserModel);
}
