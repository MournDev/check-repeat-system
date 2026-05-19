package com.abin.checkrepeatsystem.student.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 查重任务创建事件
 * 用于解耦任务创建和任务执行
 */
@Getter
public class CheckTaskCreatedEvent extends ApplicationEvent {
    
    /**
     * 查重任务 ID
     */
    private final Long taskId;

    /**
     * 论文 ID
     */
    private final Long paperId;

    /**
     * 操作人用户ID（用于异步线程中传递用户上下文）
     */
    private final Long operatorUserId;

    public CheckTaskCreatedEvent(Object source, Long taskId, Long paperId, Long operatorUserId) {
        super(source);
        this.taskId = taskId;
        this.paperId = paperId;
        this.operatorUserId = operatorUserId;
    }
}
