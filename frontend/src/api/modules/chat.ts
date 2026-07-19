/**
 * AI 对话 API (SSE 流式 + 会话管理)
 *
 * EventSource 原生不支持 POST 与自定义 header，
 * 因此走 fetch + ReadableStream 解析 SSE 协议。
 */
import { aiHttp } from '../http'

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system' | 'tool'
  content: string
}

export interface ChatRequest {
  sessionId?: string
  message: string
  history?: ChatMessage[]
}

export type ChatEventType = 'token' | 'tool_call' | 'tool_result' | 'error' | 'done'

export interface ChatEvent {
  type: ChatEventType
  content?: string
  name?: string
  data?: unknown
  messageId?: string
  error?: string
}

export interface StreamHandlers {
  onToken?: (content: string) => void
  onToolCall?: (name: string, args: unknown) => void
  onToolResult?: (name: string, data: unknown) => void
  onError?: (err: Error) => void
  onDone?: (messageId: string) => void
  onSignal?: () => AbortSignal
}

export interface SessionItem {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface MessageItem {
  id: string
  role: string
  content: string
  metadata?: Record<string, unknown> | null
  createdAt: string
}

export interface SessionDetail {
  session: SessionItem & { messageCount: number }
  messages: MessageItem[]
}

export async function chatStream(req: ChatRequest, handlers: StreamHandlers): Promise<void> {
  const signal = handlers.onSignal?.()
  const res = await aiHttp.post('/chat', req, {
    responseType: 'stream',
    signal,
    headers: { Accept: 'text/event-stream' },
  })

  const reader = res.data.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE 规范：事件以 \n\n 分隔（sse-starlette 使用 CRLF）
      // 先统一归一化为 LF
      buffer = buffer.replace(/\r\n/g, '\n')
      const parts = buffer.split('\n\n')
      buffer = parts.pop() ?? ''

      for (const part of parts) {
        // 提取事件类型（event: xxx）和 data 负载
        const lines = part.split('\n')
        let eventType = ''
        let dataPayload = ''
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            dataPayload += (dataPayload ? '\n' : '') + line.slice(5).trim()
          }
        }
        if (!dataPayload) continue

        // 处理纯 JSON 的 data（done 事件可能用 {"cancelled":true} 格式）
        if (eventType === 'done' || dataPayload.startsWith('{"cancelled')) {
          try {
            const doneData = JSON.parse(dataPayload)
            if (doneData.cancelled) {
              handlers.onDone?.('')
              continue
            }
          } catch { /* fall through */ }
        }

        // 解析业务事件 JSON
        try {
          const event = JSON.parse(dataPayload) as ChatEvent
          if (event.type === 'done' && event.messageId) {
            handlers.onDone?.(event.messageId)
          } else {
            dispatchEvent(event, handlers)
          }
        } catch {
          // 忽略无法解析的行
        }
      }
    }
  } catch (err) {
    if ((err as Error).name === 'AbortError') {
      return
    }
    handlers.onError?.(err as Error)
    throw err
  }
}

function dispatchEvent(event: ChatEvent, handlers: StreamHandlers) {
  switch (event.type) {
    case 'token':
      event.content && handlers.onToken?.(event.content)
      break
    case 'tool_call':
      event.name && handlers.onToolCall?.(event.name, event.data)
      break
    case 'tool_result':
      event.name && handlers.onToolResult?.(event.name, event.data)
      break
    case 'error':
      handlers.onError?.(new Error(event.error ?? 'unknown'))
      break
    case 'done':
      event.messageId && handlers.onDone?.(event.messageId)
      break
  }
}

/** 会话 CRUD API */
export const chatApi = {
  /** 获取会话列表 */
  listSessions: async (params?: { limit?: number; offset?: number }): Promise<SessionItem[]> => {
    const paramsObj: Record<string, string> = {}
    if (params?.limit) paramsObj.limit = String(params.limit)
    if (params?.offset) paramsObj.offset = String(params.offset)
    const qs = new URLSearchParams(paramsObj).toString()
    const url = qs ? `/chat/sessions?${qs}` : '/chat/sessions'
    const res = await aiHttp.get<{ code: number; data: SessionItem[] }>(url)
    return res.data.data ?? []
  },

  /** 获取会话详情（含消息历史） */
  getSession: async (id: string): Promise<SessionDetail | null> => {
    const res = await aiHttp.get<{ code: number; data: SessionDetail }>(
      `/chat/sessions/${id}`
    )
    return res.data.data ?? null
  },

  /** 删除会话 */
  deleteSession: async (id: string): Promise<void> => {
    await aiHttp.delete(`/chat/sessions/${id}`)
  },

  /** 停止当前对话 */
  stop: (sessionId: string): Promise<unknown> =>
    aiHttp.post('/chat/stop', { sessionId }),
}
