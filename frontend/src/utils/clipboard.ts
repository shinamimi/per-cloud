/*
 * 剪贴板工具 —— 安全上下文（HTTPS/localhost）下用 navigator.clipboard，
 * 非安全上下文（公网明文 HTTP）下降级 document.execCommand('copy')，
 * 保证复制在公网 HTTP 部署下也能生效。返回是否真正写入剪贴板。
 */
export function copyText(text: string): Promise<boolean> {
  if (navigator.clipboard?.writeText) {
    return navigator.clipboard
      .writeText(text)
      .then(() => true)
      .catch(() => fallbackCopy(text))
  }
  return Promise.resolve(fallbackCopy(text))
}

/** execCommand 降级：临时 textarea + select + copy */
function fallbackCopy(text: string): boolean {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.top = '-1000px'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  try {
    textarea.select()
    // iOS 需 range 选中，确保 select 生效
    textarea.setSelectionRange(0, text.length)
    return document.execCommand('copy')
  } catch {
    return false
  } finally {
    document.body.removeChild(textarea)
  }
}