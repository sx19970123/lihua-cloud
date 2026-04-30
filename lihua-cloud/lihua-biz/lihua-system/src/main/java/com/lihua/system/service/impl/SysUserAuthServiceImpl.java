package com.lihua.system.service.impl;

import com.lihua.api.model.RegisterUserModel;
import com.lihua.common.exception.ServiceException;
import com.lihua.system.entity.SysUser;
import com.lihua.system.mapper.SysRoleMapper;
import com.lihua.system.mapper.SysUserMapper;
import com.lihua.system.model.dto.SysSettingDTO;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import com.lihua.system.service.SysSettingService;
import com.lihua.system.service.SysUserAuthService;
import com.lihua.system.service.SysUserService;
import com.lihua.system.strategy.cacheloginuser.CacheLoginUserStrategy;
import com.lihua.system.strategy.saveuserregister.SaveRegisterUserAssociatedStrategy;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysUserAuthServiceImpl implements SysUserAuthService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysSettingService sysSettingService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private List<CacheLoginUserStrategy> cacheLoginUserStrategyList;

    @Resource
    private List<SaveRegisterUserAssociatedStrategy> saveRegisterUserAssociatedStrategieList;


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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterUserModel registerUserModel) {

        SysSettingDTO.SignInSetting signInSetting = sysSettingService.getSignInSetting();

        if (signInSetting == null || !signInSetting.isEnable()) {
            throw new ServiceException("用户注册未开放");
        }

        // 校验用户名
        boolean checked = sysUserService.checkUserName(registerUserModel.getUsername());
        if (!checked) {
            throw new ServiceException("该用户名已存在");
        }

        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(registerUserModel, sysUser);

        // 保存用户基本信息
        sysUserMapper.insert(sysUser);

        // 通过用户注册配置类保存相关关联表数据
        saveRegisterUserAssociatedStrategieList.forEach(strategy -> strategy.saveRegisterUserAssociated(sysUser.getId(), signInSetting));

        return sysUser.getId();
    }

}
