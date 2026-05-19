package com.abin.checkrepeatsystem.common.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.College;
import com.abin.checkrepeatsystem.pojo.entity.Major;

import java.util.List;
import java.util.Map;

/**
 * 学院和专业统一服务接口
 * 提供高可用的学院和专业数据管理，支持缓存、分页、统一返回格式
 */
public interface CollegeAndMajorService {

    /**
     * 获取所有学院列表（带缓存）
     * @return 学院列表
     */
    Result<List<College>> getAllColleges();

    /**
     * 获取学院分页列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param searchKey 搜索关键字（可选）
     * @return 学院分页列表
     */
    Result<Map<String, Object>> getCollegePage(int page, int pageSize, String searchKey);

    /**
     * 根据学院ID获取专业列表（带缓存）
     * @param collegeId 学院ID
     * @return 专业列表
     */
    Result<List<Major>> getMajorsByCollegeId(Long collegeId);

    /**
     * 获取所有专业列表（带缓存）
     * @return 专业列表
     */
    Result<List<Major>> getAllMajors();

    /**
     * 获取专业分页列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param collegeId 学院ID（可选，用于筛选）
     * @param searchKey 搜索关键字（可选）
     * @return 专业分页列表
     */
    Result<Map<String, Object>> getMajorPage(int page, int pageSize, Long collegeId, String searchKey);

    /**
     * 获取学院和专业的完整树形结构
     * @return 树形结构数据
     */
    Result<List<Map<String, Object>>> getCollegeMajorTree();

    /**
     * 根据学院名称获取学院信息
     * @param collegeName 学院名称
     * @return 学院信息
     */
    Result<College> getCollegeByName(String collegeName);

    /**
     * 根据专业名称获取专业信息
     * @param majorName 专业名称
     * @return 专业信息
     */
    Result<Major> getMajorByName(String majorName);

    /**
     * 验证学院ID是否有效
     * @param collegeId 学院ID
     * @return 是否有效
     */
    boolean isValidCollegeId(Long collegeId);

    /**
     * 验证专业ID是否有效
     * @param majorId 专业ID
     * @return 是否有效
     */
    boolean isValidMajorId(Long majorId);

    /**
     * 获取学院ID到名称的映射
     * @return 映射关系
     */
    Result<Map<Long, String>> getCollegeIdNameMap();

    /**
     * 获取专业ID到名称的映射
     * @return 映射关系
     */
    Result<Map<Long, String>> getMajorIdNameMap();

    /**
     * 清除学院缓存
     */
    void clearCollegeCache();

    /**
     * 清除专业缓存
     */
    void clearMajorCache();

    /**
     * 清除所有缓存
     */
    void clearAllCache();
}