package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.Major;
import com.abin.checkrepeatsystem.student.service.MajorService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/majors")
public class MajorController {

    @Resource
    private MajorService majorService;

    /**
     * 根据学院ID查询专业接口
     *
     * @param collegeId 学院ID
     * @return 该学院下的专业列表
     */
    @GetMapping("/all")
    public Result<List<Major>> getMajorsByCollegeId(@RequestParam Long collegeId) {
        List<Major> majors = majorService.getMajorsByCollegeId(collegeId);
        return Result.success("专业列表查询成功", majors);
    }
}
