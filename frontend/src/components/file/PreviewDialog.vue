<!--
  PreviewDialog —— 文件预览对话框。
  按 docs/file-module.md 六：图片/视频/音频/PDF 浏览器原生渲染，文本直接展示后端 content；
  图片优先使用 thumbnailUrl（缩略图），视频/音频/PDF 使用 presigned URL（10 分钟有效）。
-->
<template>
  <el-dialog
    v-model="visible"
    :title="preview?.name ?? '预览'"
    width="760px"
    top="6vh"
    :close-on-click-modal="false"
    draggable
    destroy-on-close
    @closed="reset"
  >
    <div v-loading="loading" class="preview-body">
      <!-- 图片 -->
      <img v-if="preview?.type === 'IMAGE'" :src="imageUrl" class="preview-media" alt="预览" />

      <!-- 视频 -->
      <video
        v-else-if="preview?.type === 'VIDEO'"
        :src="mediaUrl"
        controls
        class="preview-media"
      />

      <!-- 音频 -->
      <audio v-else-if="preview?.type === 'AUDIO'" :src="mediaUrl" controls class="preview-audio" />

      <!-- PDF -->
      <iframe v-else-if="preview?.type === 'PDF'" :src="mediaUrl" class="preview-pdf" />

      <!-- 文本（后端直接返回内容，无需再请求 url） -->
      <pre v-else-if="preview?.type === 'TEXT'" class="preview-text">{{ preview.content }}</pre>

      <el-empty v-else-if="!loading" description="该文件类型暂不支持预览" :image-size="80" />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { previewFile } from '@/api/file'
import type { FileItem, FilePreviewResponse } from '@/types/file'

const props = defineProps<{
  visible: boolean
  file: FileItem | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const visible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

const loading = ref(false)
const preview = ref<FilePreviewResponse | null>(null)

/** 图片优先缩略图，无缩略图回退原图 */
const imageUrl = computed(() => preview.value?.thumbnailUrl || preview.value?.url || '')

/** 音视频/PDF 直链（presigned URL） */
const mediaUrl = computed(() => preview.value?.url ?? '')

/** 打开时拉取预览信息（presigned URL / 文本内容） */
watch(
  () => props.visible,
  async (open) => {
    if (!open || !props.file) return
    loading.value = true
    preview.value = null
    try {
      preview.value = await previewFile(props.file.id)
    } catch {
      // 错误已在拦截器中提示
    } finally {
      loading.value = false
    }
  },
)

function reset() {
  preview.value = null
}
</script>

<style scoped>
.preview-body {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-media {
  max-width: 100%;
  max-height: 70vh;
  display: block;
}

.preview-audio {
  width: 100%;
}

.preview-pdf {
  width: 100%;
  height: 70vh;
  border: none;
}

.preview-text {
  width: 100%;
  max-height: 70vh;
  overflow: auto;
  margin: 0;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
