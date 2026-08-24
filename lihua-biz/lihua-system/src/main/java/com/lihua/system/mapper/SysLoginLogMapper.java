package com.lihua.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihua.system.entity.SysLoginLog;

public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
    // 清空登录日志数据
    void clear();
}
