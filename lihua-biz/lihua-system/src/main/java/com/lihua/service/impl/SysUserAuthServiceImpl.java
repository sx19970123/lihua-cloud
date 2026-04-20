package com.lihua.service.impl;

import com.lihua.mapper.SysUserMapper;
import com.lihua.security.model.CurrentUser;
import com.lihua.service.SysUserAuthService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SysUserAuthServiceImpl implements SysUserAuthService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    public CurrentUser queryUserByUsername(String username) {
        return sysUserMapper.loginSelect(username);
    }

}
