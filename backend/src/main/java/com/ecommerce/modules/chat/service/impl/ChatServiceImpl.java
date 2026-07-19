package com.ecommerce.modules.chat.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.modules.chat.dto.*;
import com.ecommerce.modules.chat.entity.ChatMessage;
import com.ecommerce.modules.chat.entity.ChatSession;
import com.ecommerce.modules.chat.mapper.ChatMessageMapper;
import com.ecommerce.modules.chat.mapper.ChatSessionMapper;
import com.ecommerce.modules.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    @Override
    @Transactional
    public void createSession(SessionCreateRequest request) {
        ChatSession session = new ChatSession();
        session.setId(request.getId());
        session.setUserId(request.getUserId());
        session.setTitle(request.getTitle());
        sessionMapper.insert(session);
        log.info("session_created id={} userId={} title={}", request.getId(), request.getUserId(), request.getTitle());
    }

    @Override
    public List<ChatSessionVO> listSessions(Long userId, int limit, int offset) {
        LambdaQueryWrapper<ChatSession> qw = Wrappers.<ChatSession>lambdaQuery()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdatedAt)
                .last("LIMIT " + limit + " OFFSET " + offset);

        List<ChatSession> sessions = sessionMapper.selectList(qw);
        if (sessions.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询每个会话的消息条数
        Set<String> sessionIds = sessions.stream().map(ChatSession::getId).collect(Collectors.toSet());
        List<ChatMessage> allMsgs = messageMapper.selectList(
                Wrappers.<ChatMessage>lambdaQuery().in(ChatMessage::getSessionId, sessionIds));
        Map<String, Long> countMap = allMsgs.stream()
                .collect(Collectors.groupingBy(ChatMessage::getSessionId, Collectors.counting()));

        return sessions.stream().map(s -> {
            ChatSessionVO vo = new ChatSessionVO();
            vo.setId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setCreatedAt(s.getCreatedAt());
            vo.setUpdatedAt(s.getUpdatedAt());
            vo.setMessageCount(countMap.getOrDefault(s.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public ChatSessionDetailVO getSessionDetail(String sessionId, Long userId) {
        ChatSessionVO sessionVO = getSessionInfo(sessionId, userId);
        List<ChatMessage> messages = messageMapper.selectList(
                Wrappers.<ChatMessage>lambdaQuery()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreatedAt));

        List<ChatMessageVO> msgVOs = messages.stream().map(this::toMessageVO).collect(Collectors.toList());
        return new ChatSessionDetailVO(sessionVO, msgVOs);
    }

    @Override
    public ChatSessionVO getSessionInfo(String sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("会话不存在或无权访问");
        }
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setCreatedAt(session.getCreatedAt());
        vo.setUpdatedAt(session.getUpdatedAt());
        return vo;
    }

    @Override
    @Transactional
    public void updateTitle(String sessionId, Long userId, String title) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("会话不存在或无权访问");
        }
        session.setTitle(title);
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public boolean deleteSession(String sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return false;
        }
        // MyBatis-Plus @TableLogic 会自动处理 deleted 字段
        sessionMapper.deleteById(sessionId);
        // 同时删除关联消息
        messageMapper.delete(Wrappers.<ChatMessage>lambdaQuery()
                .eq(ChatMessage::getSessionId, sessionId));
        return true;
    }

    @Override
    @Transactional
    public void createMessage(MessageCreateRequest request) {
        ChatMessage msg = new ChatMessage();
        msg.setId(request.getId());
        msg.setSessionId(request.getSessionId());
        msg.setRole(request.getRole());
        msg.setContent(request.getContent() != null ? request.getContent() : "");
        msg.setMetadataJson(request.getMetadataJson());
        messageMapper.insert(msg);
    }

    @Override
    @Transactional
    public void batchCreateMessages(List<MessageCreateRequest> requests) {
        List<ChatMessage> entities = requests.stream().map(r -> {
            ChatMessage msg = new ChatMessage();
            msg.setId(r.getId());
            msg.setSessionId(r.getSessionId());
            msg.setRole(r.getRole());
            msg.setContent(r.getContent() != null ? r.getContent() : "");
            msg.setMetadataJson(r.getMetadataJson());
            return msg;
        }).collect(Collectors.toList());
        // MyBatis-Plus 批量插入
        entities.forEach(messageMapper::insert);
    }

    // ---- helper ----

    @SuppressWarnings("unchecked")
    private ChatMessageVO toMessageVO(ChatMessage m) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(m.getId());
        vo.setRole(m.getRole());
        vo.setContent(m.getContent());
        vo.setCreatedAt(m.getCreatedAt());
        if (m.getMetadataJson() != null && !m.getMetadataJson().isEmpty()) {
            try {
                vo.setMetadata(JSONUtil.parseObj(m.getMetadataJson()));
            } catch (Exception e) {
                log.warn("parse_metadata_failed id={}", m.getId());
            }
        }
        return vo;
    }
}
