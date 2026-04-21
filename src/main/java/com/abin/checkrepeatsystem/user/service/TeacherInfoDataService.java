package com.abin.checkrepeatsystem.user.service;


import com.abin.checkrepeatsystem.pojo.entity.TeacherInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 教师信息服务接口
 */
public interface TeacherInfoDataService extends IService<TeacherInfo> {

    /**
     * 根据用户ID获取教师信息
     * @param userId 用户ID
     * @return 教师信息
     */
    TeacherInfo getByUserId(Long userId);

    /**
     * 保存或更新教师信息
     * @param teacherInfo 教师信息
     * @return 是否成功
     */
    boolean saveOrUpdateByUserId(TeacherInfo teacherInfo);
}

