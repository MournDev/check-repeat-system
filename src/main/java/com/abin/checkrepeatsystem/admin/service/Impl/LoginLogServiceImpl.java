package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.service.LoginLogService;
import com.abin.checkrepeatsystem.pojo.entity.SysLoginLog;
import com.abin.checkrepeatsystem.user.mapper.SysLoginLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final SysLoginLogMapper sysLoginLogMapper;

    @Override
    public Page<SysLoginLog> selectPage(Page<SysLoginLog> page, LambdaQueryWrapper<SysLoginLog> wrapper) {
        return sysLoginLogMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysLoginLog> selectList(LambdaQueryWrapper<SysLoginLog> wrapper) {
        return sysLoginLogMapper.selectList(wrapper);
    }

    @Override
    public Long selectCount(LambdaQueryWrapper<SysLoginLog> wrapper) {
        return sysLoginLogMapper.selectCount(wrapper);
    }
}
