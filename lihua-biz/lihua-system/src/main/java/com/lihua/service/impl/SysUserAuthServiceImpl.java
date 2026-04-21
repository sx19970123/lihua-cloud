package com.lihua.service.impl;

import com.lihua.mapper.SysRoleMapper;
import com.lihua.mapper.SysUserMapper;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import com.lihua.service.SysUserAuthService;
import com.lihua.strategy.cacheloginuser.CacheLoginUserStrategy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SysUserAuthServiceImpl implements SysUserAuthService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private List<CacheLoginUserStrategy> cacheLoginUserStrategyList;

    @Override
    public CurrentUser loginSelect(String username) {
        return sysUserMapper.loginSelect(username);
    }

    @Override
    public LoginUserSession queryLoginUserProfile(LoginUserSession loginUserSession) {
        List<String> roleCodes = sysRoleMapper.selectCodeByUserId(loginUserSession.getUser().getId());
        boolean isAdmin = roleCodes.contains("ROLE_admin");
        // 执行各个模块的缓存设置
        cacheLoginUserStrategyList.forEach(strategy -> strategy.cacheLoginUser(loginUserSession, isAdmin));
        return loginUserSession;
    }

}
