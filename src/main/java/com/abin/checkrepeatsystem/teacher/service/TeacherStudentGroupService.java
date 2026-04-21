package com.abin.checkrepeatsystem.teacher.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.teacher.dto.StudentGroupDTO;
import com.abin.checkrepeatsystem.teacher.vo.StudentGroupVO;
import com.abin.checkrepeatsystem.teacher.vo.StudentVO;

import java.util.List;

/**
 * 学生分组管理服务接口
 */
public interface TeacherStudentGroupService {

    /**
     * 获取分组列表
     *
     * @return 分组列表
     */
    Result<List<StudentGroupVO>> getGroups();

    /**
     * 创建分组
     *
     * @param groupDTO 分组DTO
     * @return 创建的分组
     */
    Result<StudentGroupVO> createGroup(StudentGroupDTO groupDTO);

    /**
     * 更新分组
     *
     * @param groupId 分组ID
     * @param groupDTO 分组DTO
     * @return 更新后的分组
     */
    Result<StudentGroupVO> updateGroup(Long groupId, StudentGroupDTO groupDTO);

    /**
     * 删除分组
     *
     * @param groupId 分组ID
     * @return 删除结果
     */
    Result<Void> deleteGroup(Long groupId);

    /**
     * 获取不在分组中的学生
     *
     * @param groupId 分组ID
     * @return 学生列表
     */
    Result<List<StudentVO>> getStudentsNotInGroup(Long groupId);

    /**
     * 添加学生到分组
     *
     * @param groupId 分组ID
     * @param studentIds 学生ID列表
     * @return 添加结果
     */
    Result<Void> addStudentsToGroup(Long groupId, List<Long> studentIds);

    /**
     * 从分组中移除学生
     *
     * @param groupId 分组ID
     * @param studentId 学生ID
     * @return 移除结果
     */
    Result<Void> removeStudentFromGroup(Long groupId, Long studentId);
}
