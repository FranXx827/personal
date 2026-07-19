package com.ecommerce.modules.chat.service;

import com.ecommerce.modules.chat.dto.*;

import java.util.List;

/** AI 对话会话 & 消息 服务 */
public interface ChatService {

    /** 创建会话 */
    void createSession(SessionCreateRequest request);

    /** 获取用户的会话列表（按更新时间倒序） */
    List<ChatSessionVO> listSessions(Long userId, int limit, int offset);

    /** 获取会话详情（含所有消息） */
    ChatSessionDetailVO getSessionDetail(String sessionId, Long userId);

    /** 获取单条会话基本信息 */
    ChatSessionVO getSessionInfo(String sessionId, Long userId);

    /** 更新会话标题 */
    void updateTitle(String sessionId, Long userId, String title);

    /** 软删除会话（校验用户归属） */
    boolean deleteSession(String sessionId, Long userId);

    /** 添加消息 */
    void createMessage(MessageCreateRequest request);

    /** 批量添加消息 */
    void batchCreateMessages(java.util.List<MessageCreateRequest> requests);
}
