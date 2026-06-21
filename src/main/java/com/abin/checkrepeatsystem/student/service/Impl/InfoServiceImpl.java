package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.enums.UserTypeEnum;
import com.abin.checkrepeatsystem.mapper.SysRoleMapper;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.dto.UpdatePasswordReq;
import com.abin.checkrepeatsystem.pojo.entity.SysLoginLog;
import com.abin.checkrepeatsystem.pojo.entity.SysRole;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.pojo.entity.StudentInfo;
import com.abin.checkrepeatsystem.student.dto.UpdateEmailReq;
import com.abin.checkrepeatsystem.student.service.InfoService;
import com.abin.checkrepeatsystem.student.vo.LoginHistoryVO;
import com.abin.checkrepeatsystem.student.vo.LoginLogQueryReq;
import com.abin.checkrepeatsystem.user.dto.UpdateUserInfoReq;
import com.abin.checkrepeatsystem.user.mapper.SysLoginLogMapper;
import com.abin.checkrepeatsystem.user.service.StudentInfoService;
import com.abin.checkrepeatsystem.common.service.TokenRevocationService;
import com.abin.checkrepeatsystem.user.vo.LoginVO;
import com.abin.checkrepeatsystem.user.mapper.ConversationMemberMapper;
import com.abin.checkrepeatsystem.pojo.entity.ConversationMember;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class InfoServiceImpl implements InfoService {
    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final MinioClient minioClient;

    @Value("${minio.bucket.avatar:avatar}")
    private String avatarBucket;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:}")
    private String frontendBaseUrl;


    private final SysLoginLogMapper sysLoginLogMapper;

    private final PasswordEncoder passwordEncoder;

    private final RedisTemplate<String, String> redisTemplate;

    private final JavaMailSender mailSender;

    private final StudentInfoService studentInfoService;
    
    private final ConversationMemberMapper conversationMemberMapper;

    private final TokenRevocationService tokenRevocationService;

    @Override
    public Result<LoginVO> getCurrentUserInfo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
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

            SysRole sysRole = sysRoleMapper.selectById(currentUser.getRoleId());
            if (sysRole == null || sysRole.getIsDeleted() == 1) {
                return Result.error(ResultCode.PARAM_ERROR, "用户角色无效");
            }

            LoginVO loginVO = new LoginVO();
            loginVO.setUserId(currentUser.getId());
            loginVO.setRoleCode(sysRole.getRoleCode());
            loginVO.setUsername(currentUser.getUsername());
            loginVO.setRealName(currentUser.getRealName());
            loginVO.setPhone(currentUser.getPhone());
            loginVO.setEmail(currentUser.getEmail());
            loginVO.setEmailVerified(currentUser.getEmailVerified());
            loginVO.setIntroduce(currentUser.getIntroduce());
            loginVO.setAvatar(currentUser.getAvatar());
            loginVO.setLastLoginTime(currentUser.getLastLoginTime());

            // 学生信息
            StudentInfo studentInfo = studentInfoService.getByUserId(currentUser.getId());
            if (studentInfo != null) {
                loginVO.setCollegeId(studentInfo.getCollegeId());
                loginVO.setCollegeName(studentInfo.getCollegeName());
                loginVO.setMajorId(studentInfo.getMajorId());
                loginVO.setMajor(studentInfo.getMajor());
                loginVO.setGrade(studentInfo.getGrade());
                loginVO.setClassName(studentInfo.getClassName());
            }

            return Result.success("获取成功", loginVO);
        } catch (Exception e) {
            log.error("获取用户信息失败：{}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取用户信息失败：" + e.getMessage());
        }
    }

    @Override
    public Result<LoginVO> updateUserInfo(UpdateUserInfoReq updateReq) {
        try {
            // 1. 获取当前登录用户 ID（从 Security 上下文获取）
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
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

            // 2. 更新用户信息（仅允许更新部分字段）
            currentUser.setCollegeId(updateReq.getCollegeId());
            currentUser.setRealName(updateReq.getRealName());
            currentUser.setEmail(updateReq.getEmail());
            currentUser.setPhone(updateReq.getPhone());
            currentUser.setIntroduce(updateReq.getIntroduce());

            sysUserMapper.updateById(currentUser);
            
            // 3. 更新学生信息
            StudentInfo studentInfo = studentInfoService.getByUserId(currentUser.getId());
            if (studentInfo != null) {
                studentInfo.setCollegeId(updateReq.getCollegeId());
                studentInfo.setCollegeName(updateReq.getCollegeName());
                studentInfo.setMajorId(updateReq.getMajorId());
                studentInfo.setMajor(updateReq.getMajor());
                studentInfo.setGrade(updateReq.getGrade());
                studentInfo.setClassName(updateReq.getClassName());
                studentInfoService.saveOrUpdate(studentInfo);
            }

            // 3. 查询角色信息用于构建 LoginVO
            SysRole sysRole = sysRoleMapper.selectById(currentUser.getRoleId());
            if (sysRole == null || sysRole.getIsDeleted() == 1) {
                log.warn("用户角色无效，roleId={}", currentUser.getRoleId());
                return Result.error(ResultCode.PARAM_ERROR, "用户角色无效");
            }

            // 4. 构造并返回更新后的 LoginVO 对象
            LoginVO loginVO = new LoginVO();
            loginVO.setUserId((currentUser.getId()));
            loginVO.setRoleCode(sysRole.getRoleCode());
            loginVO.setUsername(currentUser.getUsername());
            loginVO.setRealName(currentUser.getRealName());
            loginVO.setPhone(currentUser.getPhone());
            loginVO.setEmail(currentUser.getEmail());
            loginVO.setIntroduce(currentUser.getIntroduce());
            loginVO.setLastLoginTime(currentUser.getLastLoginTime());
            
            // 从StudentInfo表获取学生信息（复用上面已查询的studentInfo对象）
            if (studentInfo != null) {
                loginVO.setCollegeId(studentInfo.getCollegeId());
                loginVO.setCollegeName(studentInfo.getCollegeName());
                loginVO.setMajorId(studentInfo.getMajorId());
                loginVO.setMajor(studentInfo.getMajor());
                loginVO.setGrade(studentInfo.getGrade());
                loginVO.setClassName(studentInfo.getClassName());
            }

            log.info("用户信息更新成功：用户名={}", currentUser.getUsername());
            return Result.success( "用户信息更新成功", loginVO);
        } catch (Exception e) {
            log.error("用户信息更新失败：{}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR,"用户信息更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> uploadAvatar(MultipartFile file) {
        try {
            // 1. 参数校验
            if (file == null || file.isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR, "请选择要上传的头像文件");
            }

            // 2. 获取当前登录用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
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

            // 4. 文件类型和大小校验
            String contentType = file.getContentType();
            if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
                return Result.error(ResultCode.PARAM_ERROR, "只支持 JPG 和 PNG 格式的图片");
            }

            // 使用Tika检测实际MIME类型，防止扩展名/Content-Type伪造
            try {
                org.apache.tika.Tika tika = new org.apache.tika.Tika();
                String detectedType = tika.detect(file.getInputStream(), file.getOriginalFilename());
                if (!"image/jpeg".equals(detectedType) && !"image/png".equals(detectedType)) {
                    return Result.error(ResultCode.PARAM_ERROR, "文件内容与声称的图片格式不匹配");
                }
            } catch (Exception e) {
                return Result.error(ResultCode.PARAM_ERROR, "文件校验失败，请重试");
            }

            // 限制文件大小（例如5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error(ResultCode.PARAM_ERROR, "头像文件大小不能超过5MB");
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : "";

            // 生成唯一文件名
            String uniqueFileName = "avatar/" + currentUser.getId() + "/" +
                    UUID.randomUUID().toString().replace("-", "") + fileExtension;

            // 5. 上传文件到 MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(avatarBucket)
                        .object(uniqueFileName)
                        .stream(inputStream, inputStream.available(), -1)
                        .contentType(contentType)
                        .build());
            }

            // 6. 构建可访问的头像URL
            String avatarUrl = minioEndpoint + "/" + avatarBucket + "/" + uniqueFileName;

            // 7. 更新用户头像路径
            currentUser.setAvatar(avatarUrl);
            sysUserMapper.updateById(currentUser);
            
            // 8. 同步更新会话成员的头像
            LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ConversationMember::getUserId, currentUser.getId());
            List<ConversationMember> members = conversationMemberMapper.selectList(wrapper);
            for (ConversationMember member : members) {
                member.setAvatar(avatarUrl);
                conversationMemberMapper.updateById(member);
            }
            log.info("同步更新会话成员头像成功: userId={}", currentUser.getId());

            log.info("用户头像上传成功： 头像路径={}", avatarUrl);
            return Result.success("头像上传成功", avatarUrl);
        } catch (Exception e) {
            log.error("头像上传失败: 错误信息={}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "头像上传失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Page<LoginHistoryVO>> getLoginHistory(LoginLogQueryReq queryReq) {
        try {
            // 获取当前登录用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
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

            // 获取用户角色
            SysRole sysRole = sysRoleMapper.selectById(currentUser.getRoleId());
            String roleCode = sysRole != null ? sysRole.getRoleCode() : "";

            // 构建查询条件
            LambdaQueryWrapper<SysLoginLog> queryWrapper = Wrappers.<SysLoginLog>lambdaQuery();

            // 管理员可以看到全部，学生和教师只能看到自己的
            if (!UserTypeEnum.ROLE_ADMIN.equals(roleCode)) {
                queryWrapper.eq(SysLoginLog::getUsername, currentUsername);
            } else if (StringUtils.hasText(queryReq.getUsername())) {
                // 管理员可以按用户名筛选
                queryWrapper.like(SysLoginLog::getUsername, queryReq.getUsername());
            }

            queryWrapper.like(StringUtils.hasText(queryReq.getLoginIp()), SysLoginLog::getLoginIp, queryReq.getLoginIp())
                    .eq(queryReq.getLoginStatus() != null, SysLoginLog::getLoginResult, queryReq.getLoginStatus())
                    .orderByDesc(SysLoginLog::getLoginTime);

            // 分页查询
            Page<SysLoginLog> page = new Page<>(queryReq.getPageNo(), queryReq.getPageSize());
            Page<SysLoginLog> resultPage = sysLoginLogMapper.selectPage(page, queryWrapper);

            // 转换为VO
            Page<LoginHistoryVO> voPage = new Page<>(queryReq.getPageNo(), queryReq.getPageSize(), resultPage.getTotal());
            List<LoginHistoryVO> voList = resultPage.getRecords().stream().map(sysLoginLog -> {
                LoginHistoryVO vo = new LoginHistoryVO();
                BeanUtils.copyProperties(sysLoginLog, vo);
                return vo;
            }).collect(Collectors.toList());

            voPage.setRecords(voList);

            log.info("查询登录历史成功，查询条件：{}，当前用户：{}，角色：{}", queryReq, currentUsername, roleCode);
            return Result.success("查询成功", voPage);
        } catch (Exception e) {
            log.error("查询登录历史失败：{}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> updatePassword(UpdatePasswordReq updatePasswordReq) {
        try {
            // 1. 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
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

            // 2. 校验原密码是否正确
            if (!passwordEncoder.matches(updatePasswordReq.getOldPassword(), currentUser.getPassword())) {
                return Result.error(ResultCode.PARAM_ERROR, "原密码错误");
            }

            // 4. 校验新密码不能与原密码相同
            if (updatePasswordReq.getNewPassword().equals(updatePasswordReq.getOldPassword())) {
                return Result.error(ResultCode.PARAM_ERROR, "新密码不能与原密码相同");
            }

            // 5. 更新密码（加密存储）
            currentUser.setPassword(passwordEncoder.encode(updatePasswordReq.getNewPassword()));
            sysUserMapper.updateById(currentUser);

            // 6. 吊销该用户的所有旧 token
            tokenRevocationService.revokeAllTokensForUser(currentUser.getId());

            log.info("用户密码修改成功：用户名={}", currentUsername);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            log.error("用户密码修改失败：{}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "密码修改失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> sendEmailCode(String email) {
        // 生成6位随机验证码
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        // 将验证码存储到Redis中，设置5分钟过期时间
        redisTemplate.opsForValue().set("email_code:" + email, code, 300, TimeUnit.SECONDS);

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("验证码");
            message.setText("您的验证码是：" + code + "，5分钟内有效。");
            mailSender.send(message);
            return Result.success("验证码发送成功");
        } catch (Exception e) {
            log.error("发送验证码失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "验证码发送失败");
        }
    }


    @Override
    public Result<String> verifyEmail(String token) {
        // 从Redis中获取token对应的信息
        String email = redisTemplate.opsForValue().get("email_verify_token:" + token);

        if (StringUtils.isEmpty(email)) {
            return Result.error(ResultCode.PARAM_ERROR, "验证链接已失效");
        }

        // 更新用户邮箱状态为已验证
        SysUser user = sysUserMapper.selectByEmail(email);
        if (user != null) {
            user.setEmailVerified(1);
            sysUserMapper.updateById(user);

            // 删除已使用的token
            redisTemplate.delete("email_verify_token:" + token);

            return Result.success("邮箱验证成功");
        }

        return Result.error(ResultCode.PARAM_ERROR, "用户不存在");
    }


    @Override
    public Result<String> sendVerifyEmail(String email) {
        // 生成唯一验证token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 将token和邮箱存储到Redis中，设置24小时过期时间
        redisTemplate.opsForValue().set("email_verify_token:" + token, email, 24, TimeUnit.HOURS);

        // 构建验证链接（使用配置的前端地址）
        String baseUrl = java.util.Optional.ofNullable(frontendBaseUrl)
                .filter(s -> !s.isEmpty())
                .orElseGet(() -> System.getenv().getOrDefault("FRONTEND_BASE_URL", ""));
        String verifyUrl = baseUrl + "/verify-email?token=" + token;
        // 发送验证邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("邮箱验证");
            message.setText("请点击以下链接完成邮箱验证：\n" + verifyUrl + "\n链接24小时内有效。");
            mailSender.send(message);
            return Result.success("验证邮件发送成功");
        } catch (Exception e) {
            log.error("发送验证邮件失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "验证邮件发送失败");
        }
    }


    @Override
    public Result<String> updateEmail(UpdateEmailReq updateReq) {
        // 获取用户ID和新邮箱
        Long userId = updateReq.getUserId();
        String newEmail = updateReq.getNewEmail();
        String verificationCode = updateReq.getVerificationCode();

        // 检查验证码是否正确
        String storedCode = redisTemplate.opsForValue().get("email_code:" + newEmail);
        if (!verificationCode.equals(storedCode)) {
            return Result.error(ResultCode.PARAM_ERROR, "验证码错误");
        }

        // 查找用户
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error(ResultCode.PARAM_ERROR, "用户不存在");
        }

        // 检查邮箱是否已被其他用户使用
        SysUser existingUser = sysUserMapper.selectByEmail(newEmail);
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            return Result.error(ResultCode.PARAM_ERROR, "该邮箱已被其他用户使用");
        }

        // 更新用户邮箱
        user.setEmail(newEmail);
        user.setEmailVerified(0); // 新邮箱需要重新验证
        sysUserMapper.updateById(user);

        // 删除已使用的验证码
        redisTemplate.delete("email_code:" + newEmail);

        return Result.success("邮箱更新成功，请重新验证新邮箱");
    }

}
