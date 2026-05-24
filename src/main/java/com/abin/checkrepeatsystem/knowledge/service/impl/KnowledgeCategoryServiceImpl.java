package com.abin.checkrepeatsystem.knowledge.service.impl;

import com.abin.checkrepeatsystem.knowledge.service.KnowledgeCategoryService;
import com.abin.checkrepeatsystem.mapper.KnowledgeCategoryMapper;
import com.abin.checkrepeatsystem.pojo.entity.KnowledgeCategory;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Service
public class KnowledgeCategoryServiceImpl extends ServiceImpl<KnowledgeCategoryMapper, KnowledgeCategory>
        implements KnowledgeCategoryService {

    private final KnowledgeCategoryMapper knowledgeCategoryMapper;

    @Override
    public List<KnowledgeCategory> listAllWithCount() {
        return knowledgeCategoryMapper.listWithCount();
    }
}