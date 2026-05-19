package com.abin.checkrepeatsystem.knowledge.service;

import com.abin.checkrepeatsystem.knowledge.dto.PageDTO;
import com.abin.checkrepeatsystem.pojo.entity.KnowledgeArticle;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface KnowledgeArticleService extends IService<KnowledgeArticle> {

    PageDTO<KnowledgeArticle> listArticles(int page, int size, String category, String keyword);

    List<KnowledgeArticle> listPopular(int limit);

    KnowledgeArticle getArticle(Long id);

    PageDTO<KnowledgeArticle> adminListArticles(int page, int size, String keyword, String status);

    KnowledgeArticle createArticle(KnowledgeArticle article);

    KnowledgeArticle updateArticle(Long id, KnowledgeArticle article);

    boolean deleteArticle(Long id);

    boolean updateStatus(Long id, String status);
}