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
import { batchDownload } from '@/api/file'
import { downloadByUrl } from '@/utils/download'
import type { ProgressMessage, TransferTask } from '@/types/file'

/** 任务 ID 生成（上传任务在 init 前尚无后端 uploadId，先用本地 ID 占位） */
let taskSeq = 0
function nextTaskId(): string {
  taskSeq += 1
  return `local-${Date.now()}-${taskSeq}`
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
   * 上传一组文件到指定目录。
   * 每个文件独立任务、串行执行（避免并发数限制与进度混乱）。
   */
  async function uploadFiles(files: File[], parentId: number): Promise<void> {
    if (files.length === 0) return
    ensureConnected()

    for (const file of files) {
      const task: TransferTask = {
        id: nextTaskId(),
        name: file.name,
        size: file.size,
        kind: 'upload',
        status: 'pending',
        progress: 0,
      }
      tasks.value.unshift(task)

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
        task.status = 'failed'
        task.error = e instanceof Error ? e.message : '上传失败'
      }
    }
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
