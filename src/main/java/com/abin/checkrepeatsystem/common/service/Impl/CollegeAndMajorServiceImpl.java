package com.abin.checkrepeatsystem.common.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.mapper.CollegeMapper;
import com.abin.checkrepeatsystem.common.service.CollegeAndMajorService;
import com.abin.checkrepeatsystem.pojo.entity.College;
import com.abin.checkrepeatsystem.pojo.entity.Major;
import com.abin.checkrepeatsystem.student.mapper.MajorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CollegeAndMajorServiceImpl implements CollegeAndMajorService {

    private static final String CACHE_KEY_COLLEGES = "college:all";
    private static final String CACHE_KEY_MAJORS = "major:all";
    private static final String CACHE_KEY_COLLEGE_MAP = "college:map";
    private static final String CACHE_KEY_MAJOR_MAP = "major:map";
    private static final String CACHE_KEY_TREE = "college:major:tree";

    private static final long CACHE_EXPIRE_HOURS = 24;

    @Autowired
    private CollegeMapper collegeMapper;

    @Autowired
    private MajorMapper majorMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result<List<College>> getAllColleges() {
        try {
            List<College> colleges = getCollegesFromCache();
            return Result.success("学院列表获取成功", colleges);
        } catch (Exception e) {
            log.error("获取学院列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取学院列表失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<College> getCollegesFromCache() {
        if (redisTemplate == null) {
            return getCollegesFromDb();
        }

        try {
            List<College> cached = (List<College>) redisTemplate.opsForValue().get(CACHE_KEY_COLLEGES);
            if (cached != null) {
                log.debug("从缓存获取学院列表: count={}", cached.size());
                return cached;
            }
        } catch (Exception e) {
            log.warn("读取学院缓存失败，使用数据库查询", e);
        }

        List<College> colleges = getCollegesFromDb();
        try {
            redisTemplate.opsForValue().set(CACHE_KEY_COLLEGES, colleges, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("写入学院缓存失败", e);
        }
        return colleges;
    }

    private List<College> getCollegesFromDb() {
        LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(College::getIsDeleted, 0)
               .orderByAsc(College::getCollegeName);
        return collegeMapper.selectList(wrapper);
    }

    @Override
    public Result<Map<String, Object>> getCollegePage(int page, int pageSize, String searchKey) {
        try {
            LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(College::getIsDeleted, 0);

            if (StringUtils.hasText(searchKey)) {
                wrapper.and(w -> w.like(College::getCollegeName, searchKey)
                                  .or()
                                  .like(College::getCollegeCode, searchKey));
            }

            wrapper.orderByAsc(College::getCollegeName);

            Page<College> pageObj = new Page<>(page, pageSize);
            Page<College> resultPage = collegeMapper.selectPage(pageObj, wrapper);

            Map<String, Object> result = new HashMap<>();
            result.put("list", resultPage.getRecords());
            result.put("totalCount", resultPage.getTotal());
            result.put("currentPage", resultPage.getCurrent());
            result.put("totalPages", resultPage.getPages());
            result.put("pageSize", resultPage.getSize());

            return Result.success("学院分页列表获取成功", result);
        } catch (Exception e) {
            log.error("获取学院分页列表失败: page={}, pageSize={}", page, pageSize, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取学院分页列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<Major>> getMajorsByCollegeId(Long collegeId) {
        try {
            if (collegeId == null || collegeId <= 0) {
                return Result.error(ResultCode.PARAM_ERROR, "学院ID不合法");
            }

            List<Major> majors = getMajorsByCollegeIdFromCache(collegeId);
            return Result.success("专业列表获取成功", majors);
        } catch (Exception e) {
            log.error("获取专业列表失败: collegeId={}", collegeId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取专业列表失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Major> getMajorsByCollegeIdFromCache(Long collegeId) {
        String cacheKey = "major:college:" + collegeId;

        if (redisTemplate == null) {
            return getMajorsByCollegeIdFromDb(collegeId);
        }

        try {
            List<Major> cached = (List<Major>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("从缓存获取专业列表: collegeId={}, count={}", collegeId, cached.size());
                return cached;
            }
        } catch (Exception e) {
            log.warn("读取专业缓存失败，使用数据库查询", e);
        }

        List<Major> majors = getMajorsByCollegeIdFromDb(collegeId);
        try {
            redisTemplate.opsForValue().set(cacheKey, majors, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("写入专业缓存失败", e);
        }
        return majors;
    }

    private List<Major> getMajorsByCollegeIdFromDb(Long collegeId) {
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Major::getCollegeId, collegeId)
               .eq(Major::getIsDeleted, 0)
               .orderByAsc(Major::getMajorName);
        return majorMapper.selectList(wrapper);
    }

    @Override
    public Result<List<Major>> getAllMajors() {
        try {
            List<Major> majors = getMajorsFromCache();
            return Result.success("专业列表获取成功", majors);
        } catch (Exception e) {
            log.error("获取专业列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取专业列表失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Major> getMajorsFromCache() {
        if (redisTemplate == null) {
            return getMajorsFromDb();
        }

        try {
            List<Major> cached = (List<Major>) redisTemplate.opsForValue().get(CACHE_KEY_MAJORS);
            if (cached != null) {
                log.debug("从缓存获取专业列表: count={}", cached.size());
                return cached;
            }
        } catch (Exception e) {
            log.warn("读取专业缓存失败，使用数据库查询", e);
        }

        List<Major> majors = getMajorsFromDb();
        try {
            redisTemplate.opsForValue().set(CACHE_KEY_MAJORS, majors, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("写入专业缓存失败", e);
        }
        return majors;
    }

    private List<Major> getMajorsFromDb() {
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Major::getIsDeleted, 0)
               .orderByAsc(Major::getMajorName);
        return majorMapper.selectList(wrapper);
    }

    @Override
    public Result<Map<String, Object>> getMajorPage(int page, int pageSize, Long collegeId, String searchKey) {
        try {
            LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Major::getIsDeleted, 0);

            if (collegeId != null && collegeId > 0) {
                wrapper.eq(Major::getCollegeId, collegeId);
            }

            if (StringUtils.hasText(searchKey)) {
                wrapper.and(w -> w.like(Major::getMajorName, searchKey)
                                  .or()
                                  .like(Major::getMajorCode, searchKey));
            }

            wrapper.orderByAsc(Major::getMajorName);

            Page<Major> pageObj = new Page<>(page, pageSize);
            Page<Major> resultPage = majorMapper.selectPage(pageObj, wrapper);

            Map<String, Object> result = new HashMap<>();
            result.put("list", resultPage.getRecords());
            result.put("totalCount", resultPage.getTotal());
            result.put("currentPage", resultPage.getCurrent());
            result.put("totalPages", resultPage.getPages());
            result.put("pageSize", resultPage.getSize());

            return Result.success("专业分页列表获取成功", result);
        } catch (Exception e) {
            log.error("获取专业分页列表失败: page={}, pageSize={}", page, pageSize, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取专业分页列表失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result<List<Map<String, Object>>> getCollegeMajorTree() {
        try {
            if (redisTemplate != null) {
                try {
                    List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(CACHE_KEY_TREE);
                    if (cached != null) {
                        log.debug("从缓存获取学院专业树形结构");
                        return Result.success("树形结构获取成功", cached);
                    }
                } catch (Exception e) {
                    log.warn("读取树形结构缓存失败，使用数据库查询", e);
                }
            }

            List<College> colleges = getCollegesFromDb();
            List<Major> allMajors = getMajorsFromDb();

            Map<Long, List<Major>> majorsByCollege = allMajors.stream()
                    .collect(Collectors.groupingBy(Major::getCollegeId));

            List<Map<String, Object>> tree = colleges.stream().map(college -> {
                Map<String, Object> node = new HashMap<>();
                node.put("id", college.getId());
                node.put("name", college.getCollegeName());
                node.put("code", college.getCollegeCode());
                node.put("type", "college");

                List<Map<String, Object>> majorNodes = majorsByCollege.getOrDefault(college.getId(), Collections.emptyList())
                        .stream()
                        .map(major -> {
                            Map<String, Object> majorNode = new HashMap<>();
                            majorNode.put("id", major.getId());
                            majorNode.put("name", major.getMajorName());
                            majorNode.put("code", major.getMajorCode());
                            majorNode.put("type", "major");
                            majorNode.put("collegeId", major.getCollegeId());
                            return majorNode;
                        })
                        .collect(Collectors.toList());

                node.put("children", majorNodes);
                node.put("majorCount", majorNodes.size());
                return node;
            }).collect(Collectors.toList());

            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(CACHE_KEY_TREE, tree, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("写入树形结构缓存失败", e);
                }
            }

            return Result.success("树形结构获取成功", tree);
        } catch (Exception e) {
            log.error("获取学院专业树形结构失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取树形结构失败: " + e.getMessage());
        }
    }

    @Override
    public Result<College> getCollegeByName(String collegeName) {
        try {
            if (!StringUtils.hasText(collegeName)) {
                return Result.error(ResultCode.PARAM_ERROR, "学院名称不能为空");
            }

            LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(College::getCollegeName, collegeName.trim())
                   .eq(College::getIsDeleted, 0);

            College college = collegeMapper.selectOne(wrapper);
            if (college == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "学院不存在");
            }

            return Result.success("学院信息获取成功", college);
        } catch (Exception e) {
            log.error("获取学院信息失败: collegeName={}", collegeName, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取学院信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Major> getMajorByName(String majorName) {
        try {
            if (!StringUtils.hasText(majorName)) {
                return Result.error(ResultCode.PARAM_ERROR, "专业名称不能为空");
            }

            LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Major::getMajorName, majorName.trim())
                   .eq(Major::getIsDeleted, 0);

            Major major = majorMapper.selectOne(wrapper);
            if (major == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "专业不存在");
            }

            return Result.success("专业信息获取成功", major);
        } catch (Exception e) {
            log.error("获取专业信息失败: majorName={}", majorName, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取专业信息失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isValidCollegeId(Long collegeId) {
        if (collegeId == null || collegeId <= 0) {
            return false;
        }
        College college = collegeMapper.selectById(collegeId);
        return college != null && college.getIsDeleted() == 0;
    }

    @Override
    public boolean isValidMajorId(Long majorId) {
        if (majorId == null || majorId <= 0) {
            return false;
        }
        Major major = majorMapper.selectById(majorId);
        return major != null && major.getIsDeleted() == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result<Map<Long, String>> getCollegeIdNameMap() {
        try {
            if (redisTemplate != null) {
                try {
                    Map<Long, String> cached = (Map<Long, String>) redisTemplate.opsForValue().get(CACHE_KEY_COLLEGE_MAP);
                    if (cached != null) {
                        return Result.success("学院ID名称映射获取成功", cached);
                    }
                } catch (Exception e) {
                    log.warn("读取学院映射缓存失败", e);
                }
            }

            List<College> colleges = getCollegesFromDb();
            Map<Long, String> map = colleges.stream()
                    .collect(Collectors.toMap(College::getId, College::getCollegeName, (a, b) -> a));

            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(CACHE_KEY_COLLEGE_MAP, map, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("写入学院映射缓存失败", e);
                }
            }

            return Result.success("学院ID名称映射获取成功", map);
        } catch (Exception e) {
            log.error("获取学院ID名称映射失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取学院映射失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result<Map<Long, String>> getMajorIdNameMap() {
        try {
            if (redisTemplate != null) {
                try {
                    Map<Long, String> cached = (Map<Long, String>) redisTemplate.opsForValue().get(CACHE_KEY_MAJOR_MAP);
                    if (cached != null) {
                        return Result.success("专业ID名称映射获取成功", cached);
                    }
                } catch (Exception e) {
                    log.warn("读取专业映射缓存失败", e);
                }
            }

            List<Major> majors = getMajorsFromDb();
            Map<Long, String> map = majors.stream()
                    .collect(Collectors.toMap(Major::getId, Major::getMajorName, (a, b) -> a));

            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(CACHE_KEY_MAJOR_MAP, map, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("写入专业映射缓存失败", e);
                }
            }

            return Result.success("专业ID名称映射获取成功", map);
        } catch (Exception e) {
            log.error("获取专业ID名称映射失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取专业映射失败: " + e.getMessage());
        }
    }

    @Override
    public void clearCollegeCache() {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(CACHE_KEY_COLLEGES);
                redisTemplate.delete(CACHE_KEY_COLLEGE_MAP);
                redisTemplate.delete(CACHE_KEY_TREE);
                Set<String> keys = redisTemplate.keys("major:college:*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
                log.info("学院缓存已清除");
            } catch (Exception e) {
                log.error("清除学院缓存失败", e);
            }
        }
    }

    @Override
    public void clearMajorCache() {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(CACHE_KEY_MAJORS);
                redisTemplate.delete(CACHE_KEY_MAJOR_MAP);
                redisTemplate.delete(CACHE_KEY_TREE);
                Set<String> keys = redisTemplate.keys("major:college:*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
                log.info("专业缓存已清除");
            } catch (Exception e) {
                log.error("清除专业缓存失败", e);
            }
        }
    }

    @Override
    public void clearAllCache() {
        clearCollegeCache();
        clearMajorCache();
        log.info("所有学院专业缓存已清除");
    }
}