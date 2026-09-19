package com.lihua.auth.service.impl;

import com.lihua.client.facade.SysUserAuthClientFacade;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.utils.date.DateUtils;
import com.lihua.security.config.TokenProperties;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
            // 须留日志：此处 msg 经 UsernameNotFoundException 抛出后会被 DaoAuthenticationProvider
            // 防枚举掩盖为凭据失败（"用户名或密码错误"），真实原因（如内部RPC签名拒绝）不再有任何痕迹
            log.warn("登录用户加载失败（auth→system RPC 返回非200）：username={}, code={}, msg={}",
                    username, responseModel.getCode(), responseModel.getMsg());
            throw new UsernameNotFoundException(responseModel.getMsg());
        }

        CurrentUser data = responseModel.getData();
        if (data == null) {
            throw new UsernameNotFoundException("用户名未找到");
        }

        // 创建 LoginUserSession 包含登录的用户信息 和 过期时间
        return new LoginUserSession(data, DateUtils.now().plus(tokenProperties.getTokenExpireTime()));
    }
}

