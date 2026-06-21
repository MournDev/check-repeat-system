package com.abin.checkrepeatsystem.common.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.mapper.CollegeMapper;
import com.abin.checkrepeatsystem.common.service.CollegeAndMajorService;
import com.abin.checkrepeatsystem.pojo.entity.College;
import com.abin.checkrepeatsystem.pojo.entity.Major;
import com.abin.checkrepeatsystem.student.mapper.MajorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class CollegeAndMajorServiceImpl implements CollegeAndMajorService {

    private static final String CACHE_KEY_COLLEGES = "college:all";
    private static final String CACHE_KEY_MAJORS = "major:all";
    private static final String CACHE_KEY_COLLEGE_MAP = "college:map";
    private static final String CACHE_KEY_MAJOR_MAP = "major:map";
    private static final String CACHE_KEY_TREE = "college:major:tree";

    private static final long CACHE_EXPIRE_HOURS = 24;

    private final CollegeMapper collegeMapper;
    private final MajorMapper majorMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    @Override
    public Result<List<College>> getAllColleges() {
        List<College> colleges = getCollegesFromCache();
        return Result.success("学院列表获取成功", colleges);
    }

    @SuppressWarnings("unchecked")
    private List<College> getCollegesFromCache() {
        return withCacheLock(CACHE_KEY_COLLEGES, this::getCollegesFromDb);
    }

    private List<College> getCollegesFromDb() {
        LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(College::getIsDeleted, 0)
               .orderByAsc(College::getCollegeName);
        return collegeMapper.selectList(wrapper);
    }

    @Override
    public Result<Map<String, Object>> getCollegePage(int page, int pageSize, String searchKey) {
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
    }

    @Override
    public Result<List<Major>> getMajorsByCollegeId(Long collegeId) {
        if (collegeId == null || collegeId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学院ID不合法");
        }

        List<Major> majors = getMajorsByCollegeIdFromCache(collegeId);
        return Result.success("专业列表获取成功", majors);
    }

    @SuppressWarnings("unchecked")
    private List<Major> getMajorsByCollegeIdFromCache(Long collegeId) {
        String cacheKey = "major:college:" + collegeId;
        return withCacheLock(cacheKey, () -> getMajorsByCollegeIdFromDb(collegeId));
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
        List<Major> majors = getMajorsFromCache();
        return Result.success("专业列表获取成功", majors);
    }

    @SuppressWarnings("unchecked")
    private List<Major> getMajorsFromCache() {
        return withCacheLock(CACHE_KEY_MAJORS, this::getMajorsFromDb);
    }

    private List<Major> getMajorsFromDb() {
        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Major::getIsDeleted, 0)
               .orderByAsc(Major::getMajorName);
        return majorMapper.selectList(wrapper);
    }

    @Override
    public Result<Map<String, Object>> getMajorPage(int page, int pageSize, Long collegeId, String searchKey) {
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result<List<Map<String, Object>>> getCollegeMajorTree() {
        List<Map<String, Object>> tree = withCacheLock(CACHE_KEY_TREE, () -> {
            List<College> colleges = getCollegesFromDb();
            List<Major> allMajors = getMajorsFromDb();

            Map<Long, List<Major>> majorsByCollege = allMajors.stream()
                    .collect(Collectors.groupingBy(Major::getCollegeId));

            return colleges.stream().map(college -> {
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
        });

        return Result.success("树形结构获取成功", tree);
    }

    @Override
    public Result<College> getCollegeByName(String collegeName) {
        if (!StringUtils.hasText(collegeName)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学院名称不能为空");
        }

        LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(College::getCollegeName, collegeName.trim())
               .eq(College::getIsDeleted, 0);

        College college = collegeMapper.selectOne(wrapper);
        if (college == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "学院不存在");
        }

        return Result.success("学院信息获取成功", college);
    }

    @Override
    public Result<Major> getMajorByName(String majorName) {
        if (!StringUtils.hasText(majorName)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "专业名称不能为空");
        }

        LambdaQueryWrapper<Major> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Major::getMajorName, majorName.trim())
               .eq(Major::getIsDeleted, 0);

        Major major = majorMapper.selectOne(wrapper);
        if (major == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "专业不存在");
        }

        return Result.success("专业信息获取成功", major);
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
        Map<Long, String> map = withCacheLock(CACHE_KEY_COLLEGE_MAP, () -> {
            List<College> colleges = getCollegesFromDb();
            return colleges.stream()
                    .collect(Collectors.toMap(College::getId, College::getCollegeName, (a, b) -> a));
        });
        return Result.success("学院ID名称映射获取成功", map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result<Map<Long, String>> getMajorIdNameMap() {
        Map<Long, String> map = withCacheLock(CACHE_KEY_MAJOR_MAP, () -> {
            List<Major> majors = getMajorsFromDb();
            return majors.stream()
                    .collect(Collectors.toMap(Major::getId, Major::getMajorName, (a, b) -> a));
        });
        return Result.success("专业ID名称映射获取成功", map);
    }

    /**
     * 分布式锁保护的缓存读取：防止缓存击穿（thundering herd）
     */
    @SuppressWarnings("unchecked")
    private <T> T withCacheLock(String cacheKey, Supplier<T> dbLoader) {
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return (T) cached;
                }
            } catch (Exception e) {
                log.warn("读取缓存失败: cacheKey={}", cacheKey, e);
            }
        }

        RLock lock = redissonClient.getLock("lock:cache:" + cacheKey);
        try {
            lock.lock();
            if (redisTemplate != null) {
                try {
                    Object cached = redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null) {
                        return (T) cached;
                    }
                } catch (Exception e) {
                    log.warn("双重检查读取缓存失败: cacheKey={}", cacheKey, e);
                }
            }

            T result = dbLoader.get();

            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("写入缓存失败: cacheKey={}", cacheKey, e);
                }
            }

            return result;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
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