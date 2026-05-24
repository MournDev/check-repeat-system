package com.abin.checkrepeatsystem.knowledge.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.knowledge.dto.PageDTO;
import com.abin.checkrepeatsystem.knowledge.service.KnowledgeArticleService;
import com.abin.checkrepeatsystem.knowledge.service.KnowledgeCategoryService;
import com.abin.checkrepeatsystem.pojo.entity.KnowledgeArticle;
import com.abin.checkrepeatsystem.pojo.entity.KnowledgeCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeCategoryService knowledgeCategoryService;

    private final KnowledgeArticleService knowledgeArticleService;

    @GetMapping("/categories")
    public Result<List<KnowledgeCategory>> getCategories() {
        List<KnowledgeCategory> categories = knowledgeCategoryService.listAllWithCount();
        return Result.success(categories);
    }

    @GetMapping("/articles")
    public Result<PageDTO<KnowledgeArticle>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        PageDTO<KnowledgeArticle> result = knowledgeArticleService.listArticles(page, size, category, keyword);
        return Result.success(result);
    }

    @GetMapping("/articles/popular")
    public Result<List<KnowledgeArticle>> getPopular() {
        List<KnowledgeArticle> articles = knowledgeArticleService.listPopular(10);
        return Result.success(articles);
    }

    @GetMapping("/articles/{id}")
    public Result<KnowledgeArticle> getArticle(@PathVariable Long id) {
        KnowledgeArticle article = knowledgeArticleService.getArticle(id);
        if (article == null) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "文章不存在");
        }
        return Result.success(article);
    }

    @GetMapping("/search")
    public Result<PageDTO<KnowledgeArticle>> searchArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword) {
        PageDTO<KnowledgeArticle> result = knowledgeArticleService.listArticles(page, size, null, keyword);
        return Result.success(result);
    }
}