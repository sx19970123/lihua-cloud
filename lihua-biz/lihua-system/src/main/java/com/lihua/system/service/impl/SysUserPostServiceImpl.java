package com.lihua.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lihua.system.entity.SysUserPost;
import com.lihua.system.mapper.SysUserPostMapper;
import com.lihua.system.model.vo.SysPostVO;
import com.lihua.system.service.SysUserPostService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class SysUserPostServiceImpl extends ServiceImpl<SysUserPostMapper, SysUserPost> implements SysUserPostService {

    @Resource
    private SysUserPostMapper sysUserPostMapper;

    @Override
    public void save(List<SysUserPost> sysUserPosts) {
        saveBatch(sysUserPosts);
    }

    @Override
    public void deleteByUserIds(List<String> userIds) {
        QueryWrapper<SysUserPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(SysUserPost::getUserId, userIds);
        remove(queryWrapper);
    }

    @Override
    public List<SysPostVO> queryPostByUserIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        return sysUserPostMapper.queryPostByUserIds(userIds);
    }
}
