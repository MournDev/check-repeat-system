package com.abin.checkrepeatsystem.knowledge.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.knowledge.dto.PageDTO;
import com.abin.checkrepeatsystem.knowledge.service.KnowledgeArticleService;
import com.abin.checkrepeatsystem.knowledge.service.KnowledgeCategoryService;
import com.abin.checkrepeatsystem.pojo.entity.KnowledgeArticle;
import com.abin.checkrepeatsystem.pojo.entity.KnowledgeCategory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Resource
    private KnowledgeCategoryService knowledgeCategoryService;

    @Resource
    private KnowledgeArticleService knowledgeArticleService;

    @GetMapping("/categories")
    public Result<List<KnowledgeCategory>> getCategories() {
        try {
            List<KnowledgeCategory> categories = knowledgeCategoryService.listAllWithCount();
            return Result.success(categories);
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取分类列表失败");
        }
    }

    @GetMapping("/articles")
    public Result<PageDTO<KnowledgeArticle>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        try {
            PageDTO<KnowledgeArticle> result = knowledgeArticleService.listArticles(page, size, category, keyword);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取文章列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取文章列表失败");
        }
    }

    @GetMapping("/articles/popular")
    public Result<List<KnowledgeArticle>> getPopular() {
        try {
            List<KnowledgeArticle> articles = knowledgeArticleService.listPopular(10);
            return Result.success(articles);
        } catch (Exception e) {
            log.error("获取热门文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取热门文章失败");
        }
    }

    @GetMapping("/articles/{id}")
    public Result<KnowledgeArticle> getArticle(@PathVariable Long id) {
        try {
            KnowledgeArticle article = knowledgeArticleService.getArticle(id);
            if (article == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "文章不存在");
            }
            return Result.success(article);
        } catch (Exception e) {
            log.error("获取文章详情失败 - ID: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取文章详情失败");
        }
    }

    @GetMapping("/search")
    public Result<PageDTO<KnowledgeArticle>> searchArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword) {
        try {
            PageDTO<KnowledgeArticle> result = knowledgeArticleService.listArticles(page, size, null, keyword);
            return Result.success(result);
        } catch (Exception e) {
            log.error("搜索文章失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "搜索文章失败");
        }
    }
}