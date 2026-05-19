package com.abin.checkrepeatsystem.knowledge.service.impl;

import com.abin.checkrepeatsystem.knowledge.service.KnowledgeCategoryService;
import com.abin.checkrepeatsystem.mapper.KnowledgeCategoryMapper;
import com.abin.checkrepeatsystem.pojo.entity.KnowledgeCategory;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeCategoryServiceImpl extends ServiceImpl<KnowledgeCategoryMapper, KnowledgeCategory>
        implements KnowledgeCategoryService {

    @Resource
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    @Override
    public List<KnowledgeCategory> listAllWithCount() {
        return knowledgeCategoryMapper.listWithCount();
    }
}