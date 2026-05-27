package com.abin.checkrepeatsystem.user.service;


import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;

/**
 * 指导老师分配服务接口（核心业务：指导老师自动/手动分配、任务数校验与同步）
 */
public interface AdvisorAssignService {

    /**
     * 1. 自动分配指导老师（核心方法）
     * 逻辑：根据学生专业查询“有指导权限且未达上限”的教师，按当前任务数升序分配（负载均衡）
     *
     * @param paperSubmitId 论文ID（关联 paper_submit 表，需为“待分配”状态）
     * @return 分配结果（含分配的指导老师ID、论文状态更新结果）
     */
    Result<Boolean> autoAssignAdvisor(Long paperSubmitId);

    /**
     * 2. 手动分配指导老师（管理员操作）
     * 逻辑：支持管理员指定指导老师，需额外校验“指定教师是否有该专业指导权限+未达上限”
     * @param paperId 论文ID
     * @param teacherId 目标指导老师ID（管理员指定）
     * @param reason 原因
     * @return 分配结果
     */
    Result<?> manualAssignAdvisor(Long paperId, Long teacherId, String reason);

    /**
     * 教师拒绝后重新分配指导老师（排除已拒绝的教师）
     * @param paperId 论文ID
     * @param excludedTeacherId 需要排除的教师ID
     */
    void reassignAfterRejection(Long paperId, Long excludedTeacherId);
}
