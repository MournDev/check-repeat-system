package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.service.AdminAssignmentService;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.admin.dto.AssignmentRuleConfigDTO;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.mapper.SysDictDataMapper;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysDictData;
import com.abin.checkrepeatsystem.pojo.entity.TeacherInfo;
import com.abin.checkrepeatsystem.pojo.entity.StudentInfo;
import com.abin.checkrepeatsystem.user.service.StudentInfoService;
import com.abin.checkrepeatsystem.user.service.TeacherInfoDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员论文分配服务实现类
 */
@Service
@Slf4j
public class AdminAssignmentServiceImpl implements AdminAssignmentService {

    @Resource
    private SysUserMapper sysUserMapper;
    
    @Resource
    private PaperInfoMapper paperInfoMapper;
    
    @Resource
    private SysDictDataMapper sysDictDataMapper;
    
    @Resource
    private TeacherInfoDataService teacherInfoService;
    
    @Resource
    private StudentInfoService studentInfoService;

    @Override
    public Result<Map<String, Object>> getAssignmentStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 未分配学生数 - 没有指导老师的论文
            LambdaQueryWrapper<PaperInfo> unassignedWrapper = new LambdaQueryWrapper<>();
            unassignedWrapper.isNull(PaperInfo::getTeacherId)
                           .eq(PaperInfo::getPaperStatus, "approved"); // 只统计已通过审核的论文
            int unassignedStudents = paperInfoMapper.selectCount(unassignedWrapper).intValue();
            stats.put("unassignedStudents", unassignedStudents);
            
            // 可用教师数 - 可以接受更多学生的教师
            LambdaQueryWrapper<SysUser> teacherWrapper = new LambdaQueryWrapper<>();
            teacherWrapper.eq(SysUser::getUserType, "teacher")
                         .eq(SysUser::getStatus, 1); // 启用状态
            int availableTeachers = sysUserMapper.selectCount(teacherWrapper).intValue();
            stats.put("availableTeachers", availableTeachers);
            
            // 平均指导数
            List<SysUser> teachers = sysUserMapper.selectList(teacherWrapper);
            double avgLoad = 0;
            if (!teachers.isEmpty()) {
                int totalLoad = 0;
                int validCount = 0;
                for (SysUser teacher : teachers) {
                    TeacherInfo teacherInfo = teacherInfoService.getByUserId(teacher.getId());
                    if (teacherInfo != null && teacherInfo.getCurrentAdvisorCount() != null) {
                        totalLoad += teacherInfo.getCurrentAdvisorCount();
                        validCount++;
                    }
                }
                avgLoad = validCount > 0 ? (double) totalLoad / validCount : 0;
            }
            stats.put("avgLoad", Math.round(avgLoad));
            
            // 今日分配数 - 今天创建的有指导老师的论文
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LambdaQueryWrapper<PaperInfo> todayWrapper = new LambdaQueryWrapper<>();
            todayWrapper.isNotNull(PaperInfo::getTeacherId)
                       .ge(PaperInfo::getCreateTime, todayStart);
            int todayAssignments = paperInfoMapper.selectCount(todayWrapper).intValue();
            stats.put("todayAssignments", todayAssignments);
            
            log.info("获取分配统计信息成功: {}", stats);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取分配统计信息失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取分配统计信息失败");
        }
    }

    @Override
    public Result<Page<Map<String, Object>>> getUnassignedStudents(String keyword, Integer page, Integer size) {
        try {
            Page<PaperInfo> paperPage = new Page<>(page, size);
            
            LambdaQueryWrapper<PaperInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNull(PaperInfo::getTeacherId) // 未分配指导老师
                   .eq(PaperInfo::getPaperStatus, "approved"); // 已通过审核
            
            // 关键词搜索
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w.like(PaperInfo::getPaperTitle, keyword)
                                 .or()
                                 .inSql(PaperInfo::getStudentId, 
                                        "SELECT id FROM sys_user WHERE real_name LIKE '%" + keyword + "%'"));
            }
            
            wrapper.orderByDesc(PaperInfo::getCreateTime);
            
            Page<PaperInfo> resultPage = paperInfoMapper.selectPage(paperPage, wrapper);
            
            // 转换为前端需要的格式
            Page<Map<String, Object>> resultMap = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
            List<Map<String, Object>> records = resultPage.getRecords().stream().map(paper -> {
                Map<String, Object> record = new HashMap<>();
                record.put("id", paper.getId());
                record.put("title", paper.getPaperTitle());
                record.put("studentId", paper.getStudentId());
                
                // 获取学生信息
                SysUser student = sysUserMapper.selectById(paper.getStudentId());
                if (student != null) {
                    record.put("name", student.getRealName());
                    record.put("collegeId", student.getCollegeId());
                    
                    // 从StudentInfo表获取学生详情
                    StudentInfo studentInfo = studentInfoService.getByUserId(student.getId());
                    if (studentInfo != null) {
                        record.put("major", studentInfo.getMajorId());
                        record.put("grade", studentInfo.getGrade());
                    }
                }
                
                return record;
            }).collect(Collectors.toList());
            
            resultMap.setRecords(records);
            
            log.info("获取未分配学生列表成功，共{}条记录", records.size());
            return Result.success(resultMap);
        } catch (Exception e) {
            log.error("获取未分配学生列表失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取未分配学生列表失败");
        }
    }

    @Override
    public Result<Page<Map<String, Object>>> getAvailableTeachers(String keyword, Integer page, Integer size) {
        try {
            // 先获取所有教师用户
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(SysUser::getUserType, "teacher")
                   .eq(SysUser::getStatus, 1);
            
            // 关键词搜索（只搜索姓名）
            if (keyword != null && !keyword.isEmpty()) {
                userWrapper.like(SysUser::getRealName, keyword);
            }
            
            List<SysUser> allTeachers = sysUserMapper.selectList(userWrapper);
            
            // 转换为带详细信息的记录
            List<Map<String, Object>> allRecords = allTeachers.stream().map(teacher -> {
                Map<String, Object> record = new HashMap<>();
                record.put("id", teacher.getId());
                record.put("name", teacher.getRealName());
                record.put("collegeId", teacher.getCollegeId());
                
                // 从TeacherInfo表获取教师详情
                TeacherInfo teacherInfo = teacherInfoService.getByUserId(teacher.getId());
                if (teacherInfo != null) {
                    record.put("title", teacherInfo.getProfessionalTitle());
                    record.put("department", teacherInfo.getOffice());
                    record.put("currentLoad", teacherInfo.getCurrentAdvisorCount());
                    record.put("maxLoad", teacherInfo.getMaxReviewCount());
                    
                    // 获取专长领域
                    List<String> expertise = Arrays.asList(Optional.ofNullable(teacherInfo.getResearchDirection())
                        .orElse("").split(","))
                        .stream()
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                    record.put("expertise", expertise);
                } else {
                    // 确保即使TeacherInfo不存在也能正常返回
                    record.put("title", "");
                    record.put("department", "");
                    record.put("currentLoad", 0);
                    record.put("maxLoad", 15); // 默认值
                    record.put("expertise", new ArrayList<>());
                }
                
                return record;
            }).collect(Collectors.toList());
            
            // 进一步过滤职称关键词
            if (keyword != null && !keyword.isEmpty()) {
                allRecords = allRecords.stream()
                    .filter(record -> record.get("name").toString().contains(keyword) || 
                                     record.get("title").toString().contains(keyword))
                    .collect(Collectors.toList());
            }
            
            // 按当前负载排序
            allRecords.sort(Comparator.comparingInt(record -> (Integer) record.get("currentLoad")));
            
            // 分页处理
            int total = allRecords.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, total);
            List<Map<String, Object>> pageRecords = start < total ? allRecords.subList(start, end) : new ArrayList<>();
            
            Page<Map<String, Object>> resultMap = new Page<>(page, size, total);
            resultMap.setRecords(pageRecords);
            
            log.info("获取可用教师列表成功，共{}条记录", pageRecords.size());
            return Result.success(resultMap);
        } catch (Exception e) {
            log.error("获取可用教师列表失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取可用教师列表失败");
        }
    }

    @Override
    public Result<String> singleAssign(String studentId, String teacherId, String remark) {
        try {
            // 验证学生和教师是否存在
            PaperInfo paper = paperInfoMapper.selectById(studentId);
            if (paper == null) {
                return Result.error(ResultCode.PARAM_ERROR, "论文不存在");
            }
            
            SysUser teacher = sysUserMapper.selectById(teacherId);
            if (teacher == null) {
                return Result.error(ResultCode.PARAM_ERROR, "教师不存在");
            }
            
            if (!"teacher".equals(teacher.getUserType())) {
                return Result.error(ResultCode.PARAM_ERROR, "指定用户不是教师");
            }
            
            // 执行分配
            paper.setTeacherId(Long.valueOf(teacherId));
            paper.setUpdateTime(LocalDateTime.now());
            paperInfoMapper.updateById(paper);
            
            // 更新教师负载
            TeacherInfo teacherInfo = teacherInfoService.getByUserId(Long.valueOf(teacherId));
            if (teacherInfo != null) {
                teacherInfo.setCurrentAdvisorCount(teacherInfo.getCurrentAdvisorCount() + 1);
                teacherInfo.setUpdateTime(LocalDateTime.now());
                teacherInfoService.saveOrUpdate(teacherInfo);
            }
            
            log.info("单个分配成功: 学生ID={}, 教师ID={}", studentId, teacherId);
            return Result.success("分配成功");
        } catch (Exception e) {
            log.error("单个分配失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "分配失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> batchAssign(List<String> studentIds, String teacherId, String strategy, String remark) {
        try {
            Map<String, Object> result = new HashMap<>();
            int successCount = 0;
            int failCount = 0;
            List<String> details = new ArrayList<>();
            
            SysUser teacher = sysUserMapper.selectById(teacherId);
            if (teacher == null) {
                return Result.error(ResultCode.PARAM_ERROR, "教师不存在");
            }
            
            if (!"teacher".equals(teacher.getUserType())) {
                return Result.error(ResultCode.PARAM_ERROR, "指定用户不是教师");
            }
            
            // 从TeacherInfo表获取教师负载信息
            TeacherInfo teacherInfo = teacherInfoService.getByUserId(Long.valueOf(teacherId));
            if (teacherInfo == null) {
                return Result.error(ResultCode.PARAM_ERROR, "教师信息不存在");
            }
            
            int maxLoad = teacherInfo.getMaxReviewCount() != null ? teacherInfo.getMaxReviewCount() : 15;
            int currentLoad = teacherInfo.getCurrentAdvisorCount() != null ? teacherInfo.getCurrentAdvisorCount() : 0;
            int availableSlots = maxLoad - currentLoad;
            if (availableSlots <= 0) {
                return Result.error(ResultCode.PARAM_ERROR, "该教师已达到最大指导学生数");
            }
            
            // 限制批量分配数量不超过教师可用名额
            List<String> assignableIds = studentIds.stream()
                .limit(availableSlots)
                .collect(Collectors.toList());
            
            for (String studentId : assignableIds) {
                try {
                    PaperInfo paper = paperInfoMapper.selectById(studentId);
                    if (paper == null) {
                        details.add("论文ID " + studentId + " 不存在");
                        failCount++;
                        continue;
                    }
                    
                    if (paper.getTeacherId() != null) {
                        details.add("论文ID " + studentId + " 已有指导老师");
                        failCount++;
                        continue;
                    }
                    
                    // 执行分配
                    paper.setTeacherId(Long.valueOf(teacherId));
                    paper.setUpdateTime(LocalDateTime.now());
                    paperInfoMapper.updateById(paper);
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("批量分配单个学生失败: studentId={}", studentId, e);
                    details.add("论文ID " + studentId + " 分配失败: " + e.getMessage());
                    failCount++;
                }
            }
            
            // 更新教师负载
            if (successCount > 0) {
                teacherInfo.setCurrentAdvisorCount(teacherInfo.getCurrentAdvisorCount() + successCount);
                teacherInfo.setUpdateTime(LocalDateTime.now());
                teacherInfoService.saveOrUpdate(teacherInfo);
            }
            
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("details", details);
            
            log.info("批量分配完成: 成功{}个，失败{}个", successCount, failCount);
            return Result.success("批量分配完成", result);
        } catch (Exception e) {
            log.error("批量分配失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量分配失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, String>> getMajorNameMap() {
        try {
            LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysDictData::getDictType, "major")
                   .eq(SysDictData::getStatus, "0"); // 启用状态
            
            List<SysDictData> majors = sysDictDataMapper.selectList(wrapper);
            
            Map<String, String> majorMap = majors.stream()
                .collect(Collectors.toMap(
                    SysDictData::getDictValue,
                    SysDictData::getDictLabel,
                    (existing, replacement) -> existing
                ));
            
            log.info("获取专业名称映射成功，共{}个专业", majorMap.size());
            return Result.success(majorMap);
        } catch (Exception e) {
            log.error("获取专业名称映射失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取专业名称映射失败");
        }
    }

    @Override
    public Result<AssignmentRuleConfigDTO> getAssignmentRules() {
        try {
            // 从数据库获取真实的分配规则配置
            AssignmentRuleConfigDTO config = new AssignmentRuleConfigDTO();
            
            // 从系统参数表获取配置
            LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysDictData::getDictType, "assignment_config")
                   .eq(SysDictData::getStatus, "0");
            
            List<SysDictData> configs = sysDictDataMapper.selectList(wrapper);
            
            // 根据字典数据设置配置值
            for (SysDictData dict : configs) {
                switch (dict.getDictValue()) {
                    case "max_load_per_teacher":
                        config.setMaxLoadPerTeacher(Integer.valueOf(dict.getDictLabel()));
                        break;
                    case "balance_strategy":
                        config.setBalanceStrategy(dict.getDictLabel());
                        break;
                    case "auto_match_interests":
                        config.setAutoMatchInterests("true".equalsIgnoreCase(dict.getDictLabel()));
                        break;
                    case "department_priority":
                        config.setDepartmentPriority("true".equalsIgnoreCase(dict.getDictLabel()));
                        break;
                    case "smart_recommendation":
                        config.setSmartRecommendation("true".equalsIgnoreCase(dict.getDictLabel()));
                        break;
                    case "min_load_per_teacher":
                        config.setMinLoadPerTeacher(Integer.valueOf(dict.getDictLabel()));
                        break;
                    case "cross_major_limit":
                        config.setCrossMajorLimit("true".equalsIgnoreCase(dict.getDictLabel()));
                        break;
                }
            }
            
            // 如果没有配置，默认值
            if (config.getMaxLoadPerTeacher() == null) {
                config.setMaxLoadPerTeacher(15);
            }
            if (config.getBalanceStrategy() == null) {
                config.setBalanceStrategy("load_balance");
            }
            if (config.getAutoMatchInterests() == null) {
                config.setAutoMatchInterests(true);
            }
            if (config.getDepartmentPriority() == null) {
                config.setDepartmentPriority(true);
            }
            if (config.getSmartRecommendation() == null) {
                config.setSmartRecommendation(true);
            }
            if (config.getMinLoadPerTeacher() == null) {
                config.setMinLoadPerTeacher(3);
            }
            if (config.getCrossMajorLimit() == null) {
                config.setCrossMajorLimit(false);
            }
            
            log.info("获取分配规则配置成功");
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取分配规则配置失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取分配规则配置失败");
        }
    }

    @Override
    public Result<String> saveAssignmentRules(AssignmentRuleConfigDTO config) {
        try {
            // 将配置保存到数据库字典表
            saveConfigToDict("max_load_per_teacher", String.valueOf(config.getMaxLoadPerTeacher()), "每个教师最大指导学生数");
            saveConfigToDict("balance_strategy", config.getBalanceStrategy(), "平衡策略");
            saveConfigToDict("auto_match_interests", String.valueOf(config.getAutoMatchInterests()), "是否自动匹配研究兴趣");
            saveConfigToDict("department_priority", String.valueOf(config.getDepartmentPriority()), "是否优先同部门分配");
            saveConfigToDict("smart_recommendation", String.valueOf(config.getSmartRecommendation()), "是否启用智能推荐");
            saveConfigToDict("min_load_per_teacher", String.valueOf(config.getMinLoadPerTeacher()), "最小指导学生数");
            saveConfigToDict("cross_major_limit", String.valueOf(config.getCrossMajorLimit()), "跨专业分配限制");
            
            // 同时更新sys_user表中的最大审核数量
            updateTeachersMaxReviewCount(config.getMaxLoadPerTeacher());
            
            log.info("保存分配规则配置成功: {}", config);
            return Result.success("保存成功");
        } catch (Exception e) {
            log.error("保存分配规则配置失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "保存分配规则配置失败");
        }
    }
    
    /**
     * 更新所有教师的最大审核数量
     */
    private void updateTeachersMaxReviewCount(Integer maxLoadPerTeacher) {
        try {
            // 先获取所有教师用户
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(SysUser::getUserType, "teacher");
            List<SysUser> teachers = sysUserMapper.selectList(userWrapper);
            
            // 批量更新每个教师的最大审核数量
            int updatedCount = 0;
            for (SysUser teacher : teachers) {
                TeacherInfo teacherInfo = teacherInfoService.getByUserId(teacher.getId());
                if (teacherInfo != null) {
                    teacherInfo.setMaxReviewCount(maxLoadPerTeacher);
                    teacherInfo.setUpdateTime(LocalDateTime.now());
                    teacherInfoService.saveOrUpdate(teacherInfo);
                    updatedCount++;
                }
            }
            
            log.info("更新教师最大审核数量成功，影响记录数: {}", updatedCount);
        } catch (Exception e) {
            log.error("更新教师最大审核数量失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 保存配置到字典表
     */
    private void saveConfigToDict(String configKey, String configValue, String description) {
        try {
            LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysDictData::getDictType, "assignment_config")
                   .eq(SysDictData::getDictValue, configKey);
            
            SysDictData existing = sysDictDataMapper.selectOne(wrapper);
            
            if (existing != null) {
                // 更新现有配置
                existing.setDictLabel(configValue);
                existing.setRemark(description);
                existing.setUpdateTime(LocalDateTime.now());
                sysDictDataMapper.updateById(existing);
            } else {
                // 创建新配置
                SysDictData newConfig = new SysDictData();
                newConfig.setDictType("assignment_config");
                newConfig.setDictValue(configKey);
                newConfig.setDictLabel(configValue);
                newConfig.setDictSort(0);
                newConfig.setStatus("0");
                newConfig.setRemark(description);
                newConfig.setCreateTime(LocalDateTime.now());
                newConfig.setUpdateTime(LocalDateTime.now());
                sysDictDataMapper.insert(newConfig);
            }
        } catch (Exception e) {
            log.warn("保存配置项失败: key={}, value={}, error={}", configKey, configValue, e.getMessage());
        }
    }

    @Override
    public Result<String> refreshAssignmentData() {
        try {
            // 这里可以添加数据刷新逻辑
            // 目前只是简单返回成功
            log.info("刷新分配数据成功");
            return Result.success("数据刷新成功");
        } catch (Exception e) {
            log.error("刷新分配数据失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "数据刷新失败");
        }
    }
}