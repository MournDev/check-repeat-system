package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.enums.UserTypeEnum;
import com.abin.checkrepeatsystem.mapper.SysRoleMapper;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.AdminInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysRole;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.user.mapper.AdminInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统启动数据初始化器
 * 职责：确保基础角色和超级管理员账号存在
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final AdminInfoMapper adminInfoMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.super-admin.username:superadmin}")
    private String superAdminUsername;

    @Value("${app.init.super-admin.password:}")
    private String superAdminPassword;

    @Value("${app.init.super-admin.real-name:超级管理员}")
    private String superAdminRealName;

    @Value("${app.init.super-admin.email:admin@system.local}")
    private String superAdminEmail;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        initRoles();
        initSuperAdmin();
    }

    /**
     * 初始化基础角色（4个角色，幂等操作）
     */
    private void initRoles() {
        List<Object[]> roles = List.of(
            new Object[]{UserTypeEnum.STUDENT.getCode(), UserTypeEnum.ROLE_STUDENT, "学生", "学生用户，可提交论文、查看查重报告"},
            new Object[]{UserTypeEnum.TEACHER.getCode(), UserTypeEnum.ROLE_TEACHER, "教师", "教师用户，可审核论文、管理学生"},
            new Object[]{UserTypeEnum.ADMIN.getCode(), UserTypeEnum.ROLE_ADMIN, "管理员", "业务管理员，可管理论文库、分配任务、查看数据"},
            new Object[]{UserTypeEnum.SUPER_ADMIN.getCode(), UserTypeEnum.ROLE_SUPER_ADMIN, "超级管理员", "系统管理员，可管理用户、配置系统、查看日志"}
        );

        for (Object[] roleData : roles) {
            String roleCode = (String) roleData[1];
            Long count = sysRoleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleCode, roleCode)
                    .eq(SysRole::getIsDeleted, 0)
            );
            if (count == 0) {
                SysRole role = new SysRole();
                role.setRoleName((String) roleData[2]);
                role.setRoleCode(roleCode);
                role.setPermissions("[]");
                role.setDescription((String) roleData[3]);
                role.setCreateTime(LocalDateTime.now());
                role.setUpdateTime(LocalDateTime.now());
                role.setIsDeleted(0);
                sysRoleMapper.insert(role);
                log.info("[初始化] 创建角色: {} ({})", roleData[2], roleCode);
            }
        }
    }

    /**
     * 初始化超级管理员账号（幂等操作）
     * 仅在系统中没有任何 SUPER_ADMIN 时创建
     */
    private void initSuperAdmin() {
        // 检查是否已存在超级管理员
        Long count = sysUserMapper.selectCount(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, UserTypeEnum.ROLE_SUPER_ADMIN)
                .eq(SysUser::getIsDeleted, 0)
        );
        if (count > 0) {
            return;
        }

        // 安全检查：必须通过环境变量设置密码，禁止使用空密码
        if (superAdminPassword == null || superAdminPassword.isBlank()) {
            log.error("[初始化] 超级管理员密码未设置！请通过环境变量 SUPER_ADMIN_PASSWORD 设置。"
                    + "为安全起见，禁止使用空密码或硬编码默认密码创建超级管理员。");
            return;
        }

        // 查询 SUPER_ADMIN 角色
        SysRole superAdminRole = sysRoleMapper.selectOne(
            new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, UserTypeEnum.ROLE_SUPER_ADMIN)
                .eq(SysRole::getIsDeleted, 0)
        );
        if (superAdminRole == null) {
            log.error("[初始化] SUPER_ADMIN 角色不存在，无法创建超级管理员账号");
            return;
        }

        // 创建超级管理员
        SysUser admin = new SysUser();
        admin.setUsername(superAdminUsername);
        admin.setPassword(passwordEncoder.encode(superAdminPassword));
        admin.setRealName(superAdminRealName);
        admin.setRoleId(superAdminRole.getId());
        admin.setUserType(UserTypeEnum.ROLE_SUPER_ADMIN);
        admin.setEmail(superAdminEmail);
        admin.setStatus(1);
        admin.setEmailVerified(1);
        admin.setIsDeleted(0);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(admin);

        // 创建管理员信息记录
        AdminInfo adminInfo = new AdminInfo();
        adminInfo.setUserId(admin.getId());
        adminInfo.setCreateTime(LocalDateTime.now());
        adminInfo.setUpdateTime(LocalDateTime.now());
        adminInfo.setIsDeleted(0);
        adminInfoMapper.insert(adminInfo);

        log.warn("[初始化] 已创建超级管理员账号: {}，请尽快修改默认密码！", superAdminUsername);
    }
}
