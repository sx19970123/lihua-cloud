package com.lihua.auth.service.impl;

import com.lihua.client.facade.SysUserAuthClientFacade;
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
import reactor.core.publisher.Mono;

@Service
public class LoginUserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private SysUserAuthClientFacade sysUserAuthClientFacade;

    @Resource
    private TokenProperties tokenProperties;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {

        // 远程调用获取用户信息
        Mono<ApiResponseModel<CurrentUser>> mono = sysUserAuthClientFacade.loginSelect(username);

        ApiResponseModel<CurrentUser> responseModel = mono.map(data -> {
            return data;
        });

        if (200 != responseModel.getCode()) {
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

