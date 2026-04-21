package com.abin.checkrepeatsystem.admin.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * 批量删除请求VO
 */
public class BatchDeleteReq {
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}