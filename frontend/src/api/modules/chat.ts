/**
 * AI 对话 API (SSE 流式)
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

      // SSE 协议以 \n\n 分隔事件
      const parts = buffer.split('\n\n')
      buffer = parts.pop() ?? ''

      for (const part of parts) {
        const line = part.trim()
        if (!line.startsWith('data:')) continue
        const payload = line.slice(5).trim()
        if (!payload) continue
        try {
          const event = JSON.parse(payload) as ChatEvent
          dispatchEvent(event, handlers)
        } catch {
          // 忽略无法解析的行
        }
      }
    }
  } catch (err) {
    if ((err as Error).name === 'AbortError') {
      // 用户主动停止
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

export const chatApi = {
  listSessions: () => Promise.resolve([] as Array<{ id: string; title: string; updatedAt: string }>),
  getSession: (_id: string) => Promise.resolve({ messages: [] as ChatMessage[] }),
  stop: (sessionId: string) =>
    aiHttp.post('/chat/stop', { sessionId }),
}
