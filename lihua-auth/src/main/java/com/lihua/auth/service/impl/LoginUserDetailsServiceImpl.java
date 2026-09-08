package com.lihua.auth.service.impl;

import com.lihua.client.facade.SysUserAuthClientFacade;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.utils.date.DateUtils;
import com.lihua.security.config.TokenProperties;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import jakarta.annotation.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LoginUserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private SysUserAuthClientFacade sysUserAuthClientFacade;

    @Resource
    private TokenProperties tokenProperties;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {

        // 远程调用获取用户信息
        ApiResponseModel<CurrentUser> responseModel = sysUserAuthClientFacade.loginSelect(username);

        // 枚举常量为 Integer，须 equals 值比较（== 为引用比较，200 超出 Integer 缓存必不等）
        if (!ResultCodeEnum.SUCCESS.getCode().equals(responseModel.getCode())) {
            throw new UsernameNotFoundException(responseModel.getMsg());
        }

        CurrentUser data = responseModel.getData();
        if (data == null) {
            throw new UsernameNotFoundException("用户名未找到");
        }

        // 创建 LoginUserSession 包含登录的用户信息 和 过期时间
        return new LoginUserSession(data, DateUtils.now().plusMinutes(tokenProperties.getTokenExpireTime()));
    }
}

