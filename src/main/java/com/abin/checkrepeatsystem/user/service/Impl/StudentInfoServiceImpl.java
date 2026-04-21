package com.abin.checkrepeatsystem.user.service.Impl;

import com.abin.checkrepeatsystem.pojo.entity.StudentInfo;
import com.abin.checkrepeatsystem.user.mapper.StudentInfoMapper;
import com.abin.checkrepeatsystem.user.service.StudentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 学生信息服务实现类
 */
@Service
public class StudentInfoServiceImpl extends ServiceImpl<StudentInfoMapper, StudentInfo> implements StudentInfoService {

    @Override
    public StudentInfo getByUserId(Long userId) {
        LambdaQueryWrapper<StudentInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StudentInfo::getUserId, userId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean saveOrUpdateByUserId(StudentInfo studentInfo) {
        LambdaQueryWrapper<StudentInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StudentInfo::getUserId, studentInfo.getUserId());
        StudentInfo existing = baseMapper.selectOne(queryWrapper);
        if (existing != null) {
            studentInfo.setId(existing.getId());
        }
        return saveOrUpdate(studentInfo);
    }
}
