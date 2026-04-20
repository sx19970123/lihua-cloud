package com.lihua.service;

import com.lihua.security.model.CurrentUser;

public interface SysUserAuthService {

    /**
     * 根据username查询用户信息
     * 用于登录接口
     */
    CurrentUser queryUserByUsername(String username);

}
