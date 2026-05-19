package com.abin.checkrepeatsystem.mapper;

import com.abin.checkrepeatsystem.pojo.entity.KnowledgeCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeCategoryMapper extends BaseMapper<KnowledgeCategory> {

    List<KnowledgeCategory> listWithCount();
}