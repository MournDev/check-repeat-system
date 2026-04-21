package com.abin.checkrepeatsystem.user.service;

import com.abin.checkrepeatsystem.pojo.entity.StudentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 学生信息服务接口
 */
public interface StudentInfoService extends IService<StudentInfo> {

    /**
     * 根据用户ID获取学生信息
     * @param userId 用户ID
     * @return 学生信息
     */
    StudentInfo getByUserId(Long userId);

    /**
     * 保存或更新学生信息
     * @param studentInfo 学生信息
     * @return 是否成功
     */
    boolean saveOrUpdateByUserId(StudentInfo studentInfo);
}
