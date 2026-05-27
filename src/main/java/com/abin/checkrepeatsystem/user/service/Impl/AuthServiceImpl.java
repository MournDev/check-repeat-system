package com.abin.checkrepeatsystem.user.service.Impl;


import com.abin.checkrepeatsystem.admin.mapper.SysOperationLogMapper;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.enums.UserTypeEnum;
import com.abin.checkrepeatsystem.common.utils.HttpIpUtils;
import com.abin.checkrepeatsystem.common.utils.IpLocationUtils;
import com.abin.checkrepeatsystem.common.utils.JwtUtils;
import com.abin.checkrepeatsystem.common.utils.UserAgentUtils;
import com.abin.checkrepeatsystem.mapper.SysRoleMapper;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.monitor.service.ApplicationMonitorService;
import com.abin.checkrepeatsystem.pojo.dto.ForgotPasswordReq;
import com.abin.checkrepeatsystem.pojo.dto.LoginReq;
import com.abin.checkrepeatsystem.pojo.dto.RefreshTokenReq;
import com.abin.checkrepeatsystem.pojo.dto.RegisterReq;
import com.abin.checkrepeatsystem.pojo.entity.SysRole;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.pojo.entity.SysLoginLog;
import com.abin.checkrepeatsystem.pojo.entity.StudentInfo;
import com.abin.checkrepeatsystem.pojo.entity.AdminInfo;
import com.abin.checkrepeatsystem.user.mapper.SysLoginLogMapper;
import com.abin.checkrepeatsystem.user.service.AuthService;
import com.abin.checkrepeatsystem.user.service.StudentInfoService;
import com.abin.checkrepeatsystem.user.service.AdminInfoService;
import com.abin.checkrepeatsystem.user.vo.LoginVO;
import com.abin.checkrepeatsystem.user.vo.RefreshTokenVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现类（核心业务逻辑：注册、登录、令牌刷新）
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    // 依赖注入（数据访问层、认证工具、加密工具）
    private final AuthenticationManager authenticationManager;

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final JwtUtils jwtUtils;

    private final PasswordEncoder passwordEncoder;

    private final HttpServletRequest request;

    private final SysLoginLogMapper sysLoginLogMapper;

    private final StudentInfoService studentInfoService;

    private final AdminInfoService adminInfoService;

    private final SysOperationLogMapper sysOperationLogMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;
    private final ApplicationMonitorService monitorService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 用户注册（事务保证：确保用户插入成功，避免脏数据）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "auth.register", description = "用户注册耗时")
    public Result<String> register(RegisterReq registerReq) {
        // 1. 校验用户名唯一性（避免重复注册）
        try {
            SysUser existingUser = sysUserMapper.selectOne(
                    Wrappers.<SysUser>lambdaQuery()
                            .eq(SysUser::getUsername, registerReq.getUsername())
                            .eq(SysUser::getIsDeleted, 0)
            );
            if (existingUser != null) {
                return Result.error(ResultCode.PARAM_ERROR, "用户名已存在");
            }
        } catch (Exception e) {
            log.error("注册-校验用户名唯一性异常", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "系统内部错误，注册失败");
        }

        // 2. 校验角色有效性（避免传入无效角色ID）
        SysRole sysRole = sysRoleMapper.selectById(registerReq.getRoleId());
        if (sysRole == null || sysRole.getIsDeleted() == 1) {
            return Result.error(ResultCode.PARAM_ERROR, "选择的角色无效，无法注册");
        }

        // 3. 构建新用户实体（密码加密存储）
        SysUser newUser = new SysUser();
        newUser.setUsername(registerReq.getUsername());
        newUser.setPassword(passwordEncoder.encode(registerReq.getPassword())); // 密码加密
        newUser.setRealName(registerReq.getRealName());
        newUser.setRoleId(registerReq.getRoleId());
        newUser.setEmail(registerReq.getEmail());
        newUser.setPhone(registerReq.getPhone());
        newUser.setLastLoginTime(null); // 首次注册无登录时间
        newUser.setIsDeleted(0); // 默认为未删除

        // 4. 插入数据库（审计字段由MyBatis-Plus自动填充：createTime、createUserId等）
        sysUserMapper.insert(newUser);
        monitorService.recordBusinessEvent("user_register", "success", 1);
        log.info("用户注册成功：用户名={}，角色={}", registerReq.getUsername(), sysRole.getRoleCode());
        return Result.success("注册成功");
    }

    /**
     * 用户登录（Spring Security认证 + JWT生成）
     */
    @Override
    @Timed(value = "auth.login", description = "用户登录耗时")
    public Result<LoginVO> login(LoginReq loginReq) {
        String loginIp = HttpIpUtils.getRealIp(request);           // IP地址
        String loginDevice = UserAgentUtils.parseDevice(request);   // 设备信息
        String loginLocation = IpLocationUtils.getLocationByIp(loginIp); // 地理位置
        // 1. 执行Spring Security认证（用户名+密码校验）
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginReq.getUsername(),
                            loginReq.getPassword()
                    )
            );
        } catch (Exception e) {
            log.warn("用户登录失败：用户名={}，原因=用户名或密码错误", loginReq.getUsername());
            recordLoginLog(null, loginReq.getUsername(), loginIp, loginLocation,
                    loginDevice, 0, "用户名或密码错误");
            // 记录登录失败操作日志
            try {
                com.abin.checkrepeatsystem.pojo.entity.SysOperationLog operationLog = new com.abin.checkrepeatsystem.pojo.entity.SysOperationLog();
                operationLog.setOperationType("user_login");
                operationLog.setDescription("用户登录失败");
                operationLog.setUserId(null);
                operationLog.setUserName(loginReq.getUsername());
                operationLog.setUserType("unknown");
                operationLog.setTarget("AuthServiceImpl.login");
                operationLog.setStatus(0);
                operationLog.setIpAddress(loginIp);
                operationLog.setRequestParams("{\"username\":\"" + loginReq.getUsername() + "\"}");
                operationLog.setDetails("{\"error\":\"用户名或密码错误\"}");
                operationLog.setOperationTime(LocalDateTime.now());
                operationLog.setCreateTime(LocalDateTime.now());
                operationLog.setUpdateTime(LocalDateTime.now());
                operationLog.setIsDeleted(0);
                sysOperationLogMapper.insert(operationLog);
            } catch (Exception ex) {
                log.warn("记录登录失败操作日志失败", ex);
            }
            monitorService.recordBusinessEvent("user_login", "failure", 1);
            return Result.error(ResultCode.PARAM_ERROR, "用户名或密码错误");
        }

        // 2. 认证通过：设置上下文（后续权限校验会用到）
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. 查询用户完整信息（含基础字段、审计字段）
        SysUser sysUser = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, loginReq.getUsername())
                        .eq(SysUser::getIsDeleted, 0)
        );
        // 兜底：理论上认证通过后用户必存在，避免极端情况（如认证后用户被删除）
        if (sysUser == null) {
            log.error("用户登录异常：用户名={}，认证通过但用户不存在", loginReq.getUsername());
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "登录用户不存在，请联系管理员");
        }

        // 4. 查询用户角色信息（获取角色编码，用于前端权限控制）
        SysRole sysRole = sysRoleMapper.selectById(sysUser.getRoleId());
        if (sysRole == null || sysRole.getIsDeleted() == 1) {
            log.error("用户登录异常：用户名={}，角色ID={}无效", loginReq.getUsername(), sysUser.getRoleId());
            return Result.error(ResultCode.PARAM_ERROR, "用户未分配有效角色，无法登录");
        }
        recordLoginLog(sysUser.getId(), loginReq.getUsername(), loginIp, loginLocation,
                loginDevice, 1, null);
        // 5. 更新用户最后登录时间（审计字段，记录登录轨迹）
        sysUser.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(sysUser);

        // 6. 更新登录日志中的用户ID
        try {
            SysLoginLog loginLog = sysLoginLogMapper.selectOne(
                    Wrappers.<SysLoginLog>lambdaQuery()
                            .eq(SysLoginLog::getUsername, loginReq.getUsername())
                            .last("LIMIT 1")
            );
            if (loginLog != null && loginLog.getUserId() == null) {
                loginLog.setUserId(sysUser.getId());
                sysLoginLogMapper.updateById(loginLog);
            }
        } catch (Exception e) {
            log.warn("更新登录日志用户ID失败：用户名={}", loginReq.getUsername(), e);
        }

        // 6. 生成JWT令牌（携带用户ID、用户名、角色编码）
        String jwtToken = jwtUtils.generateToken(
                sysUser.getId(),
                sysUser.getUsername(),
                sysRole.getRoleCode()
        );

        // 7. 封装登录响应VO（仅返回非敏感信息）
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(jwtToken);
        loginVO.setUserId(sysUser.getId());
        loginVO.setRoleCode(sysRole.getRoleCode());
        loginVO.setUsername(sysUser.getUsername());
        loginVO.setRealName(sysUser.getRealName());
        loginVO.setPhone(sysUser.getPhone());
        loginVO.setEmail(sysUser.getEmail());
        loginVO.setEmailVerified(sysUser.getEmailVerified());
        loginVO.setIntroduce(sysUser.getIntroduce());
        loginVO.setExpireTime(jwtUtils.getExpirationDate(jwtToken).getTime()); // 令牌过期时间
        loginVO.setAvatar(sysUser.getAvatar());
        loginVO.setLastLoginTime(sysUser.getLastLoginTime()); // 最后登录时间
        
        // 如果是学生用户，从StudentInfo表获取学生特有信息
        if (UserTypeEnum.ROLE_STUDENT.equals(sysUser.getUserType())) {
            StudentInfo studentInfo = studentInfoService.getByUserId(sysUser.getId());
            if (studentInfo != null) {
                loginVO.setMajor(studentInfo.getMajor());
                loginVO.setGrade(studentInfo.getGrade());
                loginVO.setClassName(studentInfo.getClassName());
                loginVO.setCollegeName(studentInfo.getCollegeName());
            }
        } else if (UserTypeEnum.ROLE_ADMIN.equals(sysUser.getUserType())) {
            // 如果是管理员用户，从AdminInfo表获取管理员特有信息
            AdminInfo adminInfo = adminInfoService.getByUserId(sysUser.getId());
            if (adminInfo != null) {
                loginVO.setPosition(adminInfo.getPosition());
                loginVO.setDepartment(adminInfo.getDepartment());
                loginVO.setOfficeAddress(adminInfo.getOfficeAddress());
            }
        }

        monitorService.recordBusinessEvent("user_login", "success", 1);
        log.info("用户登录成功：用户名={}，角色={}，令牌过期时间={}",
                loginReq.getUsername(), sysRole.getRoleCode(), loginVO.getExpireTime());
        return Result.success("登录成功", loginVO);
    }

    /**
     * 令牌刷新（避免用户频繁登录，提升体验）
     */
    @Override
    public Result<RefreshTokenVO> refreshToken(RefreshTokenReq refreshTokenReq) {
        String oldToken = refreshTokenReq.getOldToken();

        // 1. 校验旧令牌是否已被注销（黑名单检查）
        if (Boolean.TRUE.equals(redisTemplate.hasKey("token_blacklist:" + oldToken))) {
            log.warn("令牌刷新失败：旧令牌已被注销，token={}", maskToken(oldToken));
            return Result.error(ResultCode.PARAM_VALUE_INVALID, "令牌已失效，请重新登录");
        }

        // 2. 校验旧令牌有效性（格式+是否过期）
        if (!jwtUtils.validateTokenFormat(oldToken)) {
            log.warn("令牌刷新失败：旧令牌格式错误，token={}", maskToken(oldToken));
            return Result.error(ResultCode.PARAM_ERROR, "令牌格式错误，请重新登录");
        }
        if (jwtUtils.isTokenExpired(oldToken)) {
            log.warn("令牌刷新失败：旧令牌已过期，token={}", maskToken(oldToken));
            return Result.error(ResultCode.PARAM_VALUE_INVALID, "旧令牌已过期，请重新登录");
        }

        // 3. 从旧令牌提取用户核心信息（无需查库，减少IO开销）
        Long userId = jwtUtils.extractUserId(oldToken);
        String username = jwtUtils.extractUsername(oldToken);
        String roleCode = jwtUtils.extractRoleCode(oldToken);
        // 兜底：校验令牌中的用户信息是否完整
        if (userId == null || username == null || roleCode == null) {
            log.error("令牌刷新失败：旧令牌信息不完整，token={}", maskToken(oldToken));
            return Result.error(ResultCode.PARAM_ERROR, "令牌信息损坏，请重新登录");
        }

        // 3. 生成新令牌（过期时间重置，保持用户信息一致）
        String newToken = jwtUtils.generateToken(userId, username, roleCode);
        Long newExpireTime = jwtUtils.getExpirationDate(newToken).getTime();

        // 4. 封装刷新响应VO
        RefreshTokenVO refreshTokenVO = new RefreshTokenVO();
        refreshTokenVO.setNewToken(newToken);
        refreshTokenVO.setExpireTime(newExpireTime);

        // 将旧令牌加入黑名单
        try {
            long remainingTtl = jwtUtils.getExpirationDate(oldToken).getTime() - System.currentTimeMillis();
            if (remainingTtl > 0) {
                redisTemplate.opsForValue().set("token_blacklist:" + oldToken, "1",
                    remainingTtl, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn("旧令牌加入黑名单失败", e);
        }

        log.info("令牌刷新成功：用户名={}，旧令牌过期时间={}，新令牌过期时间={}",
                username, jwtUtils.getExpirationDate(oldToken), newExpireTime);
        return Result.success("令牌刷新成功", refreshTokenVO);
    }

    /**
     * 用户退出登录（清除上下文并加入黑名单，前端需同步清除令牌）
     */
    @Override
    public Result<String> logout() {
        // 从当前Security上下文获取JWT令牌并加入黑名单
        String token = null;
        try {
            String headerAuth = request.getHeader("Authorization");
            if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
                token = headerAuth.substring(7);
                long remainingTtl = jwtUtils.getExpirationDate(token).getTime() - System.currentTimeMillis();
                if (remainingTtl > 0) {
                    redisTemplate.opsForValue().set("token_blacklist:" + token, "1",
                        remainingTtl, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
        } catch (Exception e) {
            log.warn("令牌加入黑名单失败", e);
        }
        SecurityContextHolder.clearContext();
        log.info("用户退出登录成功：清除Security上下文，令牌已加入黑名单");
        return Result.success("退出登录成功");
    }

    @Override
    public Result<String> forgotPassword(ForgotPasswordReq forgotPasswordReq) {
        String username = forgotPasswordReq.getUsername();
        String clientIp = HttpIpUtils.getRealIp(request);

        // 1. 频率限制：单IP每小时最多5次重置尝试
        String rateLimitKey = "pwd_reset_limit:" + clientIp;
        Long attemptCount = redisTemplate.opsForValue().increment(rateLimitKey, 1);
        if (attemptCount == 1) {
            redisTemplate.expire(rateLimitKey, 1, java.util.concurrent.TimeUnit.HOURS);
        }
        if (attemptCount != null && attemptCount > 5) {
            log.warn("密码重置频率限制触发: IP={}, username={}", clientIp, username);
            return Result.error(ResultCode.TOO_MANY_REQUESTS, "操作过于频繁，请1小时后再试");
        }

        // 2. 校验验证码
        String storedCode = redisTemplate.opsForValue().get("pwd_reset_code:" + username);
        if (storedCode == null || !storedCode.equals(forgotPasswordReq.getVerificationCode())) {
            return Result.error(ResultCode.PARAM_ERROR, "验证码错误或已过期");
        }

        // 3. 根据用户名查询用户信息
        SysUser sysUser = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getIsDeleted, 0)
        );

        if (sysUser == null) {
            return Result.error(ResultCode.PARAM_ERROR, "用户不存在");
        }

        // 4. 校验邮箱是否匹配
        if (!forgotPasswordReq.getEmail().equals(sysUser.getEmail())) {
            return Result.error(ResultCode.PARAM_ERROR, "邮箱验证失败");
        }

        // 5. 更新密码（加密存储）
        try {
            sysUser.setPassword(passwordEncoder.encode(forgotPasswordReq.getNewPassword()));
            sysUserMapper.updateById(sysUser);
            redisTemplate.delete("pwd_reset_code:" + username);
            log.info("用户密码重置成功：用户名={}", username);
            return Result.success("密码重置成功");
        } catch (Exception e) {
            log.error("用户密码重置失败：用户名={}", username, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "密码重置失败");
        }
    }

    /**
     * 发送密码重置验证码到用户注册邮箱
     */
    public Result<String> sendPasswordResetCode(String username) {
        SysUser sysUser = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getIsDeleted, 0)
        );

        if (sysUser == null) {
            return Result.error(ResultCode.PARAM_ERROR, "用户不存在");
        }

        String email = sysUser.getEmail();
        if (email == null || email.isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "该账户未绑定邮箱");
        }

        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        redisTemplate.opsForValue().set("pwd_reset_code:" + username, code, 300, java.util.concurrent.TimeUnit.SECONDS);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("密码重置验证码");
            message.setText("您的密码重置验证码是：" + code + "，5分钟内有效。如非本人操作，请忽略此邮件。");
            mailSender.send(message);
            log.info("密码重置验证码已发送：用户名={}", username);
            return Result.success("验证码已发送至注册邮箱");
        } catch (Exception e) {
            log.error("发送密码重置验证码失败：用户名={}", username, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "验证码发送失败");
        }
    }

    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) return "***";
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
    /**
     * 记录登录日志
     */
    private void recordLoginLog(Long userId, String username, String loginIp,
                                String loginLocation, String loginDevice,
                                Integer loginStatus, String errorMsg) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUserId(userId);
            loginLog.setUsername(username);
            loginLog.setLoginIp(loginIp);
            loginLog.setLoginLocation(loginLocation);
            loginLog.setLoginDevice(loginDevice);
            loginLog.setLoginTime(LocalDateTime.now());
            loginLog.setLoginResult(loginStatus);
            loginLog.setFailReason(errorMsg);
            sysLoginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志失败：用户名={}", username, e);
        }
    }

}
