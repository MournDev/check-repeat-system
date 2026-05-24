package com.abin.checkrepeatsystem.common.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.service.CollegeAndMajorService;
import com.abin.checkrepeatsystem.pojo.entity.College;
import com.abin.checkrepeatsystem.pojo.entity.Major;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学院和专业统一控制器
 * 提供高可用的学院和专业数据接口，支持缓存、分页、统一返回格式
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/college-major")
@Tag(name = "学院和专业接口", description = "学院和专业数据统一管理接口")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CollegeAndMajorController {

    private final CollegeAndMajorService collegeAndMajorService;

    @GetMapping("/colleges")
    @Operation(summary = "获取所有学院列表", description = "获取所有未删除的学院列表，带缓存支持")
    public Result<List<College>> getAllColleges() {
        log.info("接收获取所有学院列表请求");
        return collegeAndMajorService.getAllColleges();
    }

    @GetMapping("/colleges/page")
    @Operation(summary = "获取学院分页列表", description = "分页获取学院列表，支持关键字搜索")
    public Result<Map<String, Object>> getCollegePage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String searchKey) {
        log.info("接收获取学院分页列表请求: page={}, pageSize={}, searchKey={}", page, pageSize, searchKey);
        return collegeAndMajorService.getCollegePage(page, pageSize, searchKey);
    }

    @GetMapping("/majors")
    @Operation(summary = "获取所有专业列表", description = "获取所有未删除的专业列表，带缓存支持")
    public Result<List<Major>> getAllMajors() {
        log.info("接收获取所有专业列表请求");
        return collegeAndMajorService.getAllMajors();
    }

    @GetMapping("/majors/college/{collegeId}")
    @Operation(summary = "根据学院ID获取专业列表", description = "获取指定学院下的所有专业，带缓存支持")
    public Result<List<Major>> getMajorsByCollegeId(
            @Parameter(description = "学院ID") @PathVariable Long collegeId) {
        log.info("接收获取专业列表请求: collegeId={}", collegeId);
        return collegeAndMajorService.getMajorsByCollegeId(collegeId);
    }

    @GetMapping("/majors/page")
    @Operation(summary = "获取专业分页列表", description = "分页获取专业列表，支持学院ID筛选和关键字搜索")
    public Result<Map<String, Object>> getMajorPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "学院ID") @RequestParam(required = false) Long collegeId,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String searchKey) {
        log.info("接收获取专业分页列表请求: page={}, pageSize={}, collegeId={}, searchKey={}",
                page, pageSize, collegeId, searchKey);
        return collegeAndMajorService.getMajorPage(page, pageSize, collegeId, searchKey);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取学院专业树形结构", description = "获取完整的学院和专业树形结构，带缓存支持")
    public Result<List<Map<String, Object>>> getCollegeMajorTree() {
        log.info("接收获取学院专业树形结构请求");
        return collegeAndMajorService.getCollegeMajorTree();
    }

    @GetMapping("/college/name")
    @Operation(summary = "根据学院名称获取学院信息", description = "通过学院名称精确查询学院信息")
    public Result<College> getCollegeByName(
            @Parameter(description = "学院名称") @RequestParam String collegeName) {
        log.info("接收根据学院名称获取学院信息请求: collegeName={}", collegeName);
        return collegeAndMajorService.getCollegeByName(collegeName);
    }

    @GetMapping("/major/name")
    @Operation(summary = "根据专业名称获取专业信息", description = "通过专业名称精确查询专业信息")
    public Result<Major> getMajorByName(
            @Parameter(description = "专业名称") @RequestParam String majorName) {
        log.info("接收根据专业名称获取专业信息请求: majorName={}", majorName);
        return collegeAndMajorService.getMajorByName(majorName);
    }

    @GetMapping("/college/map")
    @Operation(summary = "获取学院ID名称映射", description = "获取学院ID到名称的映射关系，带缓存支持")
    public Result<Map<Long, String>> getCollegeIdNameMap() {
        log.info("接收获取学院ID名称映射请求");
        return collegeAndMajorService.getCollegeIdNameMap();
    }

    @GetMapping("/major/map")
    @Operation(summary = "获取专业ID名称映射", description = "获取专业ID到名称的映射关系，带缓存支持")
    public Result<Map<Long, String>> getMajorIdNameMap() {
        log.info("接收获取专业ID名称映射请求");
        return collegeAndMajorService.getMajorIdNameMap();
    }

    @GetMapping("/validate/college/{collegeId}")
    @Operation(summary = "验证学院ID是否有效", description = "检查学院ID是否存在且未被删除")
    public Result<Boolean> validateCollegeId(
            @Parameter(description = "学院ID") @PathVariable Long collegeId) {
        log.info("接收验证学院ID请求: collegeId={}", collegeId);
        boolean isValid = collegeAndMajorService.isValidCollegeId(collegeId);
        return Result.success("验证完成", isValid);
    }

    @GetMapping("/validate/major/{majorId}")
    @Operation(summary = "验证专业ID是否有效", description = "检查专业ID是否存在且未被删除")
    public Result<Boolean> validateMajorId(
            @Parameter(description = "专业ID") @PathVariable Long majorId) {
        log.info("接收验证专业ID请求: majorId={}", majorId);
        boolean isValid = collegeAndMajorService.isValidMajorId(majorId);
        return Result.success("验证完成", isValid);
    }

    @DeleteMapping("/cache/college")
    @Operation(summary = "清除学院缓存", description = "清除学院相关的所有缓存数据")
    public Result<Void> clearCollegeCache() {
        log.info("接收清除学院缓存请求");
        collegeAndMajorService.clearCollegeCache();
        return Result.success("学院缓存已清除", null);
    }

    @DeleteMapping("/cache/major")
    @Operation(summary = "清除专业缓存", description = "清除专业相关的所有缓存数据")
    public Result<Void> clearMajorCache() {
        log.info("接收清除专业缓存请求");
        collegeAndMajorService.clearMajorCache();
        return Result.success("专业缓存已清除", null);
    }

    @DeleteMapping("/cache/all")
    @Operation(summary = "清除所有缓存", description = "清除学院和专业相关的所有缓存数据")
    public Result<Void> clearAllCache() {
        log.info("接收清除所有缓存请求");
        collegeAndMajorService.clearAllCache();
        return Result.success("所有缓存已清除", null);
    }
}