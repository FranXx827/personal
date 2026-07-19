<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { chatStream, chatApi, type ChatMessage, type SessionItem } from '@/api/modules/chat'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import JIcon from '@/components/JIcon.vue'

interface Message extends ChatMessage {
  id: string
  toolCalls?: Array<{ name: string; args?: unknown }>
  pending?: boolean
}

// ---- 状态 ----
const messages = ref<Message[]>([])
const input = ref('')
const sending = ref(false)
const abortController = ref<AbortController | null>(null)
const messagesEl = ref<HTMLElement | null>(null)

// 会话管理
const sessions = ref<SessionItem[]>([])
const currentSessionId = ref<string>('')
const sidebarOpen = ref(false)
const loadingSessions = ref(false)
const loadingHistory = ref(false)

// ---- 初始化 ----
onMounted(async () => {
  await loadSessions()
  // 默认新建一个会话
  newSession()
})

// ---- 会话管理 ----
async function loadSessions() {
  loadingSessions.value = true
  try {
    sessions.value = await chatApi.listSessions()
  } catch (e) {
    // 静默失败
  } finally {
    loadingSessions.value = false
  }
}

async function switchSession(sessionId: string) {
  if (sessionId === currentSessionId.value) return
  if (sending.value) {
    ElMessage.warning('当前对话正在进行中')
    return
  }

  loadingHistory.value = true
  currentSessionId.value = sessionId
  messages.value = [] // 先清空，避免闪烁

  try {
    const detail = await chatApi.getSession(sessionId)
    if (detail) {
      messages.value = detail.messages.map((m) => ({
        id: m.id,
        role: m.role as Message['role'],
        content: m.content,
        toolCalls: m.metadata && typeof m.metadata === 'object'
          ? (m.metadata as Record<string, unknown>).tool_calls as Array<{ name: string }> ?? undefined
          : undefined,
      }))
    }
  } catch {
    messages.value = []
  } finally {
    loadingHistory.value = false
    sidebarOpen.value = false
    await scrollToBottom()
  }
}

function newSession() {
  if (sending.value) {
    ElMessage.warning('当前对话正在进行中')
    return
  }
  currentSessionId.value = ''
  messages.value = [
    {
      id: 'sys_welcome',
      role: 'system',
      content: '你好，我是购物助手。找商品、查订单、申请售后都可以问我。',
    },
  ]
  sidebarOpen.value = false
}

async function deleteSession(sessionId: string, evt: Event) {
  evt.stopPropagation()
  try {
    await ElMessageBox.confirm('确定删除该对话？', '确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await chatApi.deleteSession(sessionId)
    sessions.value = sessions.value.filter((s) => s.id !== sessionId)
    if (currentSessionId.value === sessionId) {
      newSession()
    }
  } catch {
    // 取消删除
  }
}

// ---- 发送消息 ----
async function send() {
  const text = input.value.trim()
  if (!text || sending.value) return

  const userMsg: Message = { id: `u_${Date.now()}`, role: 'user', content: text }
  const assistantId = `a_${Date.now()}`
  const assistantMsg: Message = { id: assistantId, role: 'assistant', content: '', pending: true }
  messages.value.push(userMsg, assistantMsg)
  input.value = ''
  await scrollToBottom()

  sending.value = true
  abortController.value = new AbortController()

  try {
    await chatStream(
      {
        sessionId: currentSessionId.value || undefined,
        message: text,
        history: messages.value
          .filter((m) => m.id !== assistantId && m.id !== 'sys_welcome')
          .map((m) => ({ role: m.role, content: m.content })),
      },
      {
        onSignal: () => abortController.value!.signal,
        onToken: (chunk) => {
          assistantMsg.content += chunk
        },
        onToolCall: (name, args) => {
          assistantMsg.toolCalls = assistantMsg.toolCalls ?? []
          assistantMsg.toolCalls.push({ name, args })
        },
        onError: (err) => {
          ElMessage.error(err.message)
        },
        onDone: (messageId) => {
          assistantMsg.pending = false
          // 如果是新会话，用后端返回的 messageId 作为 sessionId
          if (messageId && !currentSessionId.value) {
            currentSessionId.value = messageId
          }
          // 刷新会话列表
          loadSessions()
        },
      },
    )
  } finally {
    assistantMsg.pending = false
    sending.value = false
    abortController.value = null
    await scrollToBottom()
  }
}

function stop() {
  abortController.value?.abort()
  sending.value = false
}

async function scrollToBottom() {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

// 格式化时间
function fmtTime(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}
</script>

<template>
  <div class="page chat-page">
    <AppHeader />
    <div class="chat-layout">
      <!-- 侧边栏 -->
      <aside class="sidebar" :class="{ open: sidebarOpen }">
        <div class="sidebar-header">
          <button class="new-btn" @click="newSession">
            <JIcon name="plus" :size="14" />
            新对话
          </button>
        </div>
        <div class="session-list" v-if="!loadingSessions">
          <div
            v-for="s in sessions"
            :key="s.id"
            class="session-item"
            :class="{ active: s.id === currentSessionId }"
            @click="switchSession(s.id)"
          >
            <div class="session-title">{{ s.title }}</div>
            <div class="session-meta">
              <span class="session-time">{{ fmtTime(s.updatedAt) }}</span>
              <button class="del-btn" @click="deleteSession(s.id, $event)" title="删除">
                <JIcon name="trash" :size="12" />
              </button>
            </div>
          </div>
          <div v-if="sessions.length === 0" class="empty-sessions">
            暂无对话记录
          </div>
        </div>
        <div v-else class="sidebar-loading">加载中...</div>
      </aside>

      <!-- 主内容 -->
      <div class="chat-main">
        <!-- mobile sidebar toggle -->
        <button class="menu-toggle" @click="sidebarOpen = !sidebarOpen">
          <JIcon name="menu" :size="18" />
        </button>

        <!-- 覆盖层（移动端） -->
        <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false" />

        <header class="chat-header">
          <h2>
            <JIcon name="chatRound" :size="20" />
            AI 助手
          </h2>
          <p>找商品、查订单、问售后 — 用说的就行</p>
        </header>

        <div
          ref="messagesEl"
          class="messages"
          role="log"
          aria-label="对话消息"
          aria-live="polite"
        >
          <div v-if="loadingHistory" class="loading-msg">加载历史消息...</div>

          <div
            v-for="m in messages"
            :key="m.id"
            class="msg"
            :class="['role-' + m.role, { pending: m.pending }]"
          >
            <div class="bubble">
              <!-- 等待中（思考动画） -->
              <template v-if="m.role === 'assistant' && m.pending && !m.content">
                <span class="thinking-dots">
                  <span class="dot" /><span class="dot" /><span class="dot" />
                </span>
              </template>
              <!-- 已结束但无内容 -->
              <template v-else-if="m.role === 'assistant' && !m.pending && !m.content">
                <span class="empty-reply">助手未能生成回复，请换个方式提问</span>
              </template>
              <!-- 正常显示 -->
              <template v-else>
                {{ m.content }}<span v-if="m.pending" class="cursor">▍</span>
              </template>
            </div>
            <div v-if="m.toolCalls?.length" class="tool-chips">
              <div v-for="(tc, i) in m.toolCalls" :key="i" class="tool-chip">
                <JIcon name="info" :size="12" />
                {{ tc.name }}
              </div>
            </div>
          </div>
        </div>

        <footer class="composer">
          <div class="composer-input">
            <textarea
              v-model="input"
              class="input-area"
              :rows="2"
              placeholder="试试输入：帮我找一款 5000 元左右的拍照手机"
              :disabled="sending"
              @keydown.enter.exact.prevent="send"
            />
            <button
              v-if="sending"
              class="send-btn stop-btn"
              @click="stop"
              title="停止"
            >
              <JIcon name="stop" :size="16" />
            </button>
            <button
              v-else
              class="send-btn"
              :disabled="!input.trim()"
              @click="send"
              title="发送"
            >
              <JIcon name="send" :size="16" />
            </button>
          </div>
          <span class="composer-hint">Enter 发送</span>
        </footer>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.chat-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
  position: relative;
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 260px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;

  @media (max-width: 768px) {
    position: fixed;
    left: -280px;
    top: 0;
    bottom: 0;
    z-index: 100;
    transition: left 0.25s ease;
    box-shadow: 2px 0 12px rgba(0, 0, 0, 0.1);

    &.open {
      left: 0;
    }
  }
}

.sidebar-header {
  padding: var(--space-3);
  border-bottom: 1px solid var(--border-subtle);
}

.new-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  padding: 8px 16px;
  border: 1px dashed var(--border-subtle);
  border-radius: var(--radius-lg);
  background: transparent;
  color: var(--text-primary);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: var(--bg-subtle);
    border-color: var(--color-primary);
    color: var(--color-primary);
  }
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2);
}

.session-item {
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  margin-bottom: 2px;
  transition: background 0.15s;

  &:hover {
    background: var(--bg-subtle);
  }

  &.active {
    background: var(--color-primary-bg, #e8f4fd);
  }
}

.session-title {
  font-size: var(--text-sm);
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.session-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.session-time {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.del-btn {
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 2px;
  opacity: 0;
  transition: opacity 0.15s;

  .session-item:hover & {
    opacity: 1;
  }

  &:hover {
    color: var(--color-danger);
  }
}

.empty-sessions,
.sidebar-loading {
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  padding: var(--space-6) var(--space-3);
}

/* ===== 主区域 ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: var(--space-4);
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  position: relative;

  @media (min-width: 768px) {
    padding: var(--space-5);
  }
}

.menu-toggle {
  display: none;
  position: absolute;
  top: var(--space-3);
  left: var(--space-2);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 6px;
  cursor: pointer;
  color: var(--text-primary);
  z-index: 10;

  @media (max-width: 768px) {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 99;

  @media (max-width: 768px) {
    &.show {
      display: block;
    }
  }
}

.chat-header {
  text-align: center;
  padding: var(--space-2) 0 var(--space-3);
  margin-left: 32px;

  h2 {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    font-family: var(--font-display);
    font-size: var(--text-xl);
    font-weight: 650;
    color: var(--text-primary);
    margin: 0;
    letter-spacing: -0.02em;
  }

  p {
    color: var(--text-tertiary);
    margin: var(--space-1) 0 0;
    font-size: var(--text-sm);
  }
}

/* ===== 消息列表 ===== */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4);
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-height: 200px;
}

.loading-msg {
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  padding: var(--space-8) 0;
}

.msg {
  display: flex;
  flex-direction: column;
  max-width: 82%;

  @media (max-width: 600px) {
    max-width: 92%;
  }
}

.bubble {
  padding: 10px 16px;
  border-radius: var(--radius-lg);
  background: var(--bg-message);
  color: var(--text-primary);
  white-space: pre-wrap;
  line-height: var(--leading-relaxed);
  font-size: var(--text-sm);
}

.role-user {
  align-self: flex-end;

  .bubble {
    background: var(--color-primary);
    color: #fff;
    border-bottom-right-radius: 4px;
  }
}

.role-assistant {
  align-self: flex-start;

  .bubble {
    border-bottom-left-radius: 4px;
  }
}

.role-system {
  align-self: center;

  .bubble {
    background: transparent;
    color: var(--text-tertiary);
    font-size: var(--text-xs);
    text-align: center;
    max-width: 70%;
  }
}

.cursor {
  display: inline-block;
  animation: blink 1s step-end infinite;
  margin-left: 2px;
}

.empty-reply {
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  font-style: italic;
}

@keyframes blink {
  50% { opacity: 0; }
}

.thinking-dots {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 2px 0;
}

.dot {
  width: 6px;
  height: 6px;
  background: var(--text-tertiary);
  border-radius: 50%;
  animation: dotPulse 1.4s ease-in-out infinite;

  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

.tool-chips {
  margin-top: var(--space-2);
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.tool-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-xs);
  padding: 3px 10px;
  background: var(--bg-skeleton);
  border-radius: var(--radius-full);
  color: var(--text-tertiary);
}

/* ===== 输入区 ===== */
.composer {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: var(--space-3) var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.composer-input {
  display: flex;
  align-items: flex-end;
  gap: var(--space-2);
}

.input-area {
  flex: 1;
  border: none;
  background: transparent;
  font-family: var(--font-body);
  font-size: var(--text-sm);
  color: var(--text-primary);
  resize: none;
  outline: none;
  line-height: var(--leading-snug);
  min-height: 44px;
  max-height: 120px;

  &::placeholder {
    color: var(--text-placeholder);
  }
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  background: var(--color-primary);
  color: #fff;
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background var(--duration-fast) var(--ease-out-quart), transform var(--duration-fast) var(--ease-out-quart);

  &:hover:not(:disabled) {
    background: var(--color-primary-hover);
    transform: scale(1.05);
  }

  &:disabled {
    background: var(--bg-skeleton);
    color: var(--text-tertiary);
    cursor: not-allowed;
  }
}

.stop-btn {
  background: var(--color-danger);

  &:hover {
    background: var(--color-danger);
  }
}

.composer-hint {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  padding-left: 2px;
}
</style>
