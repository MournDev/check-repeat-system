package com.abin.checkrepeatsystem.common.utils;

import com.abin.checkrepeatsystem.pojo.entity.SysUser;

/**
 * 用户上下文持有者：用于在异步线程中传递用户信息
 */
public class UserContextHolder {
    
    private static final ThreadLocal<SysUser> userThreadLocal = new ThreadLocal<>();
    
    /**
     * 设置当前线程的用户信息
     */
    public static void setUser(SysUser user) {
        userThreadLocal.set(user);
    }
    
    /**
     * 获取当前线程的用户信息
     */
    public static SysUser getUser() {
        return userThreadLocal.get();
    }
    
    /**
     * 移除当前线程的用户信息
     */
    public static void removeUser() {
        userThreadLocal.remove();
    }
    
    /**
     * 获取当前线程的用户ID，未设置用户上下文时抛出异常
     */
    public static Long getUserId() {
        SysUser user = getUser();
        if (user == null) {
            throw new IllegalStateException("用户上下文未设置，无法获取用户ID。请确保在认证流程中正确设置了UserContextHolder。");
        }
        return user.getId();
    }
}