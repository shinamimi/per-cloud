/*
 * 下载工具 —— Blob 保存与直链下载。
 *
 * 设计思路：
 * - 单文件：后端 302 重定向到 MinIO presigned URL，前端经 requestBlob 拿 Blob 后本地保存
 * - 打包文件：WebSocket 通知携带 presigned URL，前端直连 MinIO 下载（无需鉴权头）
 */

/**
 * 将 Blob 保存为本地文件。
 * 使用 <a download> 触发下载；在 URL.createObjectURL 的生命周期结束后及时释放。
 */
export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * 通过 presigned URL 直连下载（打包文件场景）。
 * presigned URL 已带鉴权签名，无需额外请求头。
 */
export function downloadByUrl(url: string, filename: string): void {
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
