<!--
  UploadDialog —— 上传对话框。
  支持拖拽/点选多文件；确认后交由 uploadStore.uploadFiles 编排
  （秒传校验 → 分片上传 → 合并，进度见传输队列面板）。
-->
<template>
  <el-dialog
    :model-value="visible"
    title="上传文件"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-upload
      v-model:file-list="fileList"
      drag
      multiple
      :auto-upload="false"
      :limit="0"
      class="upload-zone"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或<em>点击选择文件</em></div>
      <template #tip>
        <div class="el-upload__tip">
          支持多文件上传；相同内容会自动秒传，大文件自动分片上传
        </div>
      </template>
    </el-upload>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleConfirm">开始上传</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { UploadUserFile, UploadRawFile } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useUploadStore } from '@/stores/upload'

const props = defineProps<{
  visible: boolean
  parentId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const uploadStore = useUploadStore()

const fileList = ref<UploadUserFile[]>([])

async function handleConfirm() {
  const files = fileList.value
    .map((f) => f.raw)
    .filter((f): f is UploadRawFile => !!f)
  if (files.length === 0) return

  // 确认后立即关闭：任务由 uploadStore 后台编排（秒传校验 → 分片上传 → 合并），
  // 进度在传输队列面板中持续更新，无需等待上传完成
  emit('update:visible', false)
  fileList.value = []
  const count = await uploadStore.uploadFiles(files, props.parentId)
  if (count > 0) {
    ElMessage.info(`已创建 ${count} 个上传任务，可到传输队列查看进度`)
  }
}

function handleCancel() {
  fileList.value = []
  emit('update:visible', false)
}
</script>

<style scoped>
.upload-zone {
  width: 100%;
}
</style>
