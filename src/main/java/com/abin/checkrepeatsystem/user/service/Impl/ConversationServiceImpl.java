package com.abin.checkrepeatsystem.user.service.Impl;

import com.abin.checkrepeatsystem.common.Exception.BusinessException;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.SpringContextUtil;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.Conversation;
import com.abin.checkrepeatsystem.pojo.entity.ConversationMember;
import com.abin.checkrepeatsystem.pojo.entity.InstantMessage;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.user.mapper.ConversationMapper;
import com.abin.checkrepeatsystem.user.mapper.ConversationMemberMapper;
import com.abin.checkrepeatsystem.user.mapper.InstantMessageMapper;
import com.abin.checkrepeatsystem.user.service.ConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话服务实现类
 */
@Service
@Slf4j
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
        implements ConversationService {

    @Autowired
    private ConversationMemberMapper conversationMemberMapper;

    @Override
    public IPage<Conversation> getUserConversations(Long userId, Integer pageNum, Integer pageSize) {
        Page<Conversation> page = new Page<>(pageNum, pageSize);

        // 首先查询用户参与的会话 ID 列表
        LambdaQueryWrapper<ConversationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ConversationMember::getUserId, userId)
                .eq(ConversationMember::getIsDeleted, 0);
        List<ConversationMember> members = conversationMemberMapper.selectList(memberWrapper);
        
        List<Long> conversationIds = members.stream()
                .map(ConversationMember::getConversationId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getIsDeleted, 0)
                .orderByDesc(Conversation::getUpdateTime);

        // 只查询用户参与的会话
        if (!conversationIds.isEmpty()) {
            wrapper.in(Conversation::getId, conversationIds);
        } else {
            // 如果用户没有参与任何会话，返回空结果
            wrapper.in(Conversation::getId, -1); // 一个不存在的 ID
        }

        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Conversation createConversation(String name, String type, Long creatorId, List<Long> memberIds) {
        // 创建会话
        Conversation conversation = new Conversation();
        conversation.setName(name);
        conversation.setType(type);
        conversation.setCreatorId(creatorId);
        conversation.setLastMessageTime(LocalDateTime.now());
        UserBusinessInfoUtils.setAuditField(conversation, true);
        save(conversation);

        // 添加会话成员
        SysUserMapper sysUserMapper = SpringContextUtil.getBean(SysUserMapper.class);
        for (Long memberId : memberIds) {
            // 获取用户信息
            SysUser user = sysUserMapper.selectById(memberId);
            
            ConversationMember member = new ConversationMember();
            member.setConversationId(conversation.getId());
            member.setUserId(memberId);
            // 保存用户头像
            if (user != null && user.getAvatar() != null) {
                member.setAvatar(user.getAvatar());
            }
            member.setJoinedAt(LocalDateTime.now());
            UserBusinessInfoUtils.setAuditField(member, true);
            conversationMemberMapper.insert(member);
        }

        log.info("创建会话成功 - 会话ID: {}, 创建者: {}, 成员数: {}",
                conversation.getId(), creatorId, memberIds.size());

        // 关联历史消息
        associateHistoricalMessages(conversation.getId(), creatorId, memberIds);

        return conversation;
    }

    /**
     * 关联历史消息到会话
     */
    private void associateHistoricalMessages(Long conversationId, Long creatorId, List<Long> memberIds) {
        try {
            InstantMessageMapper instantMessageMapper = SpringContextUtil.getBean(InstantMessageMapper.class);
            
            // 遍历所有成员，关联与创建者之间的历史消息
            for (Long memberId : memberIds) {
                if (!memberId.equals(creatorId)) {
                    LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
                    wrapper.and(w -> w
                            .eq(InstantMessage::getSenderId, creatorId)
                            .eq(InstantMessage::getReceiverId, memberId))
                        .or(w -> w
                            .eq(InstantMessage::getSenderId, memberId)
                            .eq(InstantMessage::getReceiverId, creatorId))
                        .isNull(InstantMessage::getConversationId)
                        .eq(InstantMessage::getIsDeleted, 0);
                    
                    InstantMessage updateMessage = new InstantMessage();
                    updateMessage.setConversationId(conversationId);
                    updateMessage.setUpdateTime(LocalDateTime.now());
                    
                    int updatedCount = instantMessageMapper.update(updateMessage, wrapper);
                    if (updatedCount > 0) {
                        log.info("已关联 {} 条历史消息到会话 {} (创建者: {}, 成员: {})", 
                                updatedCount, conversationId, creatorId, memberId);
                    }
                }
            }
            
            // 更新会话的最后消息时间
            updateConversationLastMessage(conversationId);
            
        } catch (Exception e) {
            log.error("关联历史消息失败 - 会话 ID: {}", conversationId, e);
        }
    }

    /**
     * 更新会话的最后消息时间
     */
    private void updateConversationLastMessage(Long conversationId) {
        try {
            InstantMessageMapper instantMessageMapper = SpringContextUtil.getBean(InstantMessageMapper.class);
            
            // 查找会话中最新的消息
            LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InstantMessage::getConversationId, conversationId)
                .eq(InstantMessage::getIsDeleted, 0)
                .orderByDesc(InstantMessage::getSentTime);
            
            // 使用分页查询获取第一条记录
            Page<InstantMessage> page = new Page<>(1, 1);
            IPage<InstantMessage> result = instantMessageMapper.selectPage(page, wrapper);
            
            if (result.getRecords() != null && !result.getRecords().isEmpty()) {
                InstantMessage latestMessage = result.getRecords().get(0);
                if (latestMessage.getSentTime() != null) {
                    Conversation conversation = new Conversation();
                    conversation.setId(conversationId);
                    conversation.setLastMessageTime(latestMessage.getSentTime());
                    conversation.setUpdateTime(LocalDateTime.now());
                    updateById(conversation);
                    
                    log.info("更新会话最后消息时间 - 会话 ID: {}, 时间: {}", 
                            conversationId, latestMessage.getSentTime());
                }
            }
        } catch (Exception e) {
            log.error("更新会话最后消息时间失败 - 会话 ID: {}", conversationId, e);
        }
    }

    @Override
    public boolean updateConversation(Long conversationId, String name, String avatar) {
        Conversation conversation = getById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "会话不存在");
        }

        if (name != null) {
            conversation.setName(name);
        }
        if (avatar != null) {
            conversation.setAvatar(avatar);
        }

        return updateById(conversation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean leaveConversation(Long conversationId, Long userId) {
        // 删除会话成员关系
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId);

        boolean removed = conversationMemberMapper.delete(wrapper) > 0;

        if (removed) {
            log.info("用户退出会话 - 会话ID: {}, 用户ID: {}", conversationId, userId);
        }

        return removed;
    }

    @Override
    public IPage<ConversationMember> getConversationMembers(Long conversationId, Integer pageNum, Integer pageSize) {
        Page<ConversationMember> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getIsDeleted, 0)
                .orderByAsc(ConversationMember::getJoinedAt);

        IPage<ConversationMember> result = conversationMemberMapper.selectPage(page, wrapper);
        
        // 直接使用会话成员表中的头像，避免查询用户表
        for (ConversationMember member : result.getRecords()) {
            if (member.getAvatar() != null) {
                member.setUserAvatar(member.getAvatar());
            }
        }
        
        return result;
    }

    @Override
    public IPage<Conversation> searchConversations(Long userId, String keyword, Integer pageNum, Integer pageSize) {
        Page<Conversation> page = new Page<>(pageNum, pageSize);

        // 首先查询用户参与的会话 ID 列表
        LambdaQueryWrapper<ConversationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ConversationMember::getUserId, userId)
                .eq(ConversationMember::getIsDeleted, 0);
        List<ConversationMember> members = conversationMemberMapper.selectList(memberWrapper);
        
        List<Long> conversationIds = members.stream()
                .map(ConversationMember::getConversationId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getIsDeleted, 0)
                .and(w -> w.like(Conversation::getName, keyword)
                        .or()
                        .like(Conversation::getDescription, keyword))
                .orderByDesc(Conversation::getUpdateTime);

        // 只搜索用户参与的会话
        if (!conversationIds.isEmpty()) {
            wrapper.in(Conversation::getId, conversationIds);
        } else {
            // 如果用户没有参与任何会话，返回空结果
            wrapper.in(Conversation::getId, -1); // 一个不存在的 ID
        }

        return page(page, wrapper);
    }
}

