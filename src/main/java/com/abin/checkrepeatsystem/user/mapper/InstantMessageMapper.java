package com.abin.checkrepeatsystem.user.mapper;

import com.abin.checkrepeatsystem.pojo.entity.InstantMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 即时通讯消息Mapper接口
 */
@Mapper
public interface InstantMessageMapper extends BaseMapper<InstantMessage> {
}
