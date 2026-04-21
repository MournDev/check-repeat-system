package com.abin.checkrepeatsystem.teacher.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.pojo.entity.TeacherInfo;
import com.abin.checkrepeatsystem.teacher.dto.UpdateTeacherInfoReq;
import com.abin.checkrepeatsystem.teacher.service.TeacherInfoService;
import com.abin.checkrepeatsystem.user.mapper.TeacherInfoMapper;
import com.abin.checkrepeatsystem.user.service.Impl.UserQueryService;
import com.abin.checkrepeatsystem.user.mapper.ConversationMemberMapper;
import com.abin.checkrepeatsystem.pojo.entity.ConversationMember;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class TeacherInfoServiceImpl extends ServiceImpl<TeacherInfoMapper, TeacherInfo> implements TeacherInfoService{

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private TeacherInfoMapper teacherInfoMapper;

    @Resource
    private PasswordEncoder passwordEncoder;
    
    @Resource
    private ConversationMemberMapper conversationMemberMapper;
    @Override
    public Result<String> updateInfo(UpdateTeacherInfoReq updateReq){
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null|| !authentication.isAuthenticated()){
                return Result.error(ResultCode.NOT_LOGIN, "用户未登录");
            }
            String currentUsername = authentication.getName();
            SysUser currentUser = sysUserMapper.selectOne(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getUsername, currentUsername)
                            .eq(SysUser::getIsDeleted, 0)
            );

            if (currentUser == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "当前用户不存在");
            }
            // 更新SysUser表中的通用字段
            currentUser.setRealName(updateReq.getRealName());
            currentUser.setCollegeId(updateReq.getCollegeId());
            currentUser.setIntroduce(updateReq.getIntroduce());
            currentUser.setPhone(updateReq.getPhone());
            currentUser.setEmail(updateReq.getEmail());
            sysUserMapper.updateById(currentUser);

            // 更新TeacherInfo表中的教师特定字段
            TeacherInfo teacherInfo = teacherInfoMapper.selectOne(
                    Wrappers.<TeacherInfo>lambdaQuery()
                            .eq(TeacherInfo::getUserId, updateReq.getUserId())
                            .eq(TeacherInfo::getIsDeleted, 0)
            );
            if (teacherInfo == null) {
                teacherInfo = new TeacherInfo();
                teacherInfo.setUserId(updateReq.getUserId());
            }
            teacherInfo.setProfessionalTitle(updateReq.getTitle());
            teacherInfo.setResearchDirection(String.join(",", updateReq.getResearchFields()));
            teacherInfo.setOffice(updateReq.getOffice());
            teacherInfo.setOfficeHours(updateReq.getOfficeHours());
            teacherInfo.setMaxReviewCount(updateReq.getMaxReviewCount());
            teacherInfo.setReviewDeadline(updateReq.getReviewDeadline());
            
            if (teacherInfo.getId() == null) {
                teacherInfoMapper.insert(teacherInfo);
            } else {
                teacherInfoMapper.updateById(teacherInfo);
            }

            log.info("用户信息更新成功：用户名={}", currentUsername);
            return Result.success("用户信息更新成功");
        } catch (Exception e) {
            log.error("用户信息更新失败：{}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "用户信息更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result<UpdateTeacherInfoReq> getInfoByUserId(Long userId) {
        try {
            // 根据用户ID查询用户信息
            SysUser user = sysUserMapper.selectOne(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getId, userId)
                            .eq(SysUser::getIsDeleted, 0)
            );

            if (user == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "用户不存在");
            }

            // 查询教师特定信息
            TeacherInfo teacherInfo = teacherInfoMapper.selectOne(
                    Wrappers.<TeacherInfo>lambdaQuery()
                            .eq(TeacherInfo::getUserId, userId)
                            .eq(TeacherInfo::getIsDeleted, 0)
            );

            // 将SysUser和TeacherInfo实体转换为UpdateTeacherInfoReq DTO
            UpdateTeacherInfoReq info = new UpdateTeacherInfoReq();
            info.setUserId(userId);
            info.setRealName(user.getRealName());
            info.setUsername(user.getUsername());
            info.setCollegeId(user.getCollegeId());
            info.setIntroduce(user.getIntroduce());
            info.setPhone(user.getPhone());
            info.setEmail(user.getEmail());

            // 从TeacherInfo中获取教师特定字段
            if (teacherInfo != null) {
                info.setTitle(teacherInfo.getProfessionalTitle());
                // 将研究方向字符串转换为列表
                if (teacherInfo.getResearchDirection() != null && !teacherInfo.getResearchDirection().isEmpty()) {
                    List<String> researchFields = Arrays.asList(teacherInfo.getResearchDirection().split(","));
                    info.setResearchFields(researchFields);
                } else {
                    info.setResearchFields(new ArrayList<>());
                }
                info.setOffice(teacherInfo.getOffice());
                info.setOfficeHours(teacherInfo.getOfficeHours());
                info.setMaxReviewCount(teacherInfo.getMaxReviewCount());
                info.setReviewDeadline(teacherInfo.getReviewDeadline());
            } else {
                info.setTitle("");
                info.setResearchFields(new ArrayList<>());
                info.setOffice("");
                info.setOfficeHours("");
                info.setMaxReviewCount(5); // 默认值
                info.setReviewDeadline(7); // 默认值
            }

            log.info("获取用户信息成功：用户ID={}", userId);
            return Result.success("获取用户信息成功", info);
        } catch (Exception e) {
            log.error("获取用户信息失败：用户ID={}, 错误信息={}", userId, e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取用户信息失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> changePasswordByUserId(Long userId, String newPassword) {
        if (userId == null || newPassword == null){
            return Result.error(ResultCode.PARAM_ERROR, "用户ID或密码不能为空");
        }
        
        // 添加密码长度验证
        if (newPassword.length() < 6 || newPassword.length() > 50) {
            return Result.error(ResultCode.PARAM_ERROR, "密码长度必须在6-50位之间");
        }
        
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        String encodePassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodePassword);
        sysUserMapper.updateById(user);
        log.info("用户密码修改成功：用户ID={}", userId);
        return Result.success("密码修改成功");
    }

}
