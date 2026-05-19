package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.ai.config.AIConfigProperties;
import com.abin.checkrepeatsystem.ai.dto.AIAnalysisResponse;
import com.abin.checkrepeatsystem.ai.prompt.AcademicPromptTemplates;
import com.abin.checkrepeatsystem.ai.service.AIService;
import com.abin.checkrepeatsystem.common.Exception.BusinessException;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.pojo.entity.AcademicChecklist;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.student.dto.AcademicResourceDTO;
import com.abin.checkrepeatsystem.student.dto.ChecklistItemDTO;
import com.abin.checkrepeatsystem.student.dto.PersonalAdviceDTO;
import com.abin.checkrepeatsystem.student.mapper.AcademicChecklistMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.student.service.AcademicIntegrityService;
import com.alibaba.fastjson.JSON;
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

    @Resource
    private CheckReportMapper checkReportMapper;

    @Resource
    private AIService aiService;

    @Resource
    private AIConfigProperties aiConfig;
    
    @Override
    public PersonalAdviceDTO getPersonalAdvice(Long studentId) {
        try {
            log.info("获取学生个性化学术建议 - 学生ID: {}", studentId);

            // 1. 获取学生的最新论文
            PaperInfo latestPaper = paperInfoMapper.selectLatestPaper(studentId);
            if (latestPaper == null) {
                return createDefaultPersonalAdvice();
            }

            // 2. 获取最新的查重任务
            CheckTask latestTask = checkTaskMapper.selectLatestByPaperId(latestPaper.getId());
            if (latestTask == null || latestTask.getCheckRate() == null) {
                return createDefaultPersonalAdvice();
            }

            // 3. 尝试 AI 分析
            if (aiConfig.isEnabled()) {
                try {
                    return getAIPersonalAdvice(latestPaper, latestTask);
                } catch (Exception e) {
                    log.warn("AI 生成建议失败，回退到模板逻辑: {}", e.getMessage());
                }
            }

            // 4. 回退：基于查重结果生成硬编码建议
            return getFallbackPersonalAdvice(latestTask.getCheckRate().doubleValue());

        } catch (Exception e) {
            log.error("获取个性化学术建议失败 - 学生ID: {}", studentId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "获取个性化学术建议失败");
        }
    }

    private PersonalAdviceDTO getAIPersonalAdvice(PaperInfo paperInfo, CheckTask checkTask) {
        CheckReport checkReport = checkReportMapper.selectOne(
            new LambdaQueryWrapper<CheckReport>()
                .eq(CheckReport::getTaskId, checkTask.getId())
                .eq(CheckReport::getIsDeleted, 0)
        );

        String systemPrompt = AcademicPromptTemplates.buildPersonalAdviceSystemPrompt();
        String userMessage = AcademicPromptTemplates.buildPersonalAdviceUserMessage(
            paperInfo, checkTask, checkReport);
        String aiResult = aiService.chat(systemPrompt, userMessage);

        AIAnalysisResponse aiResponse = JSON.parseObject(aiResult, AIAnalysisResponse.class);
        return mapToPersonalAdviceDTO(aiResponse);
    }

    private PersonalAdviceDTO mapToPersonalAdviceDTO(AIAnalysisResponse ai) {
        PersonalAdviceDTO advice = new PersonalAdviceDTO();
        advice.setVersion(1);

        List<PersonalAdviceDTO.HighRiskAreaDTO> highRiskAreas = new ArrayList<>();
        if (ai.getHighRiskAreas() != null) {
            for (AIAnalysisResponse.HighRiskArea area : ai.getHighRiskAreas()) {
                PersonalAdviceDTO.HighRiskAreaDTO dto = new PersonalAdviceDTO.HighRiskAreaDTO();
                dto.setSection(area.getSection());
                dto.setSimilarity(area.getSimilarity());
                dto.setIssue(area.getIssue());
                dto.setSuggestion(area.getSuggestion());
                highRiskAreas.add(dto);
            }
        }
        advice.setHighRiskAreas(highRiskAreas);

        List<PersonalAdviceDTO.GoodAspectDTO> goodAspects = new ArrayList<>();
        if (ai.getGoodAspects() != null) {
            for (AIAnalysisResponse.GoodAspect aspect : ai.getGoodAspects()) {
                PersonalAdviceDTO.GoodAspectDTO dto = new PersonalAdviceDTO.GoodAspectDTO();
                dto.setSection(aspect.getSection());
                dto.setSimilarity(aspect.getSimilarity());
                dto.setStrength(aspect.getStrength());
                dto.setEncouragement(aspect.getEncouragement());
                goodAspects.add(dto);
            }
        }
        advice.setGoodAspects(goodAspects);

        List<String> tips = ai.getGeneralTips() != null ? ai.getGeneralTips() : new ArrayList<>();
        advice.setGeneralTips(tips);

        log.info("AI 个性化学术建议生成成功");
        return advice;
    }

    private PersonalAdviceDTO getFallbackPersonalAdvice(double checkRate) {
        PersonalAdviceDTO advice = new PersonalAdviceDTO();
        advice.setVersion(1);
        advice.setHighRiskAreas(analyzeHighRiskAreas(checkRate));
        advice.setGoodAspects(identifyGoodAspects(checkRate));
        advice.setGeneralTips(generateGeneralTips(checkRate));
        return advice;
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
            
            // 2. 默认检查清单项
            List<String> defaultItems = getDefaultChecklistItems();
            
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
     * 创建默认个性化建议（用户尚无查重记录时使用）
     */
    private PersonalAdviceDTO createDefaultPersonalAdvice() {
        PersonalAdviceDTO advice = new PersonalAdviceDTO();
        advice.setVersion(1);
        advice.setHighRiskAreas(new ArrayList<>());
        advice.setGoodAspects(new ArrayList<>());
        advice.setGeneralTips(Arrays.asList(
            "完成首次论文查重后，系统将为您生成个性化的学术诚信建议",
            "请确保引用格式规范，所有参考文献均在正文中标注",
            "建议提前查重，为修改预留充足时间"
        ));
        return advice;
    }

    /**
     * 分析高风险区域（基于相似度分5档）
     */
    private List<PersonalAdviceDTO.HighRiskAreaDTO> analyzeHighRiskAreas(double similarity) {
        List<PersonalAdviceDTO.HighRiskAreaDTO> areas = new ArrayList<>();

        if (similarity < 15) {
            PersonalAdviceDTO.HighRiskAreaDTO area = new PersonalAdviceDTO.HighRiskAreaDTO();
            area.setSection("引用规范");
            area.setSimilarity("低");
            area.setIssue("您当前的论文相似度较低（" + String.format("%.1f", similarity) + "%），整体原创性良好");
            area.setSuggestion("继续保持良好的引用习惯，注意所有间接引用也要标注来源");
            areas.add(area);
        } else if (similarity < 30) {
            PersonalAdviceDTO.HighRiskAreaDTO a1 = new PersonalAdviceDTO.HighRiskAreaDTO();
            a1.setSection("文献综述");
            a1.setSimilarity("中等偏低");
            a1.setIssue("相似度达到 " + String.format("%.1f", similarity) + "%，在文献综述部分检测到一定重复");
            a1.setSuggestion("检查文献综述部分，确保对已有研究的描述是用自己的语言重新组织，而非直接翻译或改写");
            areas.add(a1);

            PersonalAdviceDTO.HighRiskAreaDTO a2 = new PersonalAdviceDTO.HighRiskAreaDTO();
            a2.setSection("背景介绍");
            a2.setSimilarity("中等偏低");
            a2.setIssue("背景部分可能存在与已有论文类似的表述");
            a2.setSuggestion("在描述研究背景时，尝试从你的研究视角出发进行阐述");
            areas.add(a2);
        } else if (similarity < 50) {
            PersonalAdviceDTO.HighRiskAreaDTO a1 = new PersonalAdviceDTO.HighRiskAreaDTO();
            a1.setSection("方法论");
            a1.setSimilarity("中等偏高");
            a1.setIssue("相似度达到 " + String.format("%.1f", similarity) + "%，方法论和讨论部分需要重点检查");
            a1.setSuggestion("方法论部分可能参考了标准流程，建议补充你的具体实验设计和参数设置细节");
            areas.add(a1);

            PersonalAdviceDTO.HighRiskAreaDTO a2 = new PersonalAdviceDTO.HighRiskAreaDTO();
            a2.setSection("文献综述");
            a2.setSimilarity("中等偏高");
            a2.setIssue("文献综述部分存在较多与其他论文相似的段落");
            a2.setSuggestion("重新整理文献综述，减少长段引用，增加批判性分析和你的研究关联性论述");
            areas.add(a2);

            PersonalAdviceDTO.HighRiskAreaDTO a3 = new PersonalAdviceDTO.HighRiskAreaDTO();
            a3.setSection("讨论与分析");
            a3.setSimilarity("中等偏高");
            a3.setIssue("讨论部分与已有成果的对比表述可能存在重复");
            a3.setSuggestion("强化你自己的数据分析和观点，减少对前人结论的复述");
            areas.add(a3);
        } else if (similarity < 70) {
            PersonalAdviceDTO.HighRiskAreaDTO a1 = new PersonalAdviceDTO.HighRiskAreaDTO();
            a1.setSection("正文整体");
            a1.setSimilarity("高");
            a1.setIssue("论文相似度达到 " + String.format("%.1f", similarity) + "%，存在较高的重复率");
            a1.setSuggestion("请逐段检查重复内容，重点关注理论框架、实验方法等章节。建议使用查重报告定位具体相似段落");
            areas.add(a1);

            PersonalAdviceDTO.HighRiskAreaDTO a2 = new PersonalAdviceDTO.HighRiskAreaDTO();
            a2.setSection("结论");
            a2.setSimilarity("高");
            a2.setIssue("结论部分通常应该是最具原创性的部分，但检测到较高相似度");
            a2.setSuggestion("结论应基于你自己的实验数据和发现来撰写，无需引用过多前人结论");
            areas.add(a2);

            PersonalAdviceDTO.HighRiskAreaDTO a3 = new PersonalAdviceDTO.HighRiskAreaDTO();
            a3.setSection("引用格式");
            a3.setSimilarity("高");
            a3.setIssue("高相似度可能部分源于引用格式不当，未正确标注的引用被计入重复");
            a3.setSuggestion("核查所有引用是否使用了正确的引用格式（GB/T 7714或你学校指定的格式）");
            areas.add(a3);
        } else {
            PersonalAdviceDTO.HighRiskAreaDTO a1 = new PersonalAdviceDTO.HighRiskAreaDTO();
            a1.setSection("全文");
            a1.setSimilarity("严重");
            a1.setIssue("论文相似度高达 " + String.format("%.1f", similarity) + "%，存在严重的学术诚信风险");
            a1.setSuggestion("论文可能无法通过审核。建议全面重写高重复段落，确保每个章节都以自己的表述为主。必要时与导师沟通修改方向");
            areas.add(a1);
        }

        return areas;
    }

    /**
     * 识别表现良好的方面
     */
    private List<PersonalAdviceDTO.GoodAspectDTO> identifyGoodAspects(double similarity) {
        List<PersonalAdviceDTO.GoodAspectDTO> aspects = new ArrayList<>();

        if (similarity < 30) {
            PersonalAdviceDTO.GoodAspectDTO g1 = new PersonalAdviceDTO.GoodAspectDTO();
            g1.setSection("整体原创性");
            g1.setSimilarity(String.format("%.1f%%", similarity));
            g1.setStrength("论文整体原创性较好，重复率在安全范围内");
            g1.setEncouragement("继续保持严谨的学术态度，这是你独立研究能力的体现");
            aspects.add(g1);
        } else {
            PersonalAdviceDTO.GoodAspectDTO g1 = new PersonalAdviceDTO.GoodAspectDTO();
            g1.setSection("主动查重");
            g1.setSimilarity(String.format("%.1f%%", similarity));
            g1.setStrength("你主动使用了查重系统，这是学术诚信意识良好的表现");
            g1.setEncouragement("发现问题是改进的第一步，根据报告针对性修改后可大幅降低重复率");
            aspects.add(g1);
        }

        PersonalAdviceDTO.GoodAspectDTO g2 = new PersonalAdviceDTO.GoodAspectDTO();
        g2.setSection("学术规范意识");
        g2.setSimilarity("—");
        g2.setStrength("你正在积极使用学术诚信检查工具，这表明你重视学术规范");
        g2.setEncouragement("持续的规范意识会让你的学术写作越来越成熟");
        aspects.add(g2);

        return aspects;
    }

    /**
     * 生成通用改进建议
     */
    private List<String> generateGeneralTips(double similarity) {
        List<String> tips = new ArrayList<>();

        if (similarity < 15) {
            tips.add("你的论文查重率较低，整体原创性良好。在提交前做最后检查即可");
            tips.add("确认所有参考文献格式统一（建议使用参考文献管理工具如Zotero或EndNote）");
            tips.add("检查文中引用的图表是否已获得授权并注明来源");
        } else if (similarity < 30) {
            tips.add("对重复段落使用'改写+引用'的双重策略：用自己的话重新表述后仍标注来源");
            tips.add("合理使用参考文献管理软件（如Zotero、EndNote）可以减少格式错误");
            tips.add("修改后建议48小时后重新查重，获得更准确的对比结果");
            tips.add("直接引用超过3行的内容建议使用缩进引用块格式");
        } else if (similarity < 50) {
            tips.add("优先修改重复率最高的章节，逐段对照查重报告进行修订");
            tips.add("改写技巧：先理解原文 → 合上书用自己话写 → 再对照原文检查 → 标注引用");
            tips.add("减少长段直接引用，改为归纳总结后用自己的语言表述");
            tips.add("检查是否过度依赖少数几篇参考文献，增加文献来源多样性");
            tips.add("修改完成后，建议请同学或导师帮忙预审");
        } else if (similarity < 70) {
            tips.add("重点排查：引言、理论框架、实验方法等章节的重复率通常最高");
            tips.add("对大量重复的段落，不要简单'换词'，而应重新组织结构并补充个人见解");
            tips.add("检查自己之前的论文/报告是否被系统纳入对比库（自我抄袭也算重复）");
            tips.add("大幅修改后建议先在自己文档中对比前后版本，确认改动足够实质性");
            tips.add("如对查重结果有疑议，可在系统中申请人工复核");
        } else {
            tips.add("超过70%的重复率意味着论文可能需要大幅重写，建议与导师详细沟通");
            tips.add("重新规划论文结构，从你的实验数据和发现出发重新组织内容");
            tips.add("查看学校学术诚信政策，了解高重复率的后果和处理流程");
            tips.add("考虑参加学术写作辅导课程或工作坊提升写作能力");
            tips.add("这不是终点：许多优秀论文都经过多次大幅度修改才最终通过");
        }

        return tips;
    }

    /**
     * 获取默认检查清单项
     */
    private List<String> getDefaultChecklistItems() {
        return Arrays.asList(
            "所有直接引用已加引号并标注出处",
            "所有间接引用（改写）已标注来源",
            "参考文献列表完整且格式统一",
            "未使用他人未发表的论文或数据",
            "图表、数据引用已获得授权并注明来源",
            "未将同一篇论文提交至多门课程",
            "AI辅助写作内容已标注并符合学校规定",
            "合作研究的分工已在论文中说明",
            "所有引用文献均已在正文中出现",
            "致谢部分已包含所有实质性帮助者",
            "论文数据真实可复现",
            "未委托第三方代写论文",
            "已理解学校的学术不端处罚规定",
            "论文中使用的代码/算法已标注来源",
            "翻译/改编内容已获得原作者许可并标注"
        );
    }

    /**
     * 获取所有学术资源（种子数据）
     */
    private List<AcademicResourceDTO> getAllAcademicResources() {
        List<AcademicResourceDTO> resources = new ArrayList<>();

        resources.add(createResource(1L, "学术诚信：从认识到实践", "BOOK",
            "系统讲解学术诚信的基本概念、常见误区和实践指南",
            "https://book.douban.com/subject/1234567/", "general"));
        resources.add(createResource(2L, "APA格式完全指南（第7版）", "BOOK",
            "APA引用格式的权威参考书，涵盖所有类型的文献引用方法",
            "https://apastyle.apa.org/", "general"));
        resources.add(createResource(3L, "如何避免抄袭：学术写作指南", "ONLINE",
            "哈佛大学写作中心提供的免费在线课程，涵盖引文、改述和学术规范",
            "https://writingcenter.harvard.edu/", "general"));
        resources.add(createResource(4L, "Zotero文献管理工具教程", "VIDEO",
            "从安装到高级使用的完整Zotero教程，帮助你高效管理参考文献",
            "https://www.zotero.org/support/", "general"));
        resources.add(createResource(5L, "科研伦理与学术规范", "COURSE",
            "中国大学MOOC课程，由知名学者讲授科研伦理与学术规范",
            "https://www.icourse163.org/", "general"));
        resources.add(createResource(6L, "Grammarly写作辅助工具", "ONLINE",
            "英文论文语法检查和查重辅助工具",
            "https://www.grammarly.com/", "general"));
        resources.add(createResource(7L, "中国知网学术不端文献检测系统使用说明", "DOCUMENT",
            "知网查重系统的官方使用指南和结果解读方法",
            "https://check.cnki.net/", "general"));
        resources.add(createResource(8L, "EndNote 20入门指南", "VIDEO",
            "EndNote文献管理软件的入门教程，适合初学者",
            "https://endnote.com/training/", "general"));
        resources.add(createResource(9L, "计算机科学论文写作指南", "BOOK",
            "针对CS专业的论文写作指导，含代码引用和算法描述规范",
            "https://book.douban.com/subject/2345678/", "cs"));
        resources.add(createResource(10L, "工程类论文写作与学术规范", "BOOK",
            "工程学科的论文写作方法、数据呈现规范和学术伦理",
            "https://book.douban.com/subject/3456789/", "engineering"));
        resources.add(createResource(11L, "医学论文写作与发表伦理", "BOOK",
            "医学领域的学术写作规范、患者数据使用伦理和发表要求",
            "https://book.douban.com/subject/4567890/", "medicine"));
        resources.add(createResource(12L, "理科实验数据管理与可重复性", "ONLINE",
            "Nature发布的数据管理指南，确保实验数据的真实性和可复现性",
            "https://www.nature.com/", "science"));
        resources.add(createResource(13L, "Turnitin学生指南", "DOCUMENT",
            "国际广泛使用的Turnitin查重系统学生使用指南",
            "https://www.turnitin.com/", "general"));
        resources.add(createResource(14L, "Mendeley文献管理入门", "VIDEO",
            "Mendeley文献管理软件的快速入门教程",
            "https://www.mendeley.com/guides/", "general"));
        resources.add(createResource(15L, "学术论文写作中的引用与参考文献格式（GB/T 7714）", "DOCUMENT",
            "中国国家标准GB/T 7714-2015参考文献著录规则详解",
            "http://www.std.gov.cn/", "general"));
        resources.add(createResource(16L, "如何正确使用AI工具辅助学术写作", "ONLINE",
            "AI辅助写作的边界、标注要求和各高校最新政策汇总",
            "https://example.com/ai-academic-guide", "general"));
        resources.add(createResource(17L, "学位论文写作指南（第4版）", "BOOK",
            "涵盖选题、结构、写作、答辩全流程的学位论文指导书",
            "https://book.douban.com/subject/5678901/", "general"));
        resources.add(createResource(18L, "Python代码引用规范", "DOCUMENT",
            "如何在论文中正确引用开源代码、算法和数据集的指南",
            "https://opensource.guide/", "cs"));
        resources.add(createResource(19L, "学术不端案例警示录", "ONLINE",
            "教育部公布的学术不端典型案例及处理结果，警示学术红线",
            "https://www.moe.gov.cn/", "general"));
        resources.add(createResource(20L, "英文论文写作中常见的抄袭陷阱", "VIDEO",
            "讲解非英语母语写作者最容易忽视的抄袭情景及避免方法",
            "https://www.youtube.com/", "general"));

        return resources;
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