/*
 * 字典状态管理（Pinia Store）—— 缓存后端下发的枚举选项。
 *
 * 设计思路（详见 docs/frontend-standard.md）：
 * - 拉取时机：登录管理后台后拉取一次（loadIfNeeded 幂等）
 * - 不持久化到 localStorage：页面刷新后重新请求
 * - 枚举来自 Java Enum，运行时不变，无需缓存失效机制
 * - 加载失败时静默降级（groups 为空），页面回退到默认文案
 *
 * 为什么用 Pinia 而不是直接调用 API？
 * 多个页面（用户管理、管理员管理）都要用同一份字典，
 * 缓存到内存避免重复请求；getGroup() 提供统一的类型安全访问。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMetaOptions } from '@/api/meta'
import type { OptionItem } from '@/types/meta'

export const useMetaStore = defineStore('meta', () => {
  /** 字典组缓存：groupName -> OptionItem[] */
  const groups = ref<Record<string, OptionItem[]>>({})

  /** 是否已加载成功 —— 用于 loadIfNeeded 幂等判断 */
  const loaded = ref(false)

  /** 是否正在加载 —— 防止并发重复请求 */
  const loading = ref(false)

  /**
   * 拉取字典。失败时静默降级（标记 loaded），
   * 业务页面可正常渲染，只是下拉/标签回退到默认值。
   */
  async function load(): Promise<void> {
    if (loading.value) return
    loading.value = true
    try {
      const res = await getMetaOptions()
      groups.value = res?.groups ?? {}
    } catch {
      groups.value = {}
    } finally {
      loaded.value = true
      loading.value = false
    }
  }

  /** 幂等加载：仅在未加载过时请求一次 */
  async function loadIfNeeded(): Promise<void> {
    if (!loaded.value) {
      await load()
    }
  }

  /** 获取某组字典选项；组不存在时返回空数组 */
  function getGroup(name: string): OptionItem[] {
    return groups.value[name] ?? []
  }

  return { groups, loaded, loading, load, loadIfNeeded, getGroup }
})
