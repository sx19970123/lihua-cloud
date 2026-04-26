package com.lihua.auth.service.impl;

import com.lihua.client.facade.SysSettingClientFacade;
import com.lihua.client.facade.SysUserAuthClientFacade;
import com.lihua.client.model.RegisterUserModel;
import com.lihua.common.exception.ServiceException;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.utils.date.DateUtils;
import com.lihua.cache.manager.RedisCacheManager;
import com.lihua.cache.enums.RedisKeyPrefixEnum;
import com.lihua.auth.model.dto.SysLoginUserDTO;
import com.lihua.security.manager.LoginUserContext;
import com.lihua.security.manager.LoginUserManager;
import com.lihua.security.model.LoginUserSession;
import com.lihua.security.utils.JwtUtils;
import com.lihua.security.utils.SecurityUtils;
import com.lihua.auth.service.SysAuthenticationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class SysAuthenticationServiceImpl implements SysAuthenticationService {

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private SysUserAuthClientFacade sysUserAuthClientFacade;

    @Resource
    private SysSettingClientFacade sysSettingClientFacade;

    @Resource
    private RedisCacheManager redisCacheManager;


    @Override
    public LoginUserSession login(SysLoginUserDTO loginUserDTO) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserDTO.getUsername(), loginUserDTO.getPassword()));
        return (LoginUserSession) authenticate.getPrincipal();
    }

    @Override
    public String cacheLoginUserInfo(LoginUserSession loginUserSession) {
        // 远程调用获取用户信息
        ApiResponseModel<LoginUserSession> responseModel = sysUserAuthClientFacade.queryLoginUserProfile(loginUserSession);
        if (200 != responseModel.getCode()) {
            throw new ServiceException(responseModel.getMsg());
        }

        loginUserSession = responseModel.getData();
        if (loginUserSession == null) {
            throw new ServiceException("获取信息为空");
        }

        // 设置redis缓存
        return LoginUserManager.setLoginUserCache(loginUserSession);
    }

    @Override
    public String cacheAndCreateToken(LoginUserSession loginUserSession) {
        String redisKey = cacheLoginUserInfo(loginUserSession);
        return JwtUtils.create(redisKey);
    }

    @Override
    public ApiResponseModel<String> register(String username, String password) {
        RegisterUserModel registerUserModel = new RegisterUserModel();
        registerUserModel
                .setUsername(username)
                .setPassword(SecurityUtils.encryptPassword(password))
                .setStatus("0")
                .setRegisterType("1")
                .setPasswordUpdateTime(DateUtils.now());
        // 远程调用注册
        return sysUserAuthClientFacade.register(registerUserModel);
    }

    @Override
    public void checkSameAccount(String token) {
        // 获取最大登录用户配置信息，-1为未配置
        ApiResponseModel<Integer> responseModel = sysSettingClientFacade.getMaxConcurrentLogins();
        if (200 != responseModel.getCode()) {
            throw new ServiceException(responseModel.getMsg());
        }

        Integer limitSize = responseModel.getData();

        if (limitSize == null || limitSize == -1) {
            return;
        }

        // 获取用户id
        String userId = LoginUserManager.getUserIdByCacheKey(JwtUtils.decode(token));

        if (!StringUtils.hasText(userId)) {
            throw new ServiceException("用户id不存在");
        }

        // 获取所有用户登录 key
        Set<String> keys = redisCacheManager.keys(RedisKeyPrefixEnum.LOGIN_USER_REDIS_PREFIX.getValue() + userId);

        int count = keys.size() - limitSize;
        if (count < 0) {
            return;
        }

        // 根据用户登录时间，先登录的被踢下线
        keys.stream()
            .sorted(Comparator.comparingLong(LoginUserManager::getLoginTimestampByCacheKey))
            .limit(count)
            .forEach(key -> redisCacheManager.delete(key));
    }

    @Override
    public String getOnceToken() {
        String uuid = UUID.randomUUID().toString();
        redisCacheManager.setCacheObject(RedisKeyPrefixEnum.ONCE_TOKEN_REDIS_PREFIX.getValue() + uuid, LoginUserContext.getUserId(), Duration.ofMinutes(1));
        return uuid;
    }
}
