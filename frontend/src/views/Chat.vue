<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { chatStream, type ChatMessage } from '@/api/modules/chat'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import JIcon from '@/components/JIcon.vue'

interface Message extends ChatMessage {
  id: string
  toolCalls?: Array<{ name: string; data?: unknown }>
  pending?: boolean
}

const messages = ref<Message[]>([
  {
    id: 'sys',
    role: 'system',
    content: '你好，我是购物助手。找商品、查订单、申请售后都可以问我。',
  },
])
const input = ref('')
const sending = ref(false)
const abortController = ref<AbortController | null>(null)
const messagesEl = ref<HTMLElement | null>(null)

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
        message: text,
        history: messages.value.slice(0, -2).map((m) => ({ role: m.role, content: m.content })),
      },
      {
        onSignal: () => abortController.value!.signal,
        onToken: (chunk) => { assistantMsg.content += chunk },
        onToolCall: (name, data) => {
          assistantMsg.toolCalls = assistantMsg.toolCalls ?? []
          assistantMsg.toolCalls.push({ name, data })
        },
        onToolResult: (_name) => {
          assistantMsg.content += '\n\n_[工具已返回结果]_'
        },
        onError: (err) => { ElMessage.error(err.message) },
        onDone: () => { assistantMsg.pending = false },
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
</script>

<template>
  <div class="page chat-page">
    <AppHeader />
    <div class="chat-container">
      <header class="chat-header">
        <h2>
          <JIcon name="chatRound" :size="20" />
          AI 助手
        </h2>
        <p>找商品、查订单、问售后 — 用说的就行</p>
      </header>

      <div ref="messagesEl" class="messages" role="log" aria-label="对话消息" aria-live="polite">
        <div
          v-for="m in messages"
          :key="m.id"
          class="msg"
          :class="['role-' + m.role, { pending: m.pending }]"
        >
          <div class="bubble">
            <template v-if="m.role === 'assistant' && !m.pending && !m.content">
              <span class="thinking-dots">
                <span class="dot" /><span class="dot" /><span class="dot" />
              </span>
            </template>
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
</template>

<style scoped lang="scss">
.chat-page {
  display: flex;
  flex-direction: column;
}

.chat-container {
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  padding: var(--space-5);
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);

  @media (min-width: 768px) {
    padding: var(--space-6);
  }
}

.chat-header {
  text-align: center;

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
    margin: var(--space-2) 0 0;
    font-size: var(--text-sm);
  }
}

/* ----- 消息列表 ----- */
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
  min-height: 360px;
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

@keyframes blink {
  50% { opacity: 0; }
}

/* 思考中 */
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

/* ----- 输入区 ----- */
.composer {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: var(--space-3) var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
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
