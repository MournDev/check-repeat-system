package com.abin.checkrepeatsystem.mapper;

import com.abin.checkrepeatsystem.pojo.entity.KnowledgeArticle;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {

    List<KnowledgeArticle> listArticles(@Param("keyword") String keyword,
                                         @Param("category") String category,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    Long countArticles(@Param("keyword") String keyword,
                       @Param("category") String category,
                       @Param("status") String status);

    List<KnowledgeArticle> listPopular(@Param("limit") int limit);

    List<KnowledgeArticle> adminListArticles(@Param("keyword") String keyword,
                                              @Param("status") String status,
                                              @Param("offset") int offset,
                                              @Param("size") int size);

    Long adminCountArticles(@Param("keyword") String keyword,
                            @Param("status") String status);
}
