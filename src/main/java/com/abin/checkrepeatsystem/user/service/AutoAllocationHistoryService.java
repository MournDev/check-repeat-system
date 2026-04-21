package com.abin.checkrepeatsystem.user.service;

import com.abin.checkrepeatsystem.pojo.entity.AutoAllocationHistory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface AutoAllocationHistoryService extends IService<AutoAllocationHistory> {

    /**
     * 创建自动分配历史记录
     * @param history 分配历史记录
     * @return 创建结果
     */
    boolean createHistory(AutoAllocationHistory history);

    /**
     * 获取分配历史列表
     * @param page 页码
     * @param size 每页大小
     * @param strategy 分配策略（可选）
     * @param result 分配结果（可选）
     * @return 历史记录列表
     */
    List<AutoAllocationHistory> getHistoryList(int page, int size, String strategy, String result);

    /**
     * 获取分配历史详情
     * @param id 历史记录ID
     * @return 历史记录详情
     */
    AutoAllocationHistory getHistoryById(Long id);

    /**
     * 获取分配统计信息
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 统计信息
     */
    Map<String, Object> getHistoryStats(String startDate, String endDate);

    /**
     * 获取最新的分配历史记录
     * @param limit 限制数量
     * @return 最新历史记录列表
     */
    List<AutoAllocationHistory> getLatestHistory(int limit);

    /**
     * 清理过期的分配历史记录
     * @param days 保留天数
     * @return 清理结果
     */
    boolean cleanExpiredHistory(int days);
}
