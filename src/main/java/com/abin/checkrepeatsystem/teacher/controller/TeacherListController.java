package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.teacher.service.TeacherReviewWorkflowService;
import com.abin.checkrepeatsystem.teacher.vo.StudentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教师列表控制器
 */
@RestController
@RequestMapping("/api/teacher")
public class TeacherListController {

    @Autowired
    private TeacherReviewWorkflowService reviewWorkflowService;

    /**
     * 获取教师列表
     *
     * @return 教师列表
     */
    @GetMapping("/list")
    public Result<List<StudentVO>> getTeachers() {
        return reviewWorkflowService.getTeachers();
    }
}
