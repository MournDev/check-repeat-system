package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.common.utils.FileMimeTypeUtils;
import com.abin.checkrepeatsystem.mapper.FileInfoMapper;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.FileInfo;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.PaperSubmit;
import com.abin.checkrepeatsystem.admin.mapper.PaperSubmitMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.student.dto.PaperSubmitDTO;
import com.abin.checkrepeatsystem.student.dto.PaperVersionDTO;
import com.abin.checkrepeatsystem.student.dto.VersionCompareResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaperVersionServiceImpl {

    private final PaperInfoMapper paperInfoMapper;
    private final PaperSubmitMapper paperSubmitMapper;
    private final CheckTaskMapper checkTaskMapper;
    private final FileInfoMapper fileInfoMapper;
    private final FileService fileService;

    @Value("${file.upload.base-path}")
    private String uploadBasePath;

    public List<PaperSubmitDTO> getPaperVersions(Long paperId, Long studentId) {
        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "论文不存在");
        }
        if (!paperInfo.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限访问该论文");
        }
        List<PaperSubmit> submits = paperSubmitMapper.selectList(
            new LambdaQueryWrapper<PaperSubmit>()
                .eq(PaperSubmit::getPaperId, paperId)
                .eq(PaperSubmit::getIsDeleted, 0)
                .orderByAsc(PaperSubmit::getSubmitVersion)
        );
        return submits.stream().map(s -> {
            PaperSubmitDTO dto = new PaperSubmitDTO();
            dto.setId(s.getId());
            dto.setSubmitVersion(s.getSubmitVersion());
            dto.setFileId(s.getFileId());
            dto.setSubmitTime(s.getSubmitTime());
            dto.setRemark(s.getRemark());
            return dto;
        }).collect(Collectors.toList());
    }

    public PaperVersionDTO getPaperVersion(Long paperId, Long versionId, Long studentId) {
        log.info("获取论文版本详情 - 论文ID: {}, 版本ID: {}, 学生ID: {}", paperId, versionId, studentId);

        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "论文不存在");
        }
        if (!paperInfo.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限访问该论文");
        }

        PaperSubmit paperSubmit = paperSubmitMapper.selectOne(
            new LambdaQueryWrapper<PaperSubmit>()
                .eq(PaperSubmit::getPaperId, paperId)
                .eq(PaperSubmit::getSubmitVersion, versionId.intValue())
                .last("LIMIT 1")
        );
        if (paperSubmit == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "版本" + versionId + "不存在");
        }

        PaperVersionDTO versionDTO = new PaperVersionDTO();
        versionDTO.setId(paperSubmit.getId());
        versionDTO.setPaperId(paperSubmit.getPaperId());
        versionDTO.setVersion(paperSubmit.getSubmitVersion());
        versionDTO.setSubmitTime(paperSubmit.getSubmitTime());
        versionDTO.setFileId(paperSubmit.getFileId());

        PaperSubmit latestSubmit = paperSubmitMapper.selectOne(
            new LambdaQueryWrapper<PaperSubmit>()
                .eq(PaperSubmit::getPaperId, paperId)
                .orderByDesc(PaperSubmit::getSubmitVersion)
                .last("LIMIT 1")
        );
        versionDTO.setIsCurrent(latestSubmit != null && latestSubmit.getSubmitVersion().equals(versionId.intValue()));

        CheckTask latestCheckTask = checkTaskMapper.selectOne(
            new LambdaQueryWrapper<CheckTask>()
                .eq(CheckTask::getPaperId, paperId)
                .eq(CheckTask::getCheckStatus, DictConstants.CheckStatus.COMPLETED)
                .eq(CheckTask::getIsDeleted, 0)
                .orderByDesc(CheckTask::getEndTime)
                .last("LIMIT 1")
        );
        if (latestCheckTask != null && latestCheckTask.getCheckRate() != null) {
            versionDTO.setSimilarityRate(latestCheckTask.getCheckRate());
        } else {
            versionDTO.setSimilarityRate(paperInfo.getSimilarityRate());
        }

        FileInfo fileInfo = fileInfoMapper.selectById(paperSubmit.getFileId());
        versionDTO.setWordCount(fileInfo != null ? fileInfo.getWordCount() : 0);

        return versionDTO;
    }

    public VersionCompareResult comparePaperVersions(Long paperId, List<Long> versionIds, Long studentId) {
        log.info("开始版本对比 - 论文ID: {}, 版本数量: {}, 学生ID: {}", paperId, versionIds.size(), studentId);

        if (versionIds.size() != 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "必须选择两个版本进行对比");
        }

        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "论文不存在");
        }
        if (!paperInfo.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限访问该论文");
        }

        PaperSubmit versionA = findSubmitByVersion(paperId, versionIds.get(0).intValue());
        PaperSubmit versionB = findSubmitByVersion(paperId, versionIds.get(1).intValue());

        if (versionA == null || versionB == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "版本信息不正确: 版本" + versionIds.get(0) + "或" + versionIds.get(1) + "不存在");
        }

        VersionCompareResult result = new VersionCompareResult();

        VersionCompareResult.VersionInfo infoA = new VersionCompareResult.VersionInfo();
        infoA.setId(versionA.getId());
        infoA.setVersion(versionA.getSubmitVersion());
        CheckTask checkTaskA = checkTaskMapper.selectOne(
            new LambdaQueryWrapper<CheckTask>()
                .eq(CheckTask::getPaperId, paperId)
                .eq(CheckTask::getCheckStatus, DictConstants.CheckStatus.COMPLETED)
                .eq(CheckTask::getIsDeleted, 0)
                .le(CheckTask::getEndTime, versionA.getSubmitTime())
                .orderByDesc(CheckTask::getEndTime)
                .last("LIMIT 1")
        );
        if (checkTaskA != null && checkTaskA.getCheckRate() != null) {
            infoA.setSimilarityRate(checkTaskA.getCheckRate());
        } else {
            infoA.setSimilarityRate(paperInfo.getSimilarityRate());
        }
        FileInfo fileInfoA = fileInfoMapper.selectById(versionA.getFileId());
        infoA.setWordCount(fileInfoA != null ? fileInfoA.getWordCount() : 0);
        result.setVersionA(infoA);

        VersionCompareResult.VersionInfo infoB = new VersionCompareResult.VersionInfo();
        infoB.setId(versionB.getId());
        infoB.setVersion(versionB.getSubmitVersion());
        CheckTask checkTaskB = checkTaskMapper.selectOne(
            new LambdaQueryWrapper<CheckTask>()
                .eq(CheckTask::getPaperId, paperId)
                .eq(CheckTask::getCheckStatus, DictConstants.CheckStatus.COMPLETED)
                .eq(CheckTask::getIsDeleted, 0)
                .le(CheckTask::getEndTime, versionB.getSubmitTime())
                .orderByDesc(CheckTask::getEndTime)
                .last("LIMIT 1")
        );
        if (checkTaskB != null && checkTaskB.getCheckRate() != null) {
            infoB.setSimilarityRate(checkTaskB.getCheckRate());
        } else {
            infoB.setSimilarityRate(paperInfo.getSimilarityRate());
        }
        FileInfo fileInfoB = fileInfoMapper.selectById(versionB.getFileId());
        infoB.setWordCount(fileInfoB != null ? fileInfoB.getWordCount() : 0);
        result.setVersionB(infoB);

        List<VersionCompareResult.DiffItem> diffItems = new ArrayList<>();

        VersionCompareResult.DiffItem similarityDiff = new VersionCompareResult.DiffItem();
        similarityDiff.setField("相似度");
        similarityDiff.setBefore(infoA.getSimilarityRate());
        similarityDiff.setAfter(infoB.getSimilarityRate());
        if (infoA.getSimilarityRate() != null && infoB.getSimilarityRate() != null) {
            similarityDiff.setChange(infoB.getSimilarityRate().subtract(infoA.getSimilarityRate()));
        }
        diffItems.add(similarityDiff);

        VersionCompareResult.DiffItem wordCountDiff = new VersionCompareResult.DiffItem();
        wordCountDiff.setField("字数");
        wordCountDiff.setBefore(infoA.getWordCount());
        wordCountDiff.setAfter(infoB.getWordCount());
        wordCountDiff.setChange(infoB.getWordCount() - infoA.getWordCount());
        diffItems.add(wordCountDiff);

        result.setDiffData(diffItems);

        log.info("版本对比完成 - 论文ID: {}", paperId);
        return result;
    }

    public void downloadVersionCompareReport(Long paperId, List<Long> versionIds, Long studentId, HttpServletResponse response) {
        log.info("开始下载版本对比报告 - 论文ID: {}, 学生ID: {}", paperId, studentId);

        VersionCompareResult compareResult = comparePaperVersions(paperId, versionIds, studentId);

        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=compare_report_" +
            System.currentTimeMillis() + ".json");

        try {
            String jsonReport = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(compareResult);
            response.getWriter().write(jsonReport);
            response.getWriter().flush();
            log.info("版本对比报告下载完成 - 论文ID: {}", paperId);
        } catch (Exception e) {
            log.error("下载版本对比报告失败 - 论文ID: {}", paperId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "下载对比报告失败: " + e.getMessage());
        }
    }

    public void downloadPaperVersion(Long versionId, Long studentId, HttpServletResponse response) {
        log.info("开始下载论文版本 - 版本ID: {}, 学生ID: {}", versionId, studentId);

        PaperSubmit paperSubmit = paperSubmitMapper.selectById(versionId);
        if (paperSubmit == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "版本不存在");
        }

        PaperInfo paperInfo = paperInfoMapper.selectById(paperSubmit.getPaperId());
        if (paperInfo == null || !paperInfo.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限下载此版本");
        }

        FileInfo fileInfo = fileService.getById(paperSubmit.getFileId());
        if (fileInfo != null && StringUtils.hasText(fileInfo.getStoragePath())) {
            String fullPath = Paths.get(uploadBasePath, fileInfo.getStoragePath()).toString();
            File file = new File(fullPath);

            if (file.exists()) {
                String fileName = fileInfo.getOriginalFilename() != null ?
                    fileInfo.getOriginalFilename() : "paper_version_" + versionId + ".pdf";
                response.setContentType(FileMimeTypeUtils.getContentType(fileName));
                response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
                response.setContentLengthLong(file.length());

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        response.getOutputStream().write(buffer, 0, len);
                    }
                    response.getOutputStream().flush();
                } catch (Exception e) {
                    throw new BusinessException(ResultCode.SYSTEM_ERROR, "读取文件失败: " + e.getMessage());
                }
            } else {
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "文件不存在");
            }
        } else {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "文件信息不存在");
        }

        log.info("论文版本下载完成 - 版本ID: {}", versionId);
    }

    PaperSubmit findSubmitByVersion(Long paperId, int versionNumber) {
        PaperSubmit submit = paperSubmitMapper.selectOne(
            new LambdaQueryWrapper<PaperSubmit>()
                .eq(PaperSubmit::getPaperId, paperId)
                .eq(PaperSubmit::getSubmitVersion, versionNumber)
                .last("LIMIT 1")
        );
        if (submit != null) {
            return submit;
        }

        List<PaperSubmit> allSubmits = paperSubmitMapper.selectList(
            new LambdaQueryWrapper<PaperSubmit>()
                .eq(PaperSubmit::getPaperId, paperId)
                .eq(PaperSubmit::getIsDeleted, 0)
                .orderByAsc(PaperSubmit::getSubmitVersion)
        );
        int index = versionNumber - 1;
        if (index >= 0 && index < allSubmits.size()) {
            return allSubmits.get(index);
        }

        return null;
    }
}
