package com.abin.checkrepeatsystem.teacher.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.pojo.entity.CategorySimilarityThreshold;
import com.abin.checkrepeatsystem.pojo.entity.SimilarityThreshold;
import com.abin.checkrepeatsystem.pojo.entity.College;
import com.abin.checkrepeatsystem.pojo.entity.Major;
import com.abin.checkrepeatsystem.teacher.dto.CategorySimilarityThresholdDTO;
import com.abin.checkrepeatsystem.teacher.dto.SimilarityThresholdDTO;
import com.abin.checkrepeatsystem.teacher.mapper.CategorySimilarityThresholdMapper;
import com.abin.checkrepeatsystem.teacher.mapper.SimilarityThresholdMapper;
import com.abin.checkrepeatsystem.teacher.service.TeacherSimilarityThresholdService;
import com.abin.checkrepeatsystem.teacher.vo.CategorySimilarityThresholdVO;
import com.abin.checkrepeatsystem.teacher.vo.SimilarityThresholdVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 相似度阈值设置服务实现
 */
@Service
public class TeacherSimilarityThresholdServiceImpl implements TeacherSimilarityThresholdService {

    @Autowired
    private SimilarityThresholdMapper similarityThresholdMapper;

    @Autowired
    private CategorySimilarityThresholdMapper categorySimilarityThresholdMapper;

    @Autowired
    private BaseMapper<College> collegeMapper;

    @Autowired
    private BaseMapper<Major> majorMapper;

    /**
     * 获取相似度阈值设置
     *
     * @return 相似度阈值设置
     */
    @Override
    public Result<SimilarityThresholdVO> getThresholds() {
        try {
            // 查询全局阈值
            SimilarityThreshold globalThreshold = similarityThresholdMapper.selectById(1L);
            Integer globalThresholdValue = 30; // 默认值
            if (globalThreshold != null) {
                globalThresholdValue = globalThreshold.getGlobalThreshold();
            }

            // 查询分类阈值
            List<CategorySimilarityThreshold> categoryThresholds = categorySimilarityThresholdMapper.selectList(null);
            List<CategorySimilarityThresholdVO> categoryThresholdVOs = new ArrayList<>();

            for (CategorySimilarityThreshold threshold : categoryThresholds) {
                CategorySimilarityThresholdVO vo = new CategorySimilarityThresholdVO();
                BeanUtils.copyProperties(threshold, vo);

                // 填充分类名称
                if ("college".equals(threshold.getCategoryType())) {
                    College college = collegeMapper.selectById(threshold.getCollegeId());
                    if (college != null) {
                        vo.setCategoryName(college.getCollegeName());
                    }
                } else if ("major".equals(threshold.getCategoryType())) {
                    Major major = majorMapper.selectById(threshold.getMajorId());
                    if (major != null) {
                        vo.setCategoryName(major.getMajorName());
                    }
                }

                categoryThresholdVOs.add(vo);
            }

            // 构建返回对象
            SimilarityThresholdVO vo = new SimilarityThresholdVO();
            vo.setGlobalThreshold(globalThresholdValue);
            vo.setCategoryThresholds(categoryThresholdVOs);

            return Result.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"获取阈值设置失败");
        }
    }

    /**
     * 更新相似度阈值设置
     *
     * @param thresholdDTO 相似度阈值设置DTO
     * @return 更新结果
     */
    @Override
    public Result<Void> updateThresholds(SimilarityThresholdDTO thresholdDTO) {
        try {
            // 获取当前用户ID
            Long userId = UserBusinessInfoUtils.getCurrentUserId();
            LocalDateTime now = LocalDateTime.now();

            // 更新全局阈值
            SimilarityThreshold globalThreshold = similarityThresholdMapper.selectById(1L);
            if (globalThreshold == null) {
                globalThreshold = new SimilarityThreshold();
                globalThreshold.setId(1L);
                globalThreshold.setCreateTime(now);
                globalThreshold.setCreateBy(userId);
            }
            globalThreshold.setGlobalThreshold(thresholdDTO.getGlobalThreshold());
            globalThreshold.setUpdateTime(now);
            globalThreshold.setUpdateBy(userId);

            if (globalThreshold.getId() == 1L && similarityThresholdMapper.selectById(1L) != null) {
                similarityThresholdMapper.updateById(globalThreshold);
            } else {
                similarityThresholdMapper.insert(globalThreshold);
            }

            // 更新分类阈值
            // 先删除所有现有分类阈值
            categorySimilarityThresholdMapper.delete(null);

            // 插入新的分类阈值
            if (thresholdDTO.getCategoryThresholds() != null) {
                for (CategorySimilarityThresholdDTO dto : thresholdDTO.getCategoryThresholds()) {
                    CategorySimilarityThreshold threshold = new CategorySimilarityThreshold();
                    BeanUtils.copyProperties(dto, threshold);
                    threshold.setCreateTime(now);
                    threshold.setUpdateTime(now);
                    threshold.setCreateBy(userId);
                    threshold.setUpdateBy(userId);
                    categorySimilarityThresholdMapper.insert(threshold);
                }
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.SYSTEM_ERROR,"更新阈值设置失败");
        }
    }
}
