package com.abin.checkrepeatsystem.user.mapper;

import com.abin.checkrepeatsystem.pojo.entity.TeacherInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 教师信息Mapper
 */
@Mapper
public interface TeacherInfoMapper extends BaseMapper<TeacherInfo> {

    /**
     * 原子递增教师当前指导学生数，避免并发竞态
     */
    @Update("UPDATE teacher_info SET current_advisor_count = current_advisor_count + 1 WHERE user_id = #{userId} AND is_deleted = 0")
    int incrementAdvisorCount(@Param("userId") Long userId);
}
