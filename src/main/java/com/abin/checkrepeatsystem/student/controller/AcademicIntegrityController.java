package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.student.dto.AcademicResourceDTO;
import com.abin.checkrepeatsystem.student.dto.ChecklistItemDTO;
import com.abin.checkrepeatsystem.student.dto.PersonalAdviceDTO;
import com.abin.checkrepeatsystem.student.service.AcademicIntegrityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学术诚信控制器
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/student/academic-integrity")
@Api(tags = "学生学术诚信管理")
public class AcademicIntegrityController {
    
    private final AcademicIntegrityService academicIntegrityService;
    
    /**
     * 获取个性化学术建议
     */
    @GetMapping("/personal-advice")
    @ApiOperation("获取个性化学术建议")
    public Result<PersonalAdviceDTO> getPersonalAdvice() {
        Long studentId = UserBusinessInfoUtils.getCurrentUserId();
        PersonalAdviceDTO advice = academicIntegrityService.getPersonalAdvice(studentId);
        return Result.success("获取个性化学术建议成功", advice);
    }
    
    /**
     * 获取推荐学习资源
     */
    @GetMapping("/resources")
    @ApiOperation("获取推荐学习资源")
    public Result<List<AcademicResourceDTO>> getRecommendedResources(
            @RequestParam(required = false) String resourceType) {
        Long studentId = UserBusinessInfoUtils.getCurrentUserId();
        List<AcademicResourceDTO> resources = academicIntegrityService.getRecommendedResources(studentId, resourceType);
        return Result.success("获取推荐学习资源成功", resources);
    }
    
    /**
     * 获取用户检查清单
     */
    @GetMapping("/checklist")
    @ApiOperation("获取用户检查清单")
    public Result<List<ChecklistItemDTO>> getChecklist() {
        Long studentId = UserBusinessInfoUtils.getCurrentUserId();
        List<ChecklistItemDTO> checklist = academicIntegrityService.getChecklist(studentId);
        return Result.success("获取用户检查清单成功", checklist);
    }
    
    /**
     * 更新检查项状态
     */
    @PutMapping("/checklist/{itemId}")
    @ApiOperation("更新检查项状态")
    public Result<String> updateChecklistItem(
            @PathVariable Long itemId,
            @RequestBody ChecklistItemDTO updateRequest) {
        Long studentId = UserBusinessInfoUtils.getCurrentUserId();
        boolean result = academicIntegrityService.updateChecklistItem(studentId, itemId, updateRequest.getChecked());
        if (result) {
            return Result.success("更新检查项状态成功");
        } else {
            return Result.error(500, "更新检查项状态失败");
        }
    }
    
    /**
     * 初始化用户检查清单
     */
    @PostMapping("/checklist/init")
    @ApiOperation("初始化用户检查清单")
    public Result<String> initializeChecklist() {
        Long studentId = UserBusinessInfoUtils.getCurrentUserId();
        academicIntegrityService.initializeChecklist(studentId);
        return Result.success("初始化用户检查清单成功");
    }
}