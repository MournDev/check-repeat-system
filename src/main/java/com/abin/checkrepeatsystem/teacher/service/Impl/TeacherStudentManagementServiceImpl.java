package com.abin.checkrepeatsystem.teacher.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.PaperStatusEnum;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.enums.UserTypeEnum;
import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.user.mapper.StudentInfoMapper;
import com.abin.checkrepeatsystem.user.service.StudentInfoService;
import com.abin.checkrepeatsystem.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.pojo.entity.SystemMessage;
import com.abin.checkrepeatsystem.pojo.entity.StudentInfo;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.teacher.dto.*;
import com.abin.checkrepeatsystem.teacher.service.TeacherStudentManagementService;
import com.abin.checkrepeatsystem.user.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 教师学生管理服务实现类
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TeacherStudentManagementServiceImpl implements TeacherStudentManagementService {

    private final SysUserMapper sysUserMapper;

    private final PaperInfoMapper paperInfoMapper;

    private final MessageService messageService;

    private final StudentInfoService studentInfoService;

    private final FileService fileService;

    private final SysUserService sysUserService;

    private final StudentInfoMapper studentInfoMapper;

    @Override
    public Result<Object> getStudentList(StudentListRequestDTO requestDTO) {
        try {
            LambdaQueryWrapper<PaperInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PaperInfo::getTeacherId, requestDTO.getTeacherId())
                       .eq(PaperInfo::getIsDeleted, 0);

            // 搜索条件：标题搜索 + 跨表搜索（学生姓名/学号/专业/学院）
            if (requestDTO.getSearch() != null && !requestDTO.getSearch().trim().isEmpty()) {
                String keyword = requestDTO.getSearch().trim();
                String searchValue = "%" + keyword + "%";

                // 先查询 sys_user 中匹配的学生ID
                Set<Long> matchingStudentIds = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId)
                        .eq(SysUser::getUserType, UserTypeEnum.ROLE_STUDENT)
                        .and(w -> w.like(SysUser::getUsername, searchValue)
                                 .or()
                                 .like(SysUser::getRealName, searchValue))
                ).stream().map(SysUser::getId).collect(Collectors.toSet());

                // 再查询 student_info 中匹配的学生ID
                Set<Long> siStudentIds = studentInfoMapper.selectList(
                    new LambdaQueryWrapper<StudentInfo>()
                        .select(StudentInfo::getUserId)
                        .and(w -> w.like(StudentInfo::getMajor, searchValue)
                                 .or()
                                 .like(StudentInfo::getCollegeName, searchValue))
                ).stream().map(StudentInfo::getUserId).collect(Collectors.toSet());
                matchingStudentIds.addAll(siStudentIds);

                queryWrapper.and(w -> {
                    w.like(PaperInfo::getPaperTitle, keyword);
                    if (!matchingStudentIds.isEmpty()) {
                        w.or().in(PaperInfo::getStudentId, matchingStudentIds);
                    }
                });
            }

            // 状态筛选
            if (requestDTO.getStatus() != null && !requestDTO.getStatus().trim().isEmpty()) {
                queryWrapper.eq(PaperInfo::getPaperStatus, requestDTO.getStatus());
            }

            // 学院筛选
            Set<Long> collegeStudentIds = queryStudentInfoIds(requestDTO.getCollegeId(), requestDTO.getCollege());
            if (collegeStudentIds != null) {
                if (collegeStudentIds.isEmpty()) {
                    return Result.success("获取学生列表成功", buildEmptyPageResult(requestDTO));
                }
                queryWrapper.in(PaperInfo::getStudentId, collegeStudentIds);
            }

            // 专业筛选
            if (requestDTO.getMajorId() != null && requestDTO.getMajorId() > 0) {
                Set<Long> majorStudentIds = studentInfoMapper.selectList(
                    new LambdaQueryWrapper<StudentInfo>()
                        .select(StudentInfo::getUserId)
                        .eq(StudentInfo::getMajorId, requestDTO.getMajorId())
                ).stream().map(StudentInfo::getUserId).collect(Collectors.toSet());

                if (majorStudentIds.isEmpty()) {
                    return Result.success("获取学生列表成功", buildEmptyPageResult(requestDTO));
                }
                queryWrapper.in(PaperInfo::getStudentId, majorStudentIds);
            }

            Page<PaperInfo> page = new Page<>(requestDTO.getPage(), requestDTO.getPageSize());
            Page<PaperInfo> paperPage = paperInfoMapper.selectPage(page, queryWrapper);

            List<StudentListDTO> studentList = paperPage.getRecords().stream()
                    .map(this::convertToStudentListDTO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(StudentListDTO::getStudentId, Function.identity(), (existing, replacement) -> existing))
                    .values().stream()
                    .collect(Collectors.toList());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("list", studentList);
            responseData.put("totalCount", paperPage.getTotal());
            responseData.put("currentPage", paperPage.getCurrent());
            responseData.put("totalPages", paperPage.getPages());

            return Result.success("获取学生列表成功", responseData);
        } catch (Exception e) {
            log.error("获取学生列表失败: teacherId={}", requestDTO.getTeacherId(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取学生列表失败: " + e.getMessage());
        }
    }

    private Set<Long> queryStudentInfoIds(Long collegeId, String collegeName) {
        if (collegeId != null && collegeId > 0) {
            return studentInfoMapper.selectList(
                new LambdaQueryWrapper<StudentInfo>()
                    .select(StudentInfo::getUserId)
                    .eq(StudentInfo::getCollegeId, collegeId)
            ).stream().map(StudentInfo::getUserId).collect(Collectors.toSet());
        }
        if (collegeName != null && !collegeName.trim().isEmpty()) {
            return studentInfoMapper.selectList(
                new LambdaQueryWrapper<StudentInfo>()
                    .select(StudentInfo::getUserId)
                    .like(StudentInfo::getCollegeName, collegeName.trim())
            ).stream().map(StudentInfo::getUserId).collect(Collectors.toSet());
        }
        return null;
    }

    private Map<String, Object> buildEmptyPageResult(StudentListRequestDTO requestDTO) {
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("list", Collections.emptyList());
        responseData.put("totalCount", 0L);
        responseData.put("currentPage", requestDTO.getPage());
        responseData.put("totalPages", 0);
        return responseData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteStudent(Long studentId) {
        // 检查学生是否存在且未被删除
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getId, studentId)
                  .eq(SysUser::getIsDeleted, 0);

        SysUser student = sysUserMapper.selectOne(userWrapper);
        if (student == null) {
            log.warn("学生不存在或已被删除: studentId={}", studentId);
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "学生不存在或已被删除");
        }

        // 软删除学生
        student.setIsDeleted(1);
        student.setUpdateTime(LocalDateTime.now());
        int result = sysUserMapper.updateById(student);

        if (result > 0) {
            log.info("成功删除学生: studentId={}", studentId);
            return true;
        }
        throw new BusinessException(ResultCode.SYSTEM_ERROR, "删除学生失败");
    }

    @Override
    public Result<Object> getStudentStats(Long teacherId) {
        try {
            Map<String, Object> stats = new HashMap<>();

            // 查询该教师指导的所有论文
            LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
            paperWrapper.eq(PaperInfo::getTeacherId, teacherId)
                       .eq(PaperInfo::getIsDeleted, 0);

            List<PaperInfo> papers = paperInfoMapper.selectList(paperWrapper);

            // 统计总学生数（去重）
            Set<Long> studentIds = papers.stream()
                    .map(PaperInfo::getStudentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            // 已提交论文数（submit_time IS NOT NULL，去重学生数）
            long submittedCount = papers.stream()
                    .filter(paper -> paper.getStudentId() != null && paper.getSubmitTime() != null)
                    .map(PaperInfo::getStudentId)
                    .distinct()
                    .count();

            // 已分配导师数（paper_status = 'assigned'，去重学生数）
            long assignedCount = papers.stream()
                    .filter(paper -> paper.getStudentId() != null && "assigned".equals(paper.getPaperStatus()))
                    .map(PaperInfo::getStudentId)
                    .distinct()
                    .count();
            
            // 已完成审核数（completed状态，去重学生数）
            long completedCount = papers.stream()
                    .filter(paper -> paper.getStudentId() != null && PaperStatusEnum.COMPLETED.getCode().equals(paper.getPaperStatus()))
                    .map(PaperInfo::getStudentId)
                    .distinct()
                    .count();

            stats.put("totalStudents", studentIds.size());
            stats.put("submittedStudents", submittedCount);
            stats.put("assignedStudents", assignedCount);
            stats.put("completedStudents", completedCount);

            return Result.success("获取统计信息成功", stats);
        } catch (Exception e) {
            log.error("获取学生统计信息失败: teacherId={}", teacherId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取统计信息失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignAdvisor(Long studentId, AssignAdvisorDTO assignAdvisorDTO) {
        // 更新论文信息中的导师信息
        LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
        paperWrapper.eq(PaperInfo::getStudentId, studentId)
                   .eq(PaperInfo::getIsDeleted, 0);

        List<PaperInfo> papers = paperInfoMapper.selectList(paperWrapper);

        if (papers.isEmpty()) {
            log.warn("学生没有论文信息: studentId={}", studentId);
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "学生没有论文信息");
        }

        boolean success = true;
        for (PaperInfo paper : papers) {
            paper.setTeacherId(assignAdvisorDTO.getAdvisorId());
            paper.setTeacherName(assignAdvisorDTO.getAdvisorName());
            paper.setUpdateTime(LocalDateTime.now());

            int result = paperInfoMapper.updateById(paper);
            if (result <= 0) {
                success = false;
                log.warn("更新论文导师信息失败: paperId={}", paper.getId());
            }
        }

        if (success) {
            log.info("成功分配导师: studentId={}, advisorId={}", studentId, assignAdvisorDTO.getAdvisorId());
        }
        return success;
    }

    @Override
    public boolean sendMessage(SendMessageDTO sendMessageDTO) {
        try {
            // 使用当前登录用户ID
            Long currentUserId = UserBusinessInfoUtils.getCurrentUserId();
                
            SystemMessage message = new SystemMessage();
            message.setSenderId(currentUserId); // 使用当前教师 ID 而非硬编码 0
            message.setReceiverId(sendMessageDTO.getReceiverId());
            message.setTitle(sendMessageDTO.getTitle() != null ? sendMessageDTO.getTitle() : "系统消息");
            message.setContent(sendMessageDTO.getContent());
            message.setMessageType("PRIVATE");
            message.setCreateTime(LocalDateTime.now());
            message.setUpdateTime(LocalDateTime.now());
            message.setIsRead(0);
            message.setPriority(1);

            Result<Boolean> result = messageService.sendMessage(message);
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("消息发送成功: receiverId={}", sendMessageDTO.getReceiverId());
                return true;
            } else {
                log.warn("消息发送失败: receiverId={}, error={}", 
                        sendMessageDTO.getReceiverId(), result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("发送消息失败: receiverId={}", sendMessageDTO.getReceiverId(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResultDTO batchAssignAdvisor(BatchAssignAdvisorDTO batchAssignDTO) {
        BatchOperationResultDTO result = new BatchOperationResultDTO();
        List<BatchOperationResultDTO.FailureDetail> failures = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < batchAssignDTO.getStudentIds().size(); i++) {
            Long studentId = batchAssignDTO.getStudentIds().get(i);
            try {
                AssignAdvisorDTO assignDTO = new AssignAdvisorDTO();
                assignDTO.setAdvisorId(batchAssignDTO.getAdvisorId());
                assignDTO.setAdvisorName(batchAssignDTO.getAdvisorName());
                
                boolean success = assignAdvisor(studentId, assignDTO);
                if (success) {
                    successCount++;
                } else {
                    BatchOperationResultDTO.FailureDetail failure = new BatchOperationResultDTO.FailureDetail();
                    failure.setIndex(i + 1);
                    failure.setReason("分配导师失败");
                    failures.add(failure);
                }
            } catch (Exception e) {
                BatchOperationResultDTO.FailureDetail failure = new BatchOperationResultDTO.FailureDetail();
                failure.setIndex(i + 1);
                failure.setReason("系统异常: " + e.getMessage());
                failures.add(failure);
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failures.size());
        result.setFailures(failures);
        return result;
    }

    @Override
    public BatchOperationResultDTO batchSendMessage(BatchSendMessageDTO batchSendDTO) {
        BatchOperationResultDTO result = new BatchOperationResultDTO();
        List<BatchOperationResultDTO.FailureDetail> failures = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < batchSendDTO.getReceiverIds().size(); i++) {
            Long receiverId = batchSendDTO.getReceiverIds().get(i);
            try {
                SendMessageDTO sendDTO = new SendMessageDTO();
                sendDTO.setReceiverId(receiverId);
                sendDTO.setReceiverType(batchSendDTO.getReceiverType());
                sendDTO.setTitle(batchSendDTO.getTitle());
                sendDTO.setContent(batchSendDTO.getContent());
                
                boolean success = sendMessage(sendDTO);
                if (success) {
                    successCount++;
                } else {
                    BatchOperationResultDTO.FailureDetail failure = new BatchOperationResultDTO.FailureDetail();
                    failure.setIndex(i + 1);
                    failure.setReason("消息发送失败");
                    failures.add(failure);
                }
            } catch (Exception e) {
                BatchOperationResultDTO.FailureDetail failure = new BatchOperationResultDTO.FailureDetail();
                failure.setIndex(i + 1);
                failure.setReason("系统异常: " + e.getMessage());
                failures.add(failure);
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failures.size());
        result.setFailures(failures);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResultDTO batchDeleteStudents(BatchDeleteDTO batchDeleteDTO) {
        BatchOperationResultDTO result = new BatchOperationResultDTO();
        List<BatchOperationResultDTO.FailureDetail> failures = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < batchDeleteDTO.getStudentIds().size(); i++) {
            Long studentId = batchDeleteDTO.getStudentIds().get(i);
            try {
                boolean success = deleteStudent(studentId);
                if (success) {
                    successCount++;
                } else {
                    BatchOperationResultDTO.FailureDetail failure = new BatchOperationResultDTO.FailureDetail();
                    failure.setIndex(i + 1);
                    failure.setReason("删除失败");
                    failures.add(failure);
                }
            } catch (Exception e) {
                BatchOperationResultDTO.FailureDetail failure = new BatchOperationResultDTO.FailureDetail();
                failure.setIndex(i + 1);
                failure.setReason("系统异常: " + e.getMessage());
                failures.add(failure);
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failures.size());
        result.setFailures(failures);
        return result;
    }

    @Override
    public String exportStudents(ExportRequestDTO exportRequest) {
        // 获取学生数据
        StudentListRequestDTO listRequest = new StudentListRequestDTO();
        listRequest.setTeacherId(exportRequest.getTeacherId());
        listRequest.setPage(1);
        listRequest.setPageSize(10000); // 导出最大数量
        listRequest.setSearch(exportRequest.getSearch());
        listRequest.setStatus(exportRequest.getStatus());
        listRequest.setCollege(exportRequest.getCollege());

        Result<Object> result = getStudentList(listRequest);
        if (!result.isSuccess()) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "获取学生数据失败: " + result.getMessage());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        @SuppressWarnings("unchecked")
        List<StudentListDTO> studentList = (List<StudentListDTO>) data.get("list");

        // 准备导出数据
        List<Map<String, Object>> exportData = studentList.stream().map(student -> {
            Map<String, Object> row = new HashMap<>();
            row.put("学号", student.getUsername());
            row.put("姓名", student.getStudentName());
            row.put("学院", student.getCollegeName());
            row.put("专业", student.getMajor());
            row.put("年级", student.getGrade());
            row.put("论文状态", student.getPaperStatus());
            row.put("指导老师", student.getAdvisorName());
            row.put("提交时间", student.getSubmitTime());
            row.put("相似度", student.getSimilarity());
            return row;
        }).collect(Collectors.toList());

        // 生成Excel文件
        String fileName = "学生列表_" + System.currentTimeMillis() + ".xlsx";
        String filePath = exportToExcel(exportData, fileName);

        log.info("导出学生数据成功: teacherId={}, count={}",
                exportRequest.getTeacherId(), studentList.size());
        return filePath;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Object> addStudent(AddStudentDTO addStudentDTO) {
        try {
            // 参数校验
            if (addStudentDTO.getUsername() == null || addStudentDTO.getUsername().trim().isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR, "学号不能为空");
            }
            if (addStudentDTO.getStudentName() == null || addStudentDTO.getStudentName().trim().isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR, "学生姓名不能为空");
            }
            if (addStudentDTO.getCollegeId() == null || addStudentDTO.getCollegeId() <= 0) {
                return Result.error(ResultCode.PARAM_ERROR, "学院ID不合法");
            }
            if (addStudentDTO.getCollegeName() == null || addStudentDTO.getCollegeName().trim().isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR, "学院名称不能为空");
            }
            if (addStudentDTO.getMajorId() == null || addStudentDTO.getMajorId() <= 0) {
                return Result.error(ResultCode.PARAM_ERROR, "专业ID不合法");
            }
            if (addStudentDTO.getMajor() == null || addStudentDTO.getMajor().trim().isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR, "专业不能为空");
            }
            if (addStudentDTO.getGrade() == null || addStudentDTO.getGrade().trim().isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR, "年级不能为空");
            }

            // 检查学号是否已存在
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(SysUser::getUsername, addStudentDTO.getUsername().trim())
                      .eq(SysUser::getIsDeleted, 0);

            SysUser existingUser = sysUserMapper.selectOne(userWrapper);
            if (existingUser != null) {
                return Result.error(ResultCode.PARAM_ERROR, "学号已存在");
            }

            // 创建新学生用户
            SysUser newUser = new SysUser();
            newUser.setUsername(addStudentDTO.getUsername().trim());
            newUser.setRealName(addStudentDTO.getStudentName().trim());
            newUser.setEmail(addStudentDTO.getEmail() != null ? addStudentDTO.getEmail().trim() : null);
            newUser.setPhone(addStudentDTO.getPhone() != null ? addStudentDTO.getPhone().trim() : null);
            newUser.setUserType(UserTypeEnum.ROLE_STUDENT);
            newUser.setIsDeleted(0);
            newUser.setCreateTime(LocalDateTime.now());
            newUser.setUpdateTime(LocalDateTime.now());

            int insertResult = sysUserMapper.insert(newUser);
            if (insertResult > 0) {
                log.info("成功添加学生: username={}, studentName={}",
                        addStudentDTO.getUsername(), addStudentDTO.getStudentName());

                // 创建学生信息
                StudentInfo studentInfo = new StudentInfo();
                studentInfo.setUserId(newUser.getId());
                studentInfo.setCollegeId(addStudentDTO.getCollegeId());
                studentInfo.setCollegeName(addStudentDTO.getCollegeName().trim());
                studentInfo.setMajorId(addStudentDTO.getMajorId());
                studentInfo.setMajor(addStudentDTO.getMajor().trim());
                studentInfo.setGrade(addStudentDTO.getGrade().trim());
                studentInfo.setClassName(addStudentDTO.getClassName() != null ? addStudentDTO.getClassName().trim() : null);
                studentInfo.setIsDeleted(0);
                studentInfo.setCreateTime(LocalDateTime.now());
                studentInfo.setUpdateTime(LocalDateTime.now());
                studentInfoService.save(studentInfo);

                // 构建返回数据
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("studentId", newUser.getId());
                responseData.put("username", newUser.getUsername());
                responseData.put("studentName", newUser.getRealName());
                responseData.put("collegeId", addStudentDTO.getCollegeId());
                responseData.put("collegeName", addStudentDTO.getCollegeName().trim());
                responseData.put("majorId", addStudentDTO.getMajorId());
                responseData.put("major", addStudentDTO.getMajor().trim());
                responseData.put("grade", addStudentDTO.getGrade().trim());
                responseData.put("createTime", newUser.getCreateTime());

                return Result.success("学生添加成功", responseData);
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "添加学生失败");
            }
        } catch (Exception e) {
            log.error("添加学生失败: username={}", addStudentDTO.getUsername(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "添加学生失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultDTO importStudents(MultipartFile file) {
        ImportResultDTO result = new ImportResultDTO();
        List<ImportResultDTO.ImportFailure> failures = new ArrayList<>();
        int successCount = 0;

        try {
            // 解析Excel文件
            List<Map<String, Object>> importData = parseExcel(file.getInputStream());
            
            for (int i = 0; i < importData.size(); i++) {
                try {
                    Map<String, Object> rowData = importData.get(i);
                    
                    // 验证必要字段
                    String username = (String) rowData.get("学号");
                    String studentName = (String) rowData.get("姓名");
                    
                    if (username == null || username.trim().isEmpty()) {
                        ImportResultDTO.ImportFailure failure = new ImportResultDTO.ImportFailure();
                        failure.setRow(i + 2); // Excel行号从1开始，加上标题行
                        failure.setReason("学号不能为空");
                        failures.add(failure);
                        continue;
                    }

                    // 检查学号是否已存在
                    LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
                    userWrapper.eq(SysUser::getUsername, username)
                              .eq(SysUser::getIsDeleted, 0);
                    
                    SysUser existingUser = sysUserMapper.selectOne(userWrapper);
                    if (existingUser != null) {
                        ImportResultDTO.ImportFailure failure = new ImportResultDTO.ImportFailure();
                        failure.setRow(i + 2);
                        failure.setReason("学号已存在");
                        failures.add(failure);
                        continue;
                    }

                    // 创建新学生用户
                    SysUser newUser = new SysUser();
                    newUser.setUsername(username);
                    newUser.setRealName(studentName);
                    newUser.setUserType(UserTypeEnum.ROLE_STUDENT);
                    newUser.setIsDeleted(0);
                    newUser.setCreateTime(LocalDateTime.now());
                    newUser.setUpdateTime(LocalDateTime.now());

                    int insertResult = sysUserMapper.insert(newUser);
                    if (insertResult > 0) {
                        // 创建学生信息
                        StudentInfo studentInfo = new StudentInfo();
                        studentInfo.setUserId(newUser.getId());
                        studentInfo.setCollegeName((String) rowData.get("学院"));
                        studentInfo.setMajor((String) rowData.get("专业"));
                        studentInfo.setGrade((String) rowData.get("年级"));
                        studentInfo.setClassName(null);
                        studentInfo.setIsDeleted(0);
                        studentInfo.setCreateTime(LocalDateTime.now());
                        studentInfo.setUpdateTime(LocalDateTime.now());
                        studentInfoService.save(studentInfo);
                        
                        successCount++;
                    } else {
                        ImportResultDTO.ImportFailure failure = new ImportResultDTO.ImportFailure();
                        failure.setRow(i + 2);
                        failure.setReason("数据库插入失败");
                        failures.add(failure);
                    }
                } catch (Exception e) {
                    ImportResultDTO.ImportFailure failure = new ImportResultDTO.ImportFailure();
                    failure.setRow(i + 2);
                    failure.setReason("系统异常: " + e.getMessage());
                    failures.add(failure);
                }
            }

            result.setSuccessCount(successCount);
            result.setFailCount(failures.size());
            result.setFailures(failures);
            
            log.info("导入学生数据完成: success={}, fail={}", successCount, failures.size());
            return result;
        } catch (IOException e) {
            log.error("解析Excel文件失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 将PaperInfo转换为StudentListDTO
     */
    private StudentListDTO convertToStudentListDTO(PaperInfo paperInfo) {
        try {
            // 查询学生信息
            SysUser student = sysUserMapper.selectById(paperInfo.getStudentId());
            if (student == null) {
                return null;
            }

            // 从StudentInfo表获取学生详细信息
            StudentInfo studentInfo = studentInfoService.getByUserId(student.getId());

            StudentListDTO dto = new StudentListDTO();
            dto.setStudentId(student.getId());
            dto.setUsername(student.getUsername());
            dto.setStudentName(student.getRealName());
            
            // 从StudentInfo获取学院、专业、年级等信息
            if (studentInfo != null) {
                dto.setCollegeId(studentInfo.getCollegeId());
                dto.setCollegeName(studentInfo.getCollegeName());
                dto.setMajorId(studentInfo.getMajorId());
                dto.setMajor(studentInfo.getMajor());
                dto.setGrade(studentInfo.getGrade());
            } else {
                // 如果StudentInfo不存在，设置默认值
                dto.setCollegeId(null);
                dto.setCollegeName("");
                dto.setMajorId(null);
                dto.setMajor("");
                dto.setGrade("");
            }
            
            dto.setPaperStatus(paperInfo.getPaperStatus());
            dto.setAdvisorName(paperInfo.getTeacherName());
            dto.setSubmitTime(paperInfo.getSubmitTime());
            dto.setSimilarity(paperInfo.getSimilarityRate() != null ? 
                             BigDecimal.valueOf(paperInfo.getSimilarityRate().doubleValue()) : BigDecimal.ZERO);
            
            return dto;
        } catch (Exception e) {
            log.error("转换学生信息失败: paperId={}", paperInfo.getId(), e);
            return null;
        }
    }

    /**
     * 导出Excel文件
     */
    private String exportToExcel(List<Map<String, Object>> data, String fileName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("学生列表");
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"学号", "姓名", "学院", "专业", "年级", "论文状态", "指导老师", "提交时间", "相似度"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // 填充数据
            int rowNum = 1;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue((String) rowData.get("学号"));
                row.createCell(1).setCellValue((String) rowData.get("姓名"));
                row.createCell(2).setCellValue((String) rowData.get("学院"));
                row.createCell(3).setCellValue((String) rowData.get("专业"));
                row.createCell(4).setCellValue((String) rowData.get("年级"));
                row.createCell(5).setCellValue((String) rowData.get("论文状态"));
                row.createCell(6).setCellValue((String) rowData.get("指导老师"));
                row.createCell(7).setCellValue(rowData.get("提交时间") != null ? 
                    rowData.get("提交时间").toString() : "");
                Object similarityObj = rowData.get("相似度");
                if (similarityObj != null) {
                    if (similarityObj instanceof Number) {
                        row.createCell(8).setCellValue(((Number) similarityObj).doubleValue());
                    } else {
                        row.createCell(8).setCellValue(similarityObj.toString());
                    }
                } else {
                    row.createCell(8).setCellValue("");
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 保存文件
            String filePath = System.getProperty("java.io.tmpdir") + "/" + fileName;
            try (java.io.FileOutputStream fileOut = new java.io.FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            
            return filePath;
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "导出Excel失败: " + e.getMessage());
        }
    }

    /**
     * 解析Excel文件
     */
    private List<Map<String, Object>> parseExcel(java.io.InputStream inputStream) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            
            // 获取标题行
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
            
            // 解析数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        switch (cell.getCellType()) {
                            case STRING:
                                rowData.put(headers.get(j), cell.getStringCellValue());
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    rowData.put(headers.get(j), cell.getDateCellValue());
                                } else {
                                    rowData.put(headers.get(j), cell.getNumericCellValue());
                                }
                                break;
                            case BOOLEAN:
                                rowData.put(headers.get(j), cell.getBooleanCellValue());
                                break;
                            default:
                                rowData.put(headers.get(j), "");
                        }
                    } else {
                        rowData.put(headers.get(j), "");
                    }
                }
                result.add(rowData);
            }
            
            workbook.close();
            return result;
        } catch (Exception e) {
            log.error("解析Excel失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "解析Excel失败: " + e.getMessage());
        }
    }

    @Override
    public Result<StudentPaperInfoDTO> getStudentPaperInfo(Long teacherId, Long studentId) {
        // 1. 验证学生是否存在
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            return Result.error(ResultCode.PARAM_ERROR, "学生不存在");
        }

        // 2. 验证教师权限
        LambdaQueryWrapper<PaperInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaperInfo::getStudentId, studentId)
                   .eq(PaperInfo::getTeacherId, teacherId)
                   .eq(PaperInfo::getIsDeleted, 0);
        Long count = paperInfoMapper.selectCount(queryWrapper);
        if (count == 0 && !sysUserService.isAdmin(teacherId)) {
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限查看此学生的论文信息");
        }

        // 3. 获取最新论文
        PaperInfo latestPaper = paperInfoMapper.selectLatestPaper(studentId);
        if (latestPaper == null) {
            return Result.error(ResultCode.PARAM_ERROR, "该学生暂无论文");
        }

        // 4. 构造返回数据
        StudentPaperInfoDTO dto = buildStudentPaperInfoDTO(latestPaper);
        return Result.success("获取成功", dto);
    }

    @Override
    public Result<List<StudentPaperInfoDTO>> getStudentAllPapers(Long teacherId, Long studentId) {
        // 1. 验证学生是否存在
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            return Result.error(ResultCode.PARAM_ERROR, "学生不存在");
        }

        // 2. 验证教师权限
        LambdaQueryWrapper<PaperInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaperInfo::getStudentId, studentId)
                   .eq(PaperInfo::getTeacherId, teacherId)
                   .eq(PaperInfo::getIsDeleted, 0);
        Long count = paperInfoMapper.selectCount(queryWrapper);
        if (count == 0 && !sysUserService.isAdmin(teacherId)) {
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限查看此学生的论文信息");
        }

        // 3. 获取所有论文
        LambdaQueryWrapper<PaperInfo> allPapersQuery = new LambdaQueryWrapper<>();
        allPapersQuery.eq(PaperInfo::getStudentId, studentId)
                    .eq(PaperInfo::getIsDeleted, 0)
                    .orderByDesc(PaperInfo::getCreateTime);
        List<PaperInfo> allPapers = paperInfoMapper.selectList(allPapersQuery);
        if (allPapers.isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "该学生暂无论文");
        }

        // 4. 构造返回数据
        List<StudentPaperInfoDTO> dtoList = new ArrayList<>();
        for (PaperInfo paper : allPapers) {
            dtoList.add(buildStudentPaperInfoDTO(paper));
        }
        return Result.success("获取成功", dtoList);
    }

    private StudentPaperInfoDTO buildStudentPaperInfoDTO(PaperInfo paper) {
        StudentPaperInfoDTO dto = new StudentPaperInfoDTO();
        dto.setPaperId(paper.getId());
        dto.setPaperTitle(paper.getPaperTitle());
        dto.setFileId(paper.getFileId());
        dto.setPaperStatus(paper.getPaperStatus());
        dto.setSubmitTime(paper.getSubmitTime() != null ? paper.getSubmitTime().toString() : null);
        dto.setSimilarity(paper.getSimilarityRate() != null ? paper.getSimilarityRate().doubleValue() : 0.0);

        if (paper.getFileId() != null) {
            try {
                com.abin.checkrepeatsystem.pojo.entity.FileInfo fileInfo =
                        fileService.getById(paper.getFileId());
                if (fileInfo != null) {
                    dto.setFileName(fileInfo.getOriginalFilename());
                    dto.setFileSize(fileInfo.getFileSizeDesc());
                }
            } catch (Exception e) {
                log.warn("获取文件详细信息失败: {}", e.getMessage());
            }
        }
        return dto;
    }
}
