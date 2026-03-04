package com.abin.checkrepeatsystem;

import com.abin.checkrepeatsystem.pojo.entity.SysLoginLog;
import com.abin.checkrepeatsystem.user.mapper.SysLoginLogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnit4ClassRunner;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class SysLoginLogTest {

    @Resource
    private SysLoginLogMapper sysLoginLogMapper;

    @Test
    public void testLoginLogQuery() {
        try {
            // 测试查询今日登录次数
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            
            Long todayLogins = sysLoginLogMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysLoginLog>()
                    .ge(SysLoginLog::getLoginTime, todayStart)
            );
            
            System.out.println("今日登录次数: " + todayLogins);
            
            // 测试查询最近的登录记录
            List<SysLoginLog> recentLogins = sysLoginLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysLoginLog>()
                    .orderByDesc(SysLoginLog::getLoginTime)
                    .last("LIMIT 5")
            );
            
            System.out.println("最近5条登录记录:");
            recentLogins.forEach(log -> {
                System.out.println("用户: " + log.getUsername() + 
                                 ", 时间: " + log.getLoginTime() + 
                                 ", 结果: " + (log.getLoginResult() == 1 ? "成功" : "失败"));
            });
            
        } catch (Exception e) {
            System.err.println("测试登录日志查询失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}