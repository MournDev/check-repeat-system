package com.abin.checkrepeatsystem.common.component;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.teacher.service.TeacherAssignmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class AssignmentTimeoutTask {

    private final TeacherAssignmentService teacherAssignmentService;

    private final PaperInfoMapper paperInfoMapper;

    private final com.abin.checkrepeatsystem.user.service.AdvisorAssignService advisorAssignService;

    /**
     * 每天凌晨2点处理超时未确认的分配
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void handleTimeoutAssignments() {
        log.info("开始执行分配超时处理任务");
        try {
            // 查询超时未确认的论文（分配时间超过3天）
            LocalDateTime timeoutTime = LocalDateTime.now().minusDays(3);

            LambdaQueryWrapper<PaperInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PaperInfo::getAllocationStatus, DictConstants.AllocationStatus.PENDING)
                    .lt(PaperInfo::getAllocationTime, timeoutTime);

            List<PaperInfo> timeoutPapers = paperInfoMapper.selectList(queryWrapper);

            int processedCount = 0;
            for (PaperInfo paper : timeoutPapers) {
                try {
                    // 自动拒绝超时未确认的分配
                    Result<Boolean> result = teacherAssignmentService.rejectAssignment(
                            paper.getId(),
                            paper.getTeacherId()
                    );

                    if (result.isSuccess()) {
                        processedCount++;
                        // 自动重新分配（排除超时未确认的教师）
                        try {
                            advisorAssignService.reassignAfterRejection(paper.getId(), paper.getTeacherId());
                        } catch (Exception reassignEx) {
                            log.error("超时拒绝后重新分配失败 - 论文ID: {}", paper.getId(), reassignEx);
                        }
                        log.info("自动拒绝超时论文成功 - 论文ID: {}, 老师ID: {}", paper.getId(), paper.getTeacherId());
                    } else {
                        log.error("自动拒绝超时论文失败 - 论文ID: {}, 错误: {}", paper.getId(), result.getMessage());
                    }

                } catch (Exception e) {
                    log.error("处理超时论文异常 - 论文ID: {}", paper.getId(), e);
                }
            }

            log.info("分配超时处理任务完成 - 处理数量: {}", processedCount);

        } catch (Exception e) {
            log.error("分配超时处理任务执行异常", e);
        }
    }
}
