package com.ecommerce.modules.chat.controller;

import com.ecommerce.common.response.Result;
import com.ecommerce.infra.security.UserContextHolder;
import com.ecommerce.modules.chat.dto.*;
import com.ecommerce.modules.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI 对话管理")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 解析当前请求的用户ID。
     * 优先从请求体/参数中取（AI 服务内部调用），
     * 其次从 JWT 上下文取（前端直接调用）。
     */
    private Long resolveUserId(Long reqUserId) {
        if (reqUserId != null && reqUserId > 0) {
            return reqUserId;
        }
        Long ctxUserId = UserContextHolder.getUserId();
        return ctxUserId != null && ctxUserId > 0 ? ctxUserId : null;
    }

    // ================ 会话 ================

    @Operation(summary = "创建会话")
    @PostMapping("/sessions")
    public Result<Void> createSession(@Valid @RequestBody SessionCreateRequest request) {
        Long userId = resolveUserId(request.getUserId());
        if (userId == null) {
            return Result.error(401, "缺少用户身份");
        }
        request.setUserId(userId);
        chatService.createSession(request);
        return Result.success();
    }

    @Operation(summary = "会话列表")
    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> listSessions(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Long uid = resolveUserId(userId);
        if (uid == null) {
            return Result.error(401, "缺少用户身份");
        }
        return Result.success(chatService.listSessions(uid, limit, offset));
    }

    @Operation(summary = "会话详情（含消息）")
    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionDetailVO> getSession(
            @PathVariable String sessionId,
            @RequestParam(required = false) Long userId) {
        Long uid = resolveUserId(userId);
        if (uid == null) {
            return Result.error(401, "缺少用户身份");
        }
        return Result.success(chatService.getSessionDetail(sessionId, uid));
    }

    @Operation(summary = "更新会话标题")
    @PutMapping("/sessions/{sessionId}/title")
    public Result<Void> updateTitle(
            @PathVariable String sessionId,
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody TitleUpdateRequest request) {
        Long uid = resolveUserId(userId);
        if (uid == null) {
            return Result.error(401, "缺少用户身份");
        }
        chatService.updateTitle(sessionId, uid, request.getTitle());
        return Result.success();
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(
            @PathVariable String sessionId,
            @RequestParam(required = false) Long userId) {
        Long uid = resolveUserId(userId);
        if (uid == null) {
            return Result.error(401, "缺少用户身份");
        }
        boolean ok = chatService.deleteSession(sessionId, uid);
        if (!ok) {
            return Result.error(404, "会话不存在或无权操作");
        }
        return Result.success();
    }

    // ================ 消息 ================

    @Operation(summary = "添加消息")
    @PostMapping("/sessions/{sessionId}/messages")
    public Result<Void> createMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody MessageCreateRequest request) {
        request.setSessionId(sessionId);
        chatService.createMessage(request);
        return Result.success();
    }

    @Operation(summary = "批量添加消息")
    @PostMapping("/sessions/{sessionId}/messages/batch")
    public Result<Void> batchCreateMessages(
            @PathVariable String sessionId,
            @RequestBody List<MessageCreateRequest> requests) {
        requests.forEach(r -> r.setSessionId(sessionId));
        chatService.batchCreateMessages(requests);
        return Result.success();
    }
}
