package com.abin.checkrepeatsystem.teacher.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.pojo.entity.StudentGroup;
import com.abin.checkrepeatsystem.pojo.entity.StudentGroupStudentRel;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.pojo.entity.StudentInfo;
import com.abin.checkrepeatsystem.teacher.dto.StudentGroupDTO;
import com.abin.checkrepeatsystem.teacher.mapper.StudentGroupMapper;
import com.abin.checkrepeatsystem.teacher.mapper.StudentGroupStudentRelMapper;
import com.abin.checkrepeatsystem.teacher.service.TeacherStudentGroupService;
import com.abin.checkrepeatsystem.teacher.vo.StudentGroupVO;
import com.abin.checkrepeatsystem.teacher.vo.StudentVO;
import com.abin.checkrepeatsystem.user.service.StudentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生分组管理服务实现
 */
@Service
public class TeacherStudentGroupServiceImpl implements TeacherStudentGroupService {

    @Autowired
    private StudentGroupMapper studentGroupMapper;

    @Autowired
    private StudentGroupStudentRelMapper studentGroupStudentRelMapper;

    @Autowired
    private BaseMapper<SysUser> sysUserMapper;

    @Resource
    private StudentInfoService studentInfoService;

    /**
     * 获取分组列表
     *
     * @return 分组列表
     */
    @Override
    public Result<List<StudentGroupVO>> getGroups() {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            
            // 查询分组列表
            LambdaQueryWrapper<StudentGroup> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(StudentGroup::getCreateBy, userId);
            List<StudentGroup> groups = studentGroupMapper.selectList(queryWrapper);
            
            // 转换为VO
            List<StudentGroupVO> groupVOs = new ArrayList<>();
            for (StudentGroup group : groups) {
                StudentGroupVO vo = new StudentGroupVO();
                BeanUtils.copyProperties(group, vo);
                
                // 查询分组中的学生
                List<StudentVO> students = getStudentsInGroup(group.getId());
                vo.setStudents(students);
                vo.setStudentCount(students.size());
                
                groupVOs.add(vo);
            }
            
            return Result.success(groupVOs);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"获取分组列表失败");
        }
    }

    /**
     * 创建分组
     *
     * @param groupDTO 分组DTO
     * @return 创建的分组
     */
    @Override
    public Result<StudentGroupVO> createGroup(StudentGroupDTO groupDTO) {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            
            // 创建分组实体
            StudentGroup group = new StudentGroup();
            BeanUtils.copyProperties(groupDTO, group);
            group.setCreateTime(LocalDateTime.now());
            group.setUpdateTime(LocalDateTime.now());
            group.setCreateBy(userId);
            group.setUpdateBy(userId);
            
            // 保存分组
            studentGroupMapper.insert(group);
            
            // 转换为VO
            StudentGroupVO vo = new StudentGroupVO();
            BeanUtils.copyProperties(group, vo);
            vo.setStudents(new ArrayList<>());
            vo.setStudentCount(0);
            
            return Result.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"创建分组失败");
        }
    }

    /**
     * 更新分组
     *
     * @param groupId 分组ID
     * @param groupDTO 分组DTO
     * @return 更新后的分组
     */
    @Override
    public Result<StudentGroupVO> updateGroup(Long groupId, StudentGroupDTO groupDTO) {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            
            // 查询分组
            StudentGroup group = studentGroupMapper.selectById(groupId);
            if (group == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND,"分组不存在");
            }
            
            // 检查权限
            if (!group.getCreateBy().equals(userId)) {
                return Result.error(ResultCode.SYSTEM_ERROR,"无权限修改此分组");
            }
            
            // 更新分组
            BeanUtils.copyProperties(groupDTO, group);
            group.setUpdateTime(LocalDateTime.now());
            group.setUpdateBy(userId);
            
            // 保存分组
            studentGroupMapper.updateById(group);
            
            // 转换为VO
            StudentGroupVO vo = new StudentGroupVO();
            BeanUtils.copyProperties(group, vo);
            
            // 查询分组中的学生
            List<StudentVO> students = getStudentsInGroup(group.getId());
            vo.setStudents(students);
            vo.setStudentCount(students.size());
            
            return Result.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"更新分组失败");
        }
    }

    /**
     * 删除分组
     *
     * @param groupId 分组ID
     * @return 删除结果
     */
    @Override
    public Result<Void> deleteGroup(Long groupId) {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            
            // 查询分组
            StudentGroup group = studentGroupMapper.selectById(groupId);
            if (group == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND,"分组不存在");
            }
            
            // 检查权限
            if (!group.getCreateBy().equals(userId)) {
                return Result.error(ResultCode.SYSTEM_ERROR,"无权限删除此分组");
            }
            
            // 删除分组与学生的关系
            LambdaQueryWrapper<StudentGroupStudentRel> relQueryWrapper = new LambdaQueryWrapper<>();
            relQueryWrapper.eq(StudentGroupStudentRel::getGroupId, groupId);
            studentGroupStudentRelMapper.delete(relQueryWrapper);
            
            // 删除分组
            studentGroupMapper.deleteById(groupId);
            
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"删除分组失败");
        }
    }

    /**
     * 获取不在分组中的学生
     *
     * @param groupId 分组ID
     * @return 学生列表
     */
    @Override
    public Result<List<StudentVO>> getStudentsNotInGroup(Long groupId) {
        try {
            // 查询分组中的学生ID
            LambdaQueryWrapper<StudentGroupStudentRel> relQueryWrapper = new LambdaQueryWrapper<>();
            relQueryWrapper.eq(StudentGroupStudentRel::getGroupId, groupId);
            List<StudentGroupStudentRel> rels = studentGroupStudentRelMapper.selectList(relQueryWrapper);
            
            List<Long> studentIdsInGroup = new ArrayList<>();
            for (StudentGroupStudentRel rel : rels) {
                studentIdsInGroup.add(rel.getStudentId());
            }
            
            // 查询所有学生
            LambdaQueryWrapper<SysUser> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(SysUser::getUserType, "student");
            if (!studentIdsInGroup.isEmpty()) {
                userQueryWrapper.notIn(SysUser::getId, studentIdsInGroup);
            }
            List<SysUser> students = sysUserMapper.selectList(userQueryWrapper);
            
            // 转换为VO
            List<StudentVO> studentVOs = new ArrayList<>();
            for (SysUser student : students) {
                StudentVO vo = new StudentVO();
                vo.setId(student.getId());
                vo.setName(student.getRealName());
                vo.setStudentId(student.getUsername());
                
                // 从StudentInfo表获取学生信息
                StudentInfo studentInfo = studentInfoService.getByUserId(student.getId());
                if (studentInfo != null) {
                    vo.setCollegeName(studentInfo.getCollegeName());
                    vo.setMajorName(studentInfo.getMajor());
                }
                
                studentVOs.add(vo);
            }
            
            return Result.success(studentVOs);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"获取学生列表失败");
        }
    }

    /**
     * 添加学生到分组
     *
     * @param groupId 分组ID
     * @param studentIds 学生ID列表
     * @return 添加结果
     */
    @Override
    public Result<Void> addStudentsToGroup(Long groupId, List<Long> studentIds) {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            
            // 检查分组是否存在
            StudentGroup group = studentGroupMapper.selectById(groupId);
            if (group == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND,"分组不存在");
            }
            
            // 检查权限
            if (!group.getCreateBy().equals(userId)) {
                return Result.error(ResultCode.SYSTEM_ERROR,"无权限修改此分组");
            }
            
            // 添加学生到分组
            for (Long studentId : studentIds) {
                // 检查学生是否已在分组中
                LambdaQueryWrapper<StudentGroupStudentRel> relQueryWrapper = new LambdaQueryWrapper<>();
                relQueryWrapper.eq(StudentGroupStudentRel::getGroupId, groupId)
                        .eq(StudentGroupStudentRel::getStudentId, studentId);
                if (studentGroupStudentRelMapper.selectOne(relQueryWrapper) == null) {
                    StudentGroupStudentRel rel = new StudentGroupStudentRel();
                    rel.setGroupId(groupId);
                    rel.setStudentId(studentId);
                    rel.setCreateTime(LocalDateTime.now());
                    rel.setCreateBy(userId);
                    studentGroupStudentRelMapper.insert(rel);
                }
            }
            
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"添加学生失败");
        }
    }

    /**
     * 从分组中移除学生
     *
     * @param groupId 分组ID
     * @param studentId 学生ID
     * @return 移除结果
     */
    @Override
    public Result<Void> removeStudentFromGroup(Long groupId, Long studentId) {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            
            // 检查分组是否存在
            StudentGroup group = studentGroupMapper.selectById(groupId);
            if (group == null) {
                return Result.error(ResultCode.SYSTEM_ERROR,"分组不存在");
            }
            
            // 检查权限
            if (!group.getCreateBy().equals(userId)) {
                return Result.error(ResultCode.SYSTEM_ERROR,"无权限修改此分组");
            }
            
            // 移除学生
            LambdaQueryWrapper<StudentGroupStudentRel> relQueryWrapper = new LambdaQueryWrapper<>();
            relQueryWrapper.eq(StudentGroupStudentRel::getGroupId, groupId)
                    .eq(StudentGroupStudentRel::getStudentId, studentId);
            studentGroupStudentRelMapper.delete(relQueryWrapper);
            
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"移除学生失败");
        }
    }

    /**
     * 获取分组中的学生
     *
     * @param groupId 分组ID
     * @return 学生列表
     */
    private List<StudentVO> getStudentsInGroup(Long groupId) {
        // 查询分组中的学生ID
        LambdaQueryWrapper<StudentGroupStudentRel> relQueryWrapper = new LambdaQueryWrapper<>();
        relQueryWrapper.eq(StudentGroupStudentRel::getGroupId, groupId);
        List<StudentGroupStudentRel> rels = studentGroupStudentRelMapper.selectList(relQueryWrapper);
        
        List<Long> studentIds = new ArrayList<>();
        for (StudentGroupStudentRel rel : rels) {
            studentIds.add(rel.getStudentId());
        }
        
        // 查询学生信息
        List<StudentVO> students = new ArrayList<>();
        if (!studentIds.isEmpty()) {
            LambdaQueryWrapper<SysUser> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.in(SysUser::getId, studentIds);
            List<SysUser> userList = sysUserMapper.selectList(userQueryWrapper);
            
            for (SysUser user : userList) {
                StudentVO vo = new StudentVO();
                vo.setId(user.getId());
                vo.setName(user.getRealName());
                vo.setStudentId(user.getUsername());
                
                // 从StudentInfo表获取学生信息
                StudentInfo studentInfo = studentInfoService.getByUserId(user.getId());
                if (studentInfo != null) {
                    vo.setCollegeName(studentInfo.getCollegeName());
                    vo.setMajorName(studentInfo.getMajor());
                }
                
                students.add(vo);
            }
        }
        
        return students;
    }
}
