package com.abin.checkrepeatsystem.knowledge.service;

import com.abin.checkrepeatsystem.pojo.entity.KnowledgeCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface KnowledgeCategoryService extends IService<KnowledgeCategory> {

    List<KnowledgeCategory> listAllWithCount();
}