/*
 * 文件状态管理（Pinia Store）—— 文件列表、目录树、面包屑、选中项、搜索。
 *
 * 设计依据：docs/DDD.md 10.2（useFileStore: fileList, currentDir, breadcrumb, selectedFiles）。
 *
 * 浏览模式与搜索模式：
 * - 浏览模式：按 currentDirId 加载列表，面包屑由树路径推导
 * - 搜索模式：keyword 非空时加载搜索结果（忽略当前目录），面包屑显示搜索条件
 * 两种模式共用同一份 fileList，切换时重新拉取。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getFileList, getFileTree, searchFiles } from '@/api/file'
import {
  FILE_CATEGORY_CODE,
  type FileItem,
  type FileTreeItem,
  type FileCategory,
  type FileListParams,
} from '@/types/file'

export const useFileStore = defineStore('file', () => {
  /* ========== State ========== */

  /** 当前目录文件列表（浏览或搜索模式共用） */
  const fileList = ref<FileItem[]>([])

  /** 分页信息 */
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(20)

  /** 当前所在目录 ID（0 = 根目录） */
  const currentDirId = ref(0)

  /** 目录树（全部展开由组件控制） */
  const tree = ref<FileTreeItem[]>([])

  /** 面包屑 —— 从根到当前目录的路径节点 */
  const breadcrumb = ref<Array<{ id: number; name: string }>>([])

  /** 表格选中项 */
  const selectedFiles = ref<FileItem[]>([])

  /** 搜索关键字（非空时处于搜索模式） */
  const keyword = ref('')

  /** 搜索类型过滤 */
  const category = ref<FileCategory | undefined>(undefined)

  /** 是否正在加载列表 */
  const loading = ref(false)

  /** 目录树是否已加载（幂等刷新用） */
  const treeLoaded = ref(false)

  /* ========== Getters ========== */

  /** 是否处于搜索模式 */
  const isSearching = computed(() => keyword.value.trim() !== '')

  /** 选中项是否全为文件（用于「预览」类仅文件操作） */
  const hasSelection = computed(() => selectedFiles.value.length > 0)

  /* ========== Actions ========== */

  /** 加载当前目录列表（浏览模式） */
  async function loadList(): Promise<void> {
    loading.value = true
    try {
      const params: FileListParams = {
        parentId: currentDirId.value,
        page: page.value,
        size: pageSize.value,
      }
      const res = await getFileList(params)
      fileList.value = res.records
      total.value = res.total
    } catch {
      // 错误已在拦截器中提示
    } finally {
      loading.value = false
    }
  }

  /** 加载搜索结果（搜索模式） */
  async function loadSearch(): Promise<void> {
    loading.value = true
    try {
      const res = await searchFiles({
        keyword: keyword.value.trim(),
        category: category.value !== undefined ? FILE_CATEGORY_CODE[category.value] : undefined,
        page: page.value,
        size: pageSize.value,
      })
      fileList.value = res.records
      total.value = res.total
    } catch {
      // 错误已在拦截器中提示
    } finally {
      loading.value = false
    }
  }

  /** 加载目录树（首次加载后幂等；refresh 后强制刷新） */
  async function loadTree(force = false): Promise<void> {
    if (treeLoaded.value && !force) return
    try {
      tree.value = await getFileTree()
      treeLoaded.value = true
    } catch {
      // 错误已在拦截器中提示
    }
  }

  /** 统一加载：搜索模式走搜索接口，否则走列表接口 */
  async function load(): Promise<void> {
    if (isSearching.value) {
      await loadSearch()
    } else {
      await loadList()
    }
  }

  /** 进入目录（点击目录树节点/双击列表目录行） */
  async function navigate(dirId: number): Promise<void> {
    selectedFiles.value = []
    currentDirId.value = dirId
    page.value = 1
    await refreshBreadcrumb()
    await load()
  }

  /** 从目录树节点反查路径，重建面包屑 */
  async function refreshBreadcrumb(): Promise<void> {
    breadcrumb.value = []
    if (currentDirId.value === 0) return
    await loadTree()
    buildBreadcrumb(0, tree.value, [])
  }

  /** 递归查找目标目录的祖先链 */
  function buildBreadcrumb(
    parentId: number,
    nodes: FileTreeItem[],
    path: Array<{ id: number; name: string }>,
  ): boolean {
    for (const node of nodes) {
      const nextPath = [...path, { id: node.id, name: node.name }]
      if (node.id === currentDirId.value) {
        breadcrumb.value = nextPath
        return true
      }
      if (buildBreadcrumb(node.id, node.children, nextPath)) return true
    }
    return false
  }

  /** 面包屑跳转（根目录 = 全部） */
  async function navigateBreadcrumb(dirId: number): Promise<void> {
    await navigate(dirId)
  }

  /** 开始搜索（输入关键字回车触发） */
  async function startSearch(kw: string, cat?: FileCategory): Promise<void> {
    keyword.value = kw.trim()
    category.value = cat
    selectedFiles.value = []
    page.value = 1
    await load()
  }

  /** 清除搜索，回到当前目录 */
  async function clearSearch(): Promise<void> {
    keyword.value = ''
    category.value = undefined
    selectedFiles.value = []
    page.value = 1
    await load()
  }

  /** 分页/页大小变化 */
  async function changePage(newPage: number, newSize: number): Promise<void> {
    page.value = newPage
    pageSize.value = newSize
    await load()
  }

  /** 列表数据变更后刷新（上传/重命名/删除等操作后调用）—— 保持当前模式（搜索/浏览） */
  async function refresh(): Promise<void> {
    if (isSearching.value) {
      await loadSearch()
    } else {
      await loadList()
    }
  }

  /** 清空选中 */
  function clearSelection(): void {
    selectedFiles.value = []
  }

  return {
    fileList,
    total,
    page,
    pageSize,
    currentDirId,
    tree,
    breadcrumb,
    selectedFiles,
    keyword,
    category,
    loading,
    isSearching,
    hasSelection,
    load,
    loadTree,
    navigate,
    navigateBreadcrumb,
    startSearch,
    clearSearch,
    changePage,
    refresh,
    clearSelection,
  }
})
