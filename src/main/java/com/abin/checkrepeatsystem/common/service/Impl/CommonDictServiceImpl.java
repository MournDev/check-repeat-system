package com.abin.checkrepeatsystem.common.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.mapper.CollegeMapper;
import com.abin.checkrepeatsystem.common.service.CommonDictService;
import com.abin.checkrepeatsystem.pojo.entity.College;
import com.abin.checkrepeatsystem.pojo.entity.Major;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.student.mapper.MajorMapper;
import com.abin.checkrepeatsystem.user.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 公共字典服务实现类
 */
@Slf4j
@Service
public class CommonDictServiceImpl implements CommonDictService {

    private static final String CACHE_KEY_COLLEGES = "common:dict:colleges";
    private static final String CACHE_KEY_MAJORS = "common:dict:majors";
    private static final String CACHE_KEY_MAJOR_MAP = "common:dict:major:map";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Autowired
    private CollegeMapper collegeMapper;

    @Autowired
    private MajorMapper majorMapper;

    @Autowired
    private SysUserService sysUserService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result<List<Map<String, Object>>> getColleges() {
        try {
            if (redisTemplate != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(CACHE_KEY_COLLEGES);
                    if (cached != null) {
                        log.debug("从缓存获取学院列表: count={}", cached.size());
                        return Result.success("学院列表获取成功", cached);
                    }
                } catch (Exception e) {
                    log.warn("读取学院缓存失败，使用数据库查询", e);
                }
            }

            LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(College::getIsDeleted, 0)
                   .orderByAsc(College::getCollegeName);

            List<College> colleges = collegeMapper.selectList(wrapper);

            List<Map<String, Object>> result = colleges.stream()
                    .map(college -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("value", college.getId());
                        map.put("label", college.getCollegeName());
                        map.put("code", college.getCollegeCode());
                        return map;
                    })
                    .collect(Collectors.toList());

            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(CACHE_KEY_COLLEGES, result, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("写入学院缓存失败", e);
                }
            }

            log.debug("获取学院列表成功: count={}", result.size());
            return Result.success("学院列表获取成功", result);
        } catch (Exception e) {
            log.error("获取学院列表失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取学院列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getMajors(Long collegeId) {
        try {
            String cacheKey = collegeId != null && collegeId > 0
                    ? CACHE_KEY_MAJORS + ":" + collegeId
                    : CACHE_KEY_MAJORS;

            if (redisTemplate != null) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null) {
                        log.debug("从缓存获取专业列表: cacheKey={}, count={}", cacheKey, cached.size());
                        return Result.success("专业列表获取成功", cached);
                    }
                } catch (Exception e) {
                    log.warn("读取专业缓存失败，使用数据库查询", e);
                }
            }

            LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Major::getIsDeleted, 0);

            if (collegeId != null && collegeId > 0) {
                wrapper.eq(Major::getCollegeId, collegeId);
            }

            wrapper.orderByAsc(Major::getMajorName);

            List<Major> majors = majorMapper.selectList(wrapper);

            List<Map<String, Object>> result = majors.stream()
                    .map(major -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("value", major.getId());
                        map.put("label", major.getMajorName());
                        map.put("code", major.getMajorCode());
                        map.put("collegeId", major.getCollegeId());
                        return map;
                    })
                    .collect(Collectors.toList());

            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("写入专业缓存失败", e);
                }
            }

            log.debug("获取专业列表成功: count={}, collegeId={}", result.size(), collegeId);
            return Result.success("专业列表获取成功", result);
        } catch (Exception e) {
            log.error("获取专业列表失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取专业列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getGrades() {
        try {
            // 从用户表中提取所有年级信息（年级是动态数据，从用户表聚合合理）
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
        } catch (Exception e) {
            log.error("获取年级列表失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取年级列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, String>> getMajorNameMap() {
        try {
            if (redisTemplate != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> cached = (Map<String, String>) redisTemplate.opsForValue().get(CACHE_KEY_MAJOR_MAP);
                    if (cached != null) {
                        log.debug("从缓存获取专业名称映射: count={}", cached.size());
                        return Result.success("专业名称映射获取成功", cached);
                    }
                } catch (Exception e) {
                    log.warn("读取专业名称映射缓存失败", e);
                }
            }

            LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Major::getIsDeleted, 0);

            List<Major> majors = majorMapper.selectList(wrapper);

            Map<String, String> majorMap = majors.stream()
                    .collect(Collectors.toMap(
                            major -> String.valueOf(major.getId()),
                            Major::getMajorName,
                            (existing, replacement) -> existing
                    ));

            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(CACHE_KEY_MAJOR_MAP, majorMap, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("写入专业名称映射缓存失败", e);
                }
            }

            log.debug("获取专业名称映射成功: count={}", majorMap.size());
            return Result.success("专业名称映射获取成功", majorMap);
        } catch (Exception e) {
            log.error("获取专业名称映射失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取专业名称映射失败: " + e.getMessage());
        }
    }
}
