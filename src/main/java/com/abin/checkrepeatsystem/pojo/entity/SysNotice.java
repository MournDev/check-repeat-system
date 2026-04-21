package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统站内信实体类（对应sys_notice表）
 */
@Data
@TableName("sys_notice")
public class SysNotice {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收用户ID（关联sys_user.id）
     */
    private Long userId;

    /**
     * 通知类型：1-提交成功，2-查重完成，3-审核结果，4-截止提醒，5-系统通知，6-教师分配，7-论文修改请求，8-其他
     */
    private Integer noticeType;

    /**
     * 通知标题
     */
    private String noticeTitle;

    /**
     * 通知内容
     */
    private String noticeContent;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 优先级：0-普通，1-重要，2-紧急
     */
    private Integer priority;

    /**
     * 相关业务ID（如论文ID、任务ID等）
     */
    private Long relatedId;

    /**
     * 相关业务类型（如paper、task等）
     */
    private String relatedType;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 软删除：0-未删除，1-已删除（自动填充）
     */
    @TableLogic
    private Integer isDeleted;
    
    /**
     * 用户姓名（冗余字段，便于显示）
     */
    @TableField(exist = false)
    private String userName;
    
    /**
     * 用户学号/工号（冗余字段，便于显示）
     */
    @TableField(exist = false)
    private String userNo;
    
    /**
     * 通知类型描述（冗余字段，便于显示）
     */
    @TableField(exist = false)
    private String noticeTypeDesc;
    
    /**
     * 优先级描述（冗余字段，便于显示）
     */
    @TableField(exist = false)
    private String priorityDesc;
    
    /**
     * 创建时间文本（冗余字段，便于显示）
     */
    @TableField(exist = false)
    private String createTimeText;
    
    /**
     * 阅读状态文本（冗余字段，便于显示）
     */
    @TableField(exist = false)
    private String readStatusText;
}
