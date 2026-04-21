package com.abin.checkrepeatsystem.user.service.Impl;

import com.abin.checkrepeatsystem.pojo.entity.AdminInfo;
import com.abin.checkrepeatsystem.user.mapper.AdminInfoMapper;
import com.abin.checkrepeatsystem.user.service.AdminInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 管理员信息服务实现类
 */
@Service
public class AdminInfoServiceImpl extends ServiceImpl<AdminInfoMapper, AdminInfo> implements AdminInfoService {

    @Override
    public AdminInfo getByUserId(Long userId) {
        LambdaQueryWrapper<AdminInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AdminInfo::getUserId, userId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean saveOrUpdateByUserId(AdminInfo adminInfo) {
        LambdaQueryWrapper<AdminInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AdminInfo::getUserId, adminInfo.getUserId());
        AdminInfo existing = baseMapper.selectOne(queryWrapper);
        if (existing != null) {
            adminInfo.setId(existing.getId());
        }
        return saveOrUpdate(adminInfo);
    }
}
