package com.abin.checkrepeatsystem.user.service.Impl;

import com.abin.checkrepeatsystem.pojo.entity.TeacherInfo;
import com.abin.checkrepeatsystem.user.mapper.TeacherInfoMapper;
import com.abin.checkrepeatsystem.user.service.TeacherInfoDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 教师信息服务实现类
 */
@Service
public class TeacherInfoDataServiceImpl extends ServiceImpl<TeacherInfoMapper, TeacherInfo> implements TeacherInfoDataService {

    @Override
    public TeacherInfo getByUserId(Long userId) {
        LambdaQueryWrapper<TeacherInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeacherInfo::getUserId, userId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean saveOrUpdateByUserId(TeacherInfo teacherInfo) {
        LambdaQueryWrapper<TeacherInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeacherInfo::getUserId, teacherInfo.getUserId());
        TeacherInfo existing = baseMapper.selectOne(queryWrapper);
        if (existing != null) {
            teacherInfo.setId(existing.getId());
        }
        return saveOrUpdate(teacherInfo);
    }
}
