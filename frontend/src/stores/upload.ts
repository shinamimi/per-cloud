/*
 * 传输队列状态管理（Pinia Store）—— 上传/批量下载任务队列 + WebSocket 进度。
 *
 * 设计依据：docs/DDD.md 10.2（useUploadStore: uploadQueue, uploadProgress）+ M9（统一进度通道）。
 *
 * 上传流程编排（见 utils/upload.ts）：
 * 每个文件一个任务：秒传校验 → 分片上传 → 合并；
 * 进度有两个来源：分片本地进度（uploadOneFile 回调）与后端 WS 推送（/ws/progress），
 * 以 WS 为准（后端全局统一进度），WS 未就绪时回退本地进度。
 *
 * WebSocket 消息（与后端 ProgressWebSocketHandler 对齐）：
 * - upload:   { type:"upload", uploadId, uploaded, total }      上传任务按 uploadId 匹配
 * - download: { type:"download", taskId, status, total, done }  打包任务按 taskId 匹配
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { connectWs, disconnectWs, onProgressMessage, isWsConnected } from '@/utils/ws'
import { uploadOneFile } from '@/utils/upload'
import { batchDownload, getUploadPolicy } from '@/api/file'
import { downloadByUrl } from '@/utils/download'
import { formatBytesAuto } from '@/utils/format'
import type { ProgressMessage, TransferTask } from '@/types/file'

/** 任务 ID 生成（上传任务在 init 前尚无后端 uploadId，先用本地 ID 占位） */
let taskSeq = 0
function nextTaskId(): string {
  taskSeq += 1
  return `local-${Date.now()}-${taskSeq}`
}

/** 后端错误码：上传任务数超过限制（并发已满，等待空位后重试） */
const UPLOAD_TASK_EXCEEDED = 10208

/** 并发已满时的重试间隔 */
const RETRY_INTERVAL_MS = 3000

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export const useUploadStore = defineStore('upload', () => {
  /* ========== State ========== */

  /** 传输队列（上传 + 批量下载） */
  const tasks = ref<TransferTask[]>([])

  /** WebSocket 是否已连接 */
  const wsConnected = ref(false)

  /* ========== Getters ========== */

  /** 进行中的任务（用于工具栏角标） */
  const activeCount = computed(() =>
    tasks.value.filter((t) => !['completed', 'failed'].includes(t.status)).length,
  )

  /** 整体上传进度（0-100，无任务时返回 -1 表示不展示） */
  const overallProgress = computed(() => {
    const uploading = tasks.value.filter((t) => t.kind === 'upload' && t.status !== 'completed')
    if (uploading.length === 0) return -1
    const sum = uploading.reduce((acc, t) => acc + t.progress, 0)
    return Math.round(sum / uploading.length)
  })

  /* ========== WebSocket ========== */

  /**
   * 懒建立连接：首次发起传输任务时调用。
   * 上传消息按 uploadId 匹配队列任务，下载消息按 taskId 匹配。
   */
  function ensureConnected(): void {
    if (!isWsConnected()) connectWs()
  }

  /** 处理后端进度消息 */
  function handleProgress(message: ProgressMessage): void {
    if (message.type === 'upload') {
      // 上传进度：按 uploadId 匹配（task.uploadId 在 init 后回填）
      const task = tasks.value.find((t) => t.uploadId === message.uploadId)
      if (!task) return
      task.status = 'uploading'
      task.progress =
        message.total > 0 ? Math.min(Math.round((message.uploaded / message.total) * 100), 99) : 0
      return
    }

    // 下载（批量打包）进度：按 taskId 匹配
    const task = tasks.value.find((t) => t.id === message.taskId)
    if (!task) return

    if (message.status === 'DONE') {
      task.status = 'completed'
      task.progress = 100
      if (message.url) {
        downloadByUrl(message.url, `${task.name}.zip`)
        ElMessage.success('打包下载完成')
      }
      return
    }

    if (message.status === 'FAILED') {
      task.status = 'failed'
      task.error = '打包失败'
      ElMessage.error(`打包下载失败：${task.name}`)
      return
    }

    // PACKING：按已打包文件数 / 总数
    task.status = 'packing'
    task.progress =
      message.total > 0 ? Math.min(Math.round((message.done / message.total) * 100), 99) : 0
  }

  /** 建立连接并注册消息监听（页面挂载时调用一次） */
  function init(): void {
    const off = onProgressMessage(handleProgress)
    ensureConnected()
    // 监听连接状态，供 UI 展示
    const timer = window.setInterval(() => {
      wsConnected.value = isWsConnected()
    }, 2000)
    // 页面卸载时清理（登出由 userStore 负责 disconnectWs）
    window.addEventListener('beforeunload', () => {
      window.clearInterval(timer)
      off()
    })
  }

  /* ========== 上传 ========== */

  /**
   * 上传一组文件到指定目录（后台执行，不等待完成）。
   *
   * 流程：
   * 1. 先拉取上传策略（单文件大小上限 / 并发任务数，VIP 差异化）
   * 2. 超过单文件上限的文件：直接拒绝，不进入传输队列
   * 3. 合法文件入队（pending = 等待中），以并发工作池执行：
   *    - 同时最多 maxConcurrent 个任务在上传
   *    - 其余任务保持等待中，有空闲槽位才被调度
   *
   * 任务入队后立即返回入队数量，进度在传输队列中持续更新；
   * 工作池内部消化全部错误（任务标记 failed），不会产生未处理拒绝。
   */
  async function uploadFiles(files: File[], parentId: number): Promise<number> {
    if (files.length === 0) return 0
    ensureConnected()

    // 拉取上传策略；失败时退化为串行（不预检大小），保证功能可用
    let maxSize = 0
    let maxConcurrent = 1
    try {
      const policy = await getUploadPolicy()
      maxSize = policy.maxSize
      maxConcurrent = policy.maxConcurrent > 0 ? policy.maxConcurrent : 1
    } catch {
      maxSize = 0
      maxConcurrent = 1
    }

    // 超过单文件大小上限的直接拒绝，不入传输队列
    const oversized = files.filter((f) => maxSize > 0 && f.size > maxSize)
    const accepted = files.filter((f) => !oversized.includes(f))
    if (oversized.length > 0) {
      ElMessage.error(
        `以下文件超过单文件大小上限（${formatBytesAuto(maxSize)}），已跳过：${oversized.map((f) => f.name).join('、')}`,
      )
    }
    if (accepted.length === 0) return 0

    // 合法文件入队：全部先标记等待中，由并发工作池调度
    const queue: Array<{ file: File; task: TransferTask }> = accepted.map((file) => {
      const task: TransferTask = {
        id: nextTaskId(),
        name: file.name,
        size: file.size,
        kind: 'upload',
        status: 'pending',
        progress: 0,
      }
      tasks.value.unshift(task)
      return { file, task }
    })

    runWorkers(queue, Math.min(maxConcurrent, queue.length), parentId)
    return accepted.length
  }

  /** 并发工作池：同时最多 workerCount 个任务，其余等待；内部消化全部错误 */
  function runWorkers(
    queue: Array<{ file: File; task: TransferTask }>,
    workerCount: number,
    parentId: number,
  ): void {
    let cursor = 0
    const worker = async () => {
      while (cursor < queue.length) {
        const item = queue[cursor]
        cursor += 1
        const { file, task } = item
        try {
          const result = await uploadOneFile(file, parentId, {
            onStatus: (status) => {
              task.status = status
            },
            onProgress: (percentage) => {
              task.progress = percentage
            },
            onUploadId: (uploadId) => {
              task.uploadId = uploadId
            },
          })
          task.status = 'completed'
          task.progress = 100
          if (result.mode === 'sec') {
            ElMessage.success(`「${file.name}」秒传完成`)
          }
        } catch (e) {
          const code = (e as Error & { code?: number }).code
          if (code === UPLOAD_TASK_EXCEEDED) {
            // 并发已满（如其他会话占用）：任务保持"等待中"，延迟后放回队尾重试
            task.status = 'pending'
            task.progress = 0
            queue.push(item)
            await sleep(RETRY_INTERVAL_MS)
          } else {
            task.status = 'failed'
            task.error = e instanceof Error ? e.message : '上传失败'
          }
        }
      }
    }

    const workers = Array.from({ length: workerCount }, () => worker())
    // 后台执行：工作池不会 reject（错误已全部捕获），无需 await
    void Promise.all(workers)
  }

  /* ========== 批量打包下载 ========== */

  /** 发起批量打包下载（异步任务，进度经 WS 通知） */
  async function createBatchDownload(ids: number[], name: string): Promise<void> {
    ensureConnected()
    const task: TransferTask = {
      id: nextTaskId(),
      name: name,
      size: 0,
      kind: 'download',
      status: 'pending',
      progress: 0,
    }
    tasks.value.unshift(task)

    try {
      const res = await batchDownload({ ids })
      // 后端任务 ID 是 WS 进度匹配的唯一依据
      task.id = res.taskId
      task.status = 'packing'
    } catch {
      task.status = 'failed'
      task.error = '打包任务创建失败'
    }
  }

  /* ========== 队列管理 ========== */

  /** 移除单个任务 */
  function removeTask(id: string): void {
    tasks.value = tasks.value.filter((t) => t.id !== id)
  }

  /** 清空已结束的任务 */
  function clearFinished(): void {
    tasks.value = tasks.value.filter((t) => !['completed', 'failed'].includes(t.status))
  }

  return {
    tasks,
    wsConnected,
    activeCount,
    overallProgress,
    init,
    uploadFiles,
    createBatchDownload,
    removeTask,
    clearFinished,
    disconnectWs,
  }
})
