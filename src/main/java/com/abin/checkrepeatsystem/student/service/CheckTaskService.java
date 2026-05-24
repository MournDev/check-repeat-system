package com.abin.checkrepeatsystem.student.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.student.dto.CheckTaskResultDTO;
import com.abin.checkrepeatsystem.user.vo.CheckResultVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CheckTaskService extends IService<CheckTask> {
    /**
     * 创建查重任务
     * @param paperId 论文ID
     * @return 查重任务结果
     */
    Result<CheckResultVO> createCheckTask(Long paperId);

    /**
     * 获取查重任务列表
     * @param paperId 论文ID
     * @param checkStatus 状态
     * @return 查重任务详情
     */
    Result<List<CheckTaskResultDTO>> getMyCheckTaskList(Long paperId, Integer checkStatus);

    /**
     * 获取查重任务详情
     * @param paperId 论文ID
     * @return 删除结果
     */
    Result<CheckTaskResultDTO> getCheckTaskDetail(Long paperId);

    /**
     * 取消待执行的查重任务
     *
     * @param taskId 待取消的任务ID
     * @return 删除结果
     */
    Result<String> cancelCheckTask(Long taskId);

    /**
     * 获取查重结果
     * @param paperId 论文ID
     * @return 查重结果
     */
    CheckResultVO getCheckResult(Long paperId);

    /**
     * 根据任务ID获取任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    Result<CheckTaskResultDTO> getCheckTaskById(Long taskId);

    /**
     * 删除查重任务
     * @param taskId 任务ID
     * @return 删除结果
     */
    Result<String> deleteCheckTask(Long taskId);

    /**
     * 批量创建查重任务
     * @param paperIds 论文ID列表
     * @return 批量创建结果
     */
    Result<String> createBatchCheckTasks(List<Long> paperIds);

    /**
     * 获取论文最新的查重任务
     * @param paperId 论文ID
     * @return 最新查重任务
     */
    CheckTask getLatestCheckTaskByPaperId(Long paperId);

    /**
     * 将CheckTask转换为DTO
     * @param checkTask 查重任务实体
     * @param withFullReport 是否包含完整报告
     * @return 查重任务DTO
     */
    CheckTaskResultDTO convertToTaskResultDTO(CheckTask checkTask, boolean withFullReport);

}
