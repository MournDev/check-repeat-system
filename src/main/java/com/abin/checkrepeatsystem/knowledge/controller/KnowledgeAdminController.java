package com.abin.checkrepeatsystem.knowledge.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.knowledge.dto.PageDTO;
import com.abin.checkrepeatsystem.knowledge.service.KnowledgeArticleService;
import com.abin.checkrepeatsystem.pojo.entity.KnowledgeArticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge/admin")
public class KnowledgeAdminController {

    private final KnowledgeArticleService knowledgeArticleService;

    @GetMapping("/articles")
    public Result<PageDTO<KnowledgeArticle>> listArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        PageDTO<KnowledgeArticle> result = knowledgeArticleService.adminListArticles(page, size, keyword, status);
        return Result.success(result);
    }

    @PostMapping("/articles")
    public Result<KnowledgeArticle> createArticle(@RequestBody KnowledgeArticle article) {
        KnowledgeArticle created = knowledgeArticleService.createArticle(article);
        return Result.success("创建成功", created);
    }

    @PutMapping("/articles/{id}")
    public Result<KnowledgeArticle> updateArticle(@PathVariable Long id, @RequestBody KnowledgeArticle article) {
        KnowledgeArticle updated = knowledgeArticleService.updateArticle(id, article);
        return Result.success("更新成功", updated);
    }

    @DeleteMapping("/articles/{id}")
    public Result<String> deleteArticle(@PathVariable Long id) {
        boolean success = knowledgeArticleService.deleteArticle(id);
        if (success) {
            return Result.success("删除成功");
        }
        return Result.error(ResultCode.RESOURCE_NOT_FOUND, "文章不存在");
    }

    @PutMapping("/articles/{id}/status")
    public Result<String> updateArticleStatus(@PathVariable Long id, @RequestBody KnowledgeArticle body) {
        String status = body.getStatus();
        boolean success = knowledgeArticleService.updateStatus(id, status);
        if (success) {
            return Result.success("状态更新成功");
        }
        return Result.error(ResultCode.RESOURCE_NOT_FOUND, "文章不存在");
    }
}