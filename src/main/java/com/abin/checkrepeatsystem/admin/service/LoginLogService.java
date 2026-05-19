package com.abin.checkrepeatsystem.admin.service;

import com.abin.checkrepeatsystem.pojo.entity.SysLoginLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface LoginLogService {

    Page<SysLoginLog> selectPage(Page<SysLoginLog> page, LambdaQueryWrapper<SysLoginLog> wrapper);

    List<SysLoginLog> selectList(LambdaQueryWrapper<SysLoginLog> wrapper);

    Long selectCount(LambdaQueryWrapper<SysLoginLog> wrapper);
}
