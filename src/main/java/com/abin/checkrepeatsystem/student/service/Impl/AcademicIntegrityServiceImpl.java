package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.common.Exception.BusinessException;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.pojo.entity.AcademicChecklist;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.student.dto.AcademicResourceDTO;
import com.abin.checkrepeatsystem.student.dto.ChecklistItemDTO;
import com.abin.checkrepeatsystem.student.dto.PersonalAdviceDTO;
import com.abin.checkrepeatsystem.student.mapper.AcademicChecklistMapper;
import com.abin.checkrepeatsystem.student.service.AcademicIntegrityService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 学术诚信服务实现类
 */
@Slf4j
@Service
public class AcademicIntegrityServiceImpl implements AcademicIntegrityService {
    
    @Resource
    private AcademicChecklistMapper academicChecklistMapper;
    
    @Resource
    private PaperInfoMapper paperInfoMapper;
    
    @Resource
    private CheckTaskMapper checkTaskMapper;
    
    @Override
    public PersonalAdviceDTO getPersonalAdvice(Long studentId) {
        try {
            log.info("获取学生个性化学术建议 - 学生ID: {}", studentId);
            
            // 1. 获取学生的最新论文
            PaperInfo latestPaper = paperInfoMapper.selectLatestPaper(studentId);
            if (latestPaper == null) {
                // 如果没有论文，返回默认建议
                return createDefaultPersonalAdvice();
            }
            
            // 2. 获取最新的查重任务
            CheckTask latestTask = checkTaskMapper.selectLatestByPaperId(latestPaper.getId());
            if (latestTask == null || latestTask.getCheckRate() == null) {
                return createDefaultPersonalAdvice();
            }
            
            // 3. 基于查重结果生成个性化建议
            PersonalAdviceDTO advice = new PersonalAdviceDTO();
            advice.setVersion(1); // 使用固定版本号
            
            // 4. 分析高风险区域（模拟数据）
            List<PersonalAdviceDTO.HighRiskAreaDTO> highRiskAreas = analyzeHighRiskAreas(latestTask.getCheckRate().doubleValue());
            advice.setHighRiskAreas(highRiskAreas);
            
            // 5. 识别表现良好的方面
            List<PersonalAdviceDTO.GoodAspectDTO> goodAspects = identifyGoodAspects(latestTask.getCheckRate().doubleValue());
            advice.setGoodAspects(goodAspects);
            
            // 6. 生成通用建议
            List<String> generalTips = generateGeneralTips(latestTask.getCheckRate().doubleValue());
            advice.setGeneralTips(generalTips);
            
            log.info("个性化学术建议生成成功 - 学生ID: {}, 相似度: {}", studentId, latestTask.getCheckRate());
            return advice;
            
        } catch (Exception e) {
            log.error("获取个性化学术建议失败 - 学生ID: {}", studentId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "获取个性化学术建议失败");
        }
    }
    
    @Override
    public List<AcademicResourceDTO> getRecommendedResources(Long studentId, String resourceType) {
        try {
            log.info("获取推荐学习资源 - 学生ID: {}, 资源类型: {}", studentId, resourceType);
            
            // 1. 获取学生最新论文信息，用于个性化推荐
            PaperInfo latestPaper = paperInfoMapper.selectLatestPaper(studentId);
            String subjectArea = latestPaper != null ? latestPaper.getSubjectCode() : "general";
            
            // 2. 根据学科领域和资源类型筛选资源
            List<AcademicResourceDTO> allResources = getAllAcademicResources();
            List<AcademicResourceDTO> filteredResources = allResources.stream()
                    .filter(resource -> resourceType == null || resource.getType().equals(resourceType))
                    .filter(resource -> "general".equals(subjectArea) || resource.getCategory().contains(subjectArea.toLowerCase()))
                    .limit(10)
                    .collect(Collectors.toList());
            
            log.info("推荐学习资源获取成功 - 学生ID: {}, 返回资源数: {}", studentId, filteredResources.size());
            return filteredResources;
            
        } catch (Exception e) {
            log.error("获取推荐学习资源失败 - 学生ID: {}", studentId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "获取推荐学习资源失败");
        }
    }
    
    @Override
    public List<ChecklistItemDTO> getChecklist(Long studentId) {
        try {
            log.info("获取用户检查清单 - 学生ID: {}", studentId);
            
            // 1. 检查用户是否已有检查清单
            LambdaQueryWrapper<AcademicChecklist> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AcademicChecklist::getStudentId, studentId);
            List<AcademicChecklist> checklistItems = academicChecklistMapper.selectList(queryWrapper);
            
            // 2. 如果没有，初始化默认检查清单
            if (checklistItems.isEmpty()) {
                initializeChecklist(studentId);
                checklistItems = academicChecklistMapper.selectList(queryWrapper);
            }
            
            // 3. 转换为DTO
            List<ChecklistItemDTO> result = checklistItems.stream()
                    .map(item -> {
                        ChecklistItemDTO dto = new ChecklistItemDTO();
                        dto.setItemId(item.getId());
                        dto.setText(item.getText());
                        dto.setChecked(item.getChecked());
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            log.info("用户检查清单获取成功 - 学生ID: {}, 项目数: {}", studentId, result.size());
            return result;
            
        } catch (Exception e) {
            log.error("获取用户检查清单失败 - 学生ID: {}", studentId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "获取用户检查清单失败");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateChecklistItem(Long studentId, Long itemId, Boolean checked) {
        try {
            log.info("更新检查项状态 - 学生ID: {}, 项目ID: {}, 状态: {}", studentId, itemId, checked);
            
            // 1. 验证权限
            AcademicChecklist item = academicChecklistMapper.selectById(itemId);
            if (item == null || !item.getStudentId().equals(studentId)) {
                throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限操作此检查项");
            }
            
            // 2. 更新状态
            item.setChecked(checked);
            item.setUpdateTime(LocalDateTime.now());
            int result = academicChecklistMapper.updateById(item);
            
            log.info("检查项状态更新成功 - 项目ID: {}, 结果: {}", itemId, result > 0);
            return result > 0;
            
        } catch (Exception e) {
            log.error("更新检查项状态失败 - 学生ID: {}, 项目ID: {}", studentId, itemId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "更新检查项状态失败");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeChecklist(Long studentId) {
        try {
            log.info("初始化用户检查清单 - 学生ID: {}", studentId);
            
            // 1. 检查是否已存在
            LambdaQueryWrapper<AcademicChecklist> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AcademicChecklist::getStudentId, studentId);
            if (academicChecklistMapper.selectCount(queryWrapper) > 0) {
                log.info("用户检查清单已存在，无需初始化 - 学生ID: {}", studentId);
                return;
            }
            
            // 2. 从数据库获取默认检查清单项
            // 暂时使用空列表，后续从数据库获取
            List<String> defaultItems = new ArrayList<>();
            
            for (int i = 0; i < defaultItems.size(); i++) {
                AcademicChecklist item = new AcademicChecklist();
                item.setStudentId(studentId);
                item.setText(defaultItems.get(i));
                item.setChecked(false);
                item.setSort(i + 1);
                UserBusinessInfoUtils.setAuditField(item, true);
                academicChecklistMapper.insert(item);
            }
            
            log.info("用户检查清单初始化成功 - 学生ID: {}, 项目数: {}", studentId, defaultItems.size());
            
        } catch (Exception e) {
            log.error("初始化用户检查清单失败 - 学生ID: {}", studentId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "初始化用户检查清单失败");
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 创建默认个性化建议
     */
    private PersonalAdviceDTO createDefaultPersonalAdvice() {
        PersonalAdviceDTO advice = new PersonalAdviceDTO();
        advice.setVersion(1);
        
        // 从数据库获取默认高风险区域
        advice.setHighRiskAreas(new ArrayList<>());
        
        // 从数据库获取默认良好方面
        advice.setGoodAspects(new ArrayList<>());
        
        // 从数据库获取默认通用建议
        advice.setGeneralTips(new ArrayList<>());
        
        return advice;
    }
    
    /**
     * 分析高风险区域
     */
    private List<PersonalAdviceDTO.HighRiskAreaDTO> analyzeHighRiskAreas(double similarity) {
        // 从数据库获取高风险区域数据
        // 暂时返回空列表，后续从数据库获取
        return new ArrayList<>();
    }
    
    /**
     * 识别表现良好的方面
     */
    private List<PersonalAdviceDTO.GoodAspectDTO> identifyGoodAspects(double similarity) {
        // 从数据库获取表现良好的方面
        // 暂时返回空列表，后续从数据库获取
        return new ArrayList<>();
    }
    
    /**
     * 生成通用改进建议
     */
    private List<String> generateGeneralTips(double similarity) {
        // 从数据库获取通用改进建议
        // 暂时返回空列表，后续从数据库获取
        return new ArrayList<>();
    }
    
    /**
     * 获取所有学术资源
     */
    private List<AcademicResourceDTO> getAllAcademicResources() {
        // 从数据库获取学术资源
        // 暂时返回空列表，后续从数据库获取
        return new ArrayList<>();
    }
    
    /**
     * 创建资源对象
     */
    private AcademicResourceDTO createResource(Long id, String title, String type, 
                                             String description, String url, String category) {
        AcademicResourceDTO resource = new AcademicResourceDTO();
        resource.setResourceId(id);
        resource.setTitle(title);
        resource.setType(type);
        resource.setDescription(description);
        resource.setUrl(url);
        resource.setCategory(category);
        return resource;
    }
}