package com.abin.checkrepeatsystem.knowledge.service.impl;

import com.abin.checkrepeatsystem.knowledge.dto.PageDTO;
import com.abin.checkrepeatsystem.knowledge.service.KnowledgeArticleService;
import com.abin.checkrepeatsystem.mapper.KnowledgeArticleMapper;
import com.abin.checkrepeatsystem.pojo.entity.KnowledgeArticle;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Service
public class KnowledgeArticleServiceImpl extends ServiceImpl<KnowledgeArticleMapper, KnowledgeArticle>
        implements KnowledgeArticleService {

    private final KnowledgeArticleMapper knowledgeArticleMapper;

    @Override
    public PageDTO<KnowledgeArticle> listArticles(int page, int size, String category, String keyword) {
        int offset = (page - 1) * size;
        List<KnowledgeArticle> items = knowledgeArticleMapper.listArticles(keyword, category, offset, size);
        Long total = knowledgeArticleMapper.countArticles(keyword, category, null);
        return new PageDTO<>(items, total);
    }

    @Override
    public List<KnowledgeArticle> listPopular(int limit) {
        return knowledgeArticleMapper.listPopular(limit);
    }

    @Override
    public KnowledgeArticle getArticle(Long id) {
        KnowledgeArticle article = getById(id);
        if (article != null) {
            LambdaUpdateWrapper<KnowledgeArticle> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(KnowledgeArticle::getId, id)
                   .setSql("view_count = view_count + 1");
            update(wrapper);
            article.setViewCount(article.getViewCount() + 1);
        }
        return article;
    }

    @Override
    public PageDTO<KnowledgeArticle> adminListArticles(int page, int size, String keyword, String status) {
        int offset = (page - 1) * size;
        List<KnowledgeArticle> items = knowledgeArticleMapper.adminListArticles(keyword, status, offset, size);
        Long total = knowledgeArticleMapper.adminCountArticles(keyword, status);
        return new PageDTO<>(items, total);
    }

    @Override
    public KnowledgeArticle createArticle(KnowledgeArticle article) {
        save(article);
        return article;
    }

    @Override
    public KnowledgeArticle updateArticle(Long id, KnowledgeArticle article) {
        article.setId(id);
        updateById(article);
        return getById(id);
    }

    @Override
    public boolean deleteArticle(Long id) {
        return removeById(id);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        LambdaUpdateWrapper<KnowledgeArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(KnowledgeArticle::getId, id)
               .set(KnowledgeArticle::getStatus, status);
        return update(wrapper);
    }
}