package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.service.AdminDictService;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.user.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员公共字典服务实现类
 */
@Slf4j
@Service
public class AdminDictServiceImpl implements AdminDictService {

    @Resource
    private SysUserService sysUserService;

    @Override
    public Result<List<Map<String, Object>>> getMajors() {
        // 从用户表中提取所有专业信息
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getMajor)
               .isNotNull(SysUser::getMajor)
               .ne(SysUser::getMajor, "")
               .eq(SysUser::getIsDeleted, 0)
               .groupBy(SysUser::getMajor);
        
        List<SysUser> users = sysUserService.list(wrapper);
        
        List<Map<String, Object>> majors = users.stream()
                .map(user -> {
                    Map<String, Object> major = new HashMap<>();
                    major.put("value", user.getMajor());
                    major.put("label", user.getMajor());
                    return major;
                })
                .distinct()
                .sorted(Comparator.comparing(m -> (String) m.get("label")))
                .collect(Collectors.toList());
        
        log.debug("获取专业列表成功: count={}", majors.size());
        return Result.success("专业列表获取成功", majors);
    }

    @Override
    public Result<List<Map<String, Object>>> getGrades() {
        // 从用户表中提取所有年级信息
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getGrade)
               .isNotNull(SysUser::getGrade)
               .ne(SysUser::getGrade, "")
               .eq(SysUser::getIsDeleted, 0)
               .groupBy(SysUser::getGrade);
        
        List<SysUser> users = sysUserService.list(wrapper);
        
        List<Map<String, Object>> grades = users.stream()
                .map(user -> {
                    Map<String, Object> grade = new HashMap<>();
                    grade.put("value", user.getGrade());
                    grade.put("label", user.getGrade() + "级");
                    return grade;
                })
                .distinct()
                .sorted((a, b) -> {
                    String gradeA = (String) a.get("value");
                    String gradeB = (String) b.get("value");
                    return gradeB.compareTo(gradeA); // 按年级倒序排列
                })
                .collect(Collectors.toList());
        
        log.debug("获取年级列表成功: count={}", grades.size());
        return Result.success("年级列表获取成功", grades);
    }

    @Override
    public Result<List<Map<String, Object>>> getColleges() {
        // 从用户表中提取所有学院信息
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getCollegeName)
               .isNotNull(SysUser::getCollegeName)
               .ne(SysUser::getCollegeName, "")
               .eq(SysUser::getIsDeleted, 0)
               .groupBy(SysUser::getCollegeName);
        
        List<SysUser> users = sysUserService.list(wrapper);
        
        List<Map<String, Object>> colleges = users.stream()
                .map(user -> {
                    Map<String, Object> college = new HashMap<>();
                    college.put("value", user.getCollegeName());
                    college.put("label", user.getCollegeName());
                    return college;
                })
                .distinct()
                .sorted(Comparator.comparing(m -> (String) m.get("label")))
                .collect(Collectors.toList());
        
        log.debug("获取学院列表成功: count={}", colleges.size());
        return Result.success("学院列表获取成功", colleges);
    }
    
    @Override
    public Result<Map<String, String>> getMajorNameMap() {
        try {
            // 从用户表中提取专业代码和名称的映射关系
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(SysUser::getMajor, SysUser::getMajor)
                   .isNotNull(SysUser::getMajor)
                   .ne(SysUser::getMajor, "")
                   .eq(SysUser::getIsDeleted, 0)
                   .groupBy(SysUser::getMajor);
            
            List<SysUser> users = sysUserService.list(wrapper);
            
            Map<String, String> majorMap = users.stream()
                .collect(Collectors.toMap(
                    SysUser::getMajor,  // key: 专业代码
                    SysUser::getMajor,  // value: 专业名称（这里简化处理）
                    (existing, replacement) -> existing  // 处理重复key
                ));
            
            log.debug("获取专业名称映射成功: count={}", majorMap.size());
            return Result.success("专业名称映射获取成功", majorMap);
        } catch (Exception e) {
            log.error("获取专业名称映射失败: {}", e.getMessage(), e);
            return Result.error(com.abin.checkrepeatsystem.common.enums.ResultCode.SYSTEM_ERROR, 
                              "获取专业名称映射失败: " + e.getMessage());
        }
    }
}