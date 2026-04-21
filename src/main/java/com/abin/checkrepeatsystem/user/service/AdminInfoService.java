package com.abin.checkrepeatsystem.user.service;

import com.abin.checkrepeatsystem.pojo.entity.AdminInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 管理员信息服务接口
 */
public interface AdminInfoService extends IService<AdminInfo> {

    /**
     * 根据用户ID获取管理员信息
     * @param userId 用户ID
     * @return 管理员信息
     */
    AdminInfo getByUserId(Long userId);

    /**
     * 保存或更新管理员信息
     * @param adminInfo 管理员信息
     * @return 是否成功
     */
    boolean saveOrUpdateByUserId(AdminInfo adminInfo);
}
