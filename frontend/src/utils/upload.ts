/*
 * 上传编排 —— SHA256 计算 + 分片上传（秒传/断点续传）。
 *
 * 流程说明（与后端 UploadServiceImpl 对齐）：
 * 1. 前端计算 SHA256 → POST /sec（全站 Hash 命中 → 秒传完成，返回 { instant, file }）
 * 2. 未命中 → POST /upload/init（校验配额/单文件上限/并发数，返回 uploadId + chunkSize + totalChunks）
 * 3. GET /upload/progress/{uploadId} 取已传分片（断点续传）
 * 4. 按 chunkSize 切片 → 并发 POST /upload/chunk 上传（seq 从 1 开始，跳过已传分片）
 * 5. POST /upload/merge 合并 → 写 t_file → 配额一次性扣减
 *
 * 进度处理：
 * - 分片阶段按「已传片数 / 总片数」计算本地进度
 * - 后端 WebSocket 同步推送（/ws/progress）作为补充，store 层合并展示
 *
 * 并发上传：
 * - 5 个分片并发上传，充分利用带宽
 * - 使用 Promise 并发控制，避免浏览器连接数限制
 */
import { sha256 as jsSha256 } from 'js-sha256'
import { secUpload, uploadChunk, uploadInit, uploadMerge, uploadProgress } from '@/api/file'

/** 上传过程回调 —— 供 store 同步队列状态与进度 */
export interface UploadHandlers {
  onStatus: (status: 'hashing' | 'uploading' | 'merging') => void
  onProgress: (percentage: number) => void
  /** 后端 uploadId 产生后回调（store 用它匹配 WS 进度消息） */
  onUploadId?: (uploadId: string) => void
}

/** 单文件上传结果 */
export interface UploadResult {
  /** sec=秒传命中；uploaded=正常分片上传 */
  mode: 'sec' | 'uploaded'
  uploadId: string
}

/**
 * 计算文件 SHA256 —— 用于秒传全站索引。
 * 优先用 Web Crypto API（HTTPS/localhost 安全上下文可用，性能好）；
 * 公网明文 HTTP 下 crypto.subtle 不可用，降级为纯 JS 实现（js-sha256，结果一致）。
 */
export async function sha256(file: File): Promise<string> {
  const buffer = await file.arrayBuffer()
  if (globalThis.crypto?.subtle) {
    const digest = await crypto.subtle.digest('SHA-256', buffer)
    return Array.from(new Uint8Array(digest))
      .map((byte) => byte.toString(16).padStart(2, '0'))
      .join('')
  }
  return jsSha256(new Uint8Array(buffer))
}

/**
 * 并发控制：限制同时执行的异步任务数量
 */
async function concurrentMap<T, R>(
  items: T[],
  concurrency: number,
  fn: (item: T) => Promise<R>,
): Promise<R[]> {
  const results: R[] = []
  const executing = new Set<Promise<void>>()

  for (const item of items) {
    const p = fn(item).then((result) => {
      results.push(result)
    })
    const wrapped = p.then(() => {
      executing.delete(wrapped)
    })
    executing.add(wrapped)

    if (executing.size >= concurrency) {
      await Promise.race(executing)
    }
  }

  await Promise.all(executing)
  return results
}

/**
 * 上传单个文件（含秒传校验）。
 *
 * @param file     待上传文件
 * @param parentId 目标目录 ID
 * @param handlers 状态/进度回调（驱动上传队列 UI）
 * @param teamId   团队 ID（缺省为个人空间）
 * @param concurrentChunks 并发分片数（默认 5）
 */
export async function uploadOneFile(
  file: File,
  parentId: number,
  handlers: UploadHandlers,
  teamId?: number,
  concurrentChunks: number = 5,
): Promise<UploadResult> {
  // 1. 计算 SHA256 → 秒传校验
  handlers.onStatus('hashing')
  handlers.onProgress(0)
  const hash = await sha256(file)

  const sec = await secUpload({
    fileHash: hash,
    fileName: file.name,
    fileSize: file.size,
    parentId,
    teamId,
  })
  if (sec.instant) {
    return { mode: 'sec', uploadId: '' }
  }

  // 2. 初始化分片上传（后端按大小自适应分片，totalChunks 以返回值为准）
  const init = await uploadInit({
    fileName: file.name,
    fileSize: file.size,
    fileHash: hash,
    parentId,
    teamId,
  })
  const { uploadId, chunkSize, totalChunks } = init
  handlers.onUploadId?.(uploadId)

  // 3. 断点续传：查询已上传分片（1 起），跳过
  let uploaded = new Set<number>()
  try {
    const progress = await uploadProgress(uploadId)
    uploaded = new Set(progress.uploadedChunks)
  } catch {
    // 进度查询失败不阻断：全量重传
  }

  handlers.onStatus('uploading')

  // 4. 并发上传分片（seq 从 1 开始，跳过已传分片）
  const pendingChunks: number[] = []
  for (let seq = 1; seq <= totalChunks; seq++) {
    if (!uploaded.has(seq)) {
      pendingChunks.push(seq)
    }
  }

  let completedChunks = totalChunks - pendingChunks.length
  const totalPending = pendingChunks.length

  // 使用并发控制上传分片
  await concurrentMap(pendingChunks, concurrentChunks, async (seq) => {
    const start = (seq - 1) * chunkSize
    const end = Math.min(start + chunkSize, file.size)
    await uploadChunk(uploadId, seq, file.slice(start, end))
    completedChunks++
    handlers.onProgress(Math.round((completedChunks / totalChunks) * 100))
  })

  // 5. 合并分片（后端一次性扣减配额）
  handlers.onStatus('merging')
  await uploadMerge({ uploadId })
  handlers.onProgress(100)

  return { mode: 'uploaded', uploadId }
}
