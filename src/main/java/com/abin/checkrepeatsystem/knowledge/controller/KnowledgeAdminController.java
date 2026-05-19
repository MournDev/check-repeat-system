package com.abin.checkrepeatsystem.knowledge.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.knowledge.dto.PageDTO;
import com.abin.checkrepeatsystem.knowledge.service.KnowledgeArticleService;
import com.abin.checkrepeatsystem.pojo.entity.KnowledgeArticle;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/knowledge/admin")
public class KnowledgeAdminController {

    @Resource
    private KnowledgeArticleService knowledgeArticleService;

    @GetMapping("/articles")
    public Result<PageDTO<KnowledgeArticle>> listArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            PageDTO<KnowledgeArticle> result = knowledgeArticleService.adminListArticles(page, size, keyword, status);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取文章管理列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取文章列表失败");
        }
    }

    @PostMapping("/articles")
    public Result<KnowledgeArticle> createArticle(@RequestBody KnowledgeArticle article) {
        try {
            KnowledgeArticle created = knowledgeArticleService.createArticle(article);
            return Result.success("创建成功", created);
        } catch (Exception e) {
            log.error("创建文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "创建文章失败");
        }
    }

    @PutMapping("/articles/{id}")
    public Result<KnowledgeArticle> updateArticle(@PathVariable Long id, @RequestBody KnowledgeArticle article) {
        try {
            KnowledgeArticle updated = knowledgeArticleService.updateArticle(id, article);
            return Result.success("更新成功", updated);
        } catch (Exception e) {
            log.error("更新文章失败 - ID: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "更新文章失败");
        }
    }

    @DeleteMapping("/articles/{id}")
    public Result<String> deleteArticle(@PathVariable Long id) {
        try {
            boolean success = knowledgeArticleService.deleteArticle(id);
            if (success) {
                return Result.success("删除成功");
            }
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "文章不存在");
        } catch (Exception e) {
            log.error("删除文章失败 - ID: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "删除文章失败");
        }
    }

    @PutMapping("/articles/{id}/status")
    public Result<String> updateArticleStatus(@PathVariable Long id, @RequestBody KnowledgeArticle body) {
        try {
            String status = body.getStatus();
            boolean success = knowledgeArticleService.updateStatus(id, status);
            if (success) {
                return Result.success("状态更新成功");
            }
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "文章不存在");
        } catch (Exception e) {
            log.error("更新文章状态失败 - ID: {}, status: {}", id, body.getStatus(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "更新文章状态失败");
        }
    }
}