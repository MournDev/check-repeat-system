package com.abin.checkrepeatsystem.admin.service;

import com.abin.checkrepeatsystem.pojo.entity.SystemParam;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SystemParamService extends IService<SystemParam> {

    /**
     * 获取系统参数（单例模式，只有一条记录）
     * @return 系统参数
     */
    SystemParam getSystemParam();

    /**
     * 更新系统参数
     * @param systemParam 系统参数
     * @return 更新结果
     */
    boolean updateSystemParam(SystemParam systemParam);

    /**
     * 初始化系统参数（如果不存在）
     * @return 初始化结果
     */
    boolean initSystemParam();

    /**
     * 获取论文最大大小
     * @return 最大大小（字节）
     */
    Long getMaxPaperSize();

    /**
     * 获取最大并发查重数
     * @return 最大并发数
     */
    Integer getMaxConcurrentCheck();

    /**
     * 获取JWT令牌有效期
     * @return 有效期（毫秒）
     */
    Long getJwtExpiration();

    /**
     * 获取默认重复率阈值
     * @return 阈值（百分比）
     */
    Double getDefaultThreshold();

    /**
     * 获取文件存储类型
     * @return 存储类型
     */
    String getStorageType();

    /**
     * 获取系统维护状态
     * @return 维护状态（0-正常，1-维护中）
     */
    Integer getMaintenanceStatus();

    /**
     * 设置系统维护状态
     * @param status 维护状态
     * @param notice 维护公告
     * @return 设置结果
     */
    boolean setMaintenanceStatus(Integer status, String notice);
}
