/*
 * WebSocket 连接管理（单例）—— 统一进度推送通道 /ws/progress?token=xxx。
 *
 * 设计依据：docs/DDD.md M9 —— 每个用户一条连接，taskId 区分任务；
 * 消息格式 { type, taskId, current, total, percentage, status }。
 *
 * 为什么做成单例？
 * 上传进度（分片）与批量打包通知共用同一通道，全应用只需一条连接；
 * 由 uploadStore 在首次发起传输任务时懒建立，避免每个页面各建一条。
 */

import type { ProgressMessage } from '@/types/file'

const WS_BASE = import.meta.env.VITE_WS_URL || '/ws'

type MessageHandler = (message: ProgressMessage) => void

let socket: WebSocket | null = null
let connected = false
const handlers = new Set<MessageHandler>()

/** 连接建立/关闭回调（供 store 刷新连接状态） */
let onOpen: (() => void) | null = null
let onClose: (() => void) | null = null

/** 是否已建立连接（或正在建立） */
export function isWsConnected(): boolean {
  return connected
}

/** 注册消息处理器，返回取消注册函数 */
export function onProgressMessage(handler: MessageHandler): () => void {
  handlers.add(handler)
  return () => {
    handlers.delete(handler)
  }
}

/**
 * 解析为绝对 WebSocket URL。
 * Safari 的 WebSocket 构造器要求 ws:// 或 wss:// 绝对地址（相对路径会抛
 * "Wrong url scheme for WebSocket"），Chrome 会自动解析相对路径，因此必须显式拼全。
 * 派生规则：https 页面 → wss，其余 → ws；host 沿用当前页面。
 */
function resolveWsUrl(base: string): string {
  if (/^wss?:\/\//i.test(base)) return base
  const scheme = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${scheme}//${window.location.host}${base.startsWith('/') ? '' : '/'}${base}`
}

/**
 * 建立（或复用）WebSocket 连接。
 * token 从 localStorage 读取，后端拦截器按 token 参数校验身份。
 * 连接断开时自动重连一次（令牌仍有效时）；失败后由调用方择机重试。
 */
export function connectWs(): void {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return
  }

  const token = localStorage.getItem('token')
  if (!token) return

  const url = `${resolveWsUrl(WS_BASE)}/progress?token=${encodeURIComponent(token)}`
  socket = new WebSocket(url)
  connected = false

  socket.onopen = () => {
    connected = true
    onOpen?.()
  }

  socket.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data) as ProgressMessage
      if (message && ('taskId' in message || 'uploadId' in message)) {
        handlers.forEach((handler) => handler(message))
      }
    } catch {
      // 忽略无法解析的消息，保持连接存活
    }
  }

  socket.onclose = () => {
    connected = false
    onClose?.()
    socket = null
  }

  socket.onerror = () => {
    socket?.close()
  }
}

/** 主动关闭连接（登出时调用） */
export function disconnectWs(): void {
  handlers.clear()
  socket?.close()
  socket = null
  connected = false
}
