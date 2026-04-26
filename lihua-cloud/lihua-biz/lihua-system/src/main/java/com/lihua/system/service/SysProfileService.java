package com.lihua.system.service;

import com.lihua.system.entity.SysUser;
import com.lihua.system.model.dto.SysCheckPasswordDTO;

import java.util.List;

public interface SysProfileService {

    /**
     * 保存主题
     */
    String saveTheme(String theme);

    /**
     * 保存基础信息
     */
    String saveBasics(SysUser sysUser);

    /**
     * 修改密码
     */
    String updatePassword(String newPassword);

    /**
     * 获取用户密码
     */
    String getPassword();

    /**
     * 用户注销
     */
    void accountDeactivate();

    /**
     * 校验密码
     */
    Boolean checkPassword(SysCheckPasswordDTO sysCheckPasswordDTO);

    /**
     * 登录后用户数据校验
     * 返回用于完善信息的前端组件名称
     */
    List<String> postLoginCheck();
}
