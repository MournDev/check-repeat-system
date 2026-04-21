package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.teacher.dto.StudentGroupDTO;
import com.abin.checkrepeatsystem.teacher.service.TeacherStudentGroupService;
import com.abin.checkrepeatsystem.teacher.vo.StudentGroupVO;
import com.abin.checkrepeatsystem.teacher.vo.StudentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生分组管理控制器
 */
@RestController
@RequestMapping("/api/teacher/student-groups")
public class TeacherStudentGroupController {

    @Autowired
    private TeacherStudentGroupService studentGroupService;

    /**
     * 获取分组列表
     *
     * @return 分组列表
     */
    @GetMapping
    public Result<List<StudentGroupVO>> getGroups() {
        return studentGroupService.getGroups();
    }

    /**
     * 创建分组
     *
     * @param groupDTO 分组DTO
     * @return 创建的分组
     */
    @PostMapping
    public Result<StudentGroupVO> createGroup(@RequestBody StudentGroupDTO groupDTO) {
        return studentGroupService.createGroup(groupDTO);
    }

    /**
     * 更新分组
     *
     * @param groupId 分组ID
     * @param groupDTO 分组DTO
     * @return 更新后的分组
     */
    @PutMapping("/{groupId}")
    public Result<StudentGroupVO> updateGroup(@PathVariable Long groupId, @RequestBody StudentGroupDTO groupDTO) {
        return studentGroupService.updateGroup(groupId, groupDTO);
    }

    /**
     * 删除分组
     *
     * @param groupId 分组ID
     * @return 删除结果
     */
    @DeleteMapping("/{groupId}")
    public Result<Void> deleteGroup(@PathVariable Long groupId) {
        return studentGroupService.deleteGroup(groupId);
    }

    /**
     * 获取不在分组中的学生
     *
     * @param groupId 分组ID
     * @return 学生列表
     */
    @GetMapping("/{groupId}/students/not-in-group")
    public Result<List<StudentVO>> getStudentsNotInGroup(@PathVariable Long groupId) {
        return studentGroupService.getStudentsNotInGroup(groupId);
    }

    /**
     * 添加学生到分组
     *
     * @param groupId 分组ID
     * @param studentIds 学生ID列表
     * @return 添加结果
     */
    @PostMapping("/{groupId}/students")
    public Result<Void> addStudentsToGroup(@PathVariable Long groupId, @RequestBody List<Long> studentIds) {
        return studentGroupService.addStudentsToGroup(groupId, studentIds);
    }

    /**
     * 从分组中移除学生
     *
     * @param groupId 分组ID
     * @param studentId 学生ID
     * @return 移除结果
     */
    @DeleteMapping("/{groupId}/students/{studentId}")
    public Result<Void> removeStudentFromGroup(@PathVariable Long groupId, @PathVariable Long studentId) {
        return studentGroupService.removeStudentFromGroup(groupId, studentId);
    }
}
