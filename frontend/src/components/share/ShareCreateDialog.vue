<!--
  ShareCreateDialog —— 创建分享对话框。
  选项：有效期（永久/按天）、提取码（默认跟随管理端配置）、下载策略（允许/禁止下载，允许时可选次数上限）、转存开关。
  对应 docs/share-module.md §四（创建分享）与配置中心 share.* 默认值。
-->
<template>
  <el-dialog
    :model-value="visible"
    title="创建分享"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => emit('update:visible', v)"
    @open="resetForm"
  >
    <el-form label-width="100px">
      <el-form-item label="有效期">
        <el-radio-group v-model="form.validType">
          <el-radio value="PERMANENT">永久有效</el-radio>
          <el-radio value="DAYS">按天</el-radio>
        </el-radio-group>
        <el-input-number
          v-if="form.validType === 'DAYS'"
          v-model="form.validDays"
          :min="1"
          :max="maxValidDays"
          class="days-input"
        />
      </el-form-item>

      <el-form-item label="提取码">
        <el-radio-group v-model="requirePassword">
          <el-radio :value="false">无提取码</el-radio>
          <el-radio :value="true">设置提取码</el-radio>
        </el-radio-group>
        <el-input
          v-if="requirePassword"
          v-model="form.accessPassword"
          placeholder="访客访问需输入"
          maxlength="64"
          class="pwd-input"
        />
      </el-form-item>

      <el-form-item label="下载策略">
        <el-radio-group v-model="form.allowDownload">
          <el-radio :value="true">允许下载</el-radio>
          <el-radio :value="false">禁止下载</el-radio>
        </el-radio-group>
        <el-input-number
          v-if="form.allowDownload"
          v-model="form.maxDownload"
          :min="0"
          :max="100000"
          placeholder="0=不限"
          class="days-input"
        />
        <span v-if="form.allowDownload" class="hint">0 表示不限次数</span>
      </el-form-item>

      <el-form-item label="允许转存">
        <el-switch v-model="form.allowSave" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createShare } from '@/api/share'
import type { ShareCreateRequest, ShareItem, ShareValidType } from '@/types/share'

const props = defineProps<{
  visible: boolean
  fileId: number
}>()

const emit = defineEmits<{
  'update:visible': [v: boolean]
  created: [share: ShareItem]
}>()

const submitting = ref(false)
const requirePassword = ref(false)
/** 分享最长有效期（管理端配置，前端兜底上限） */
const maxValidDays = 365

const form = reactive({
  validType: 'PERMANENT' as ShareValidType,
  validDays: 7,
  requirePassword: false,
  accessPassword: '',
  allowDownload: true,
  maxDownload: 0,
  allowSave: true,
})

function resetForm() {
  Object.assign(form, {
    validType: 'PERMANENT',
    validDays: 7,
    requirePassword: false,
    accessPassword: '',
    allowDownload: true,
    maxDownload: 0,
    allowSave: true,
  })
  requirePassword.value = false
}

async function handleSubmit() {
  if (form.validType === 'DAYS' && (!form.validDays || form.validDays <= 0)) {
    ElMessage.warning('请输入有效天数')
    return
  }
  if (requirePassword.value && !form.accessPassword.trim()) {
    ElMessage.warning('请输入提取码')
    return
  }
  const payload: ShareCreateRequest = {
    fileId: props.fileId,
    validType: form.validType,
    validDays: form.validType === 'DAYS' ? form.validDays : undefined,
    requirePassword: requirePassword.value,
    accessPassword: requirePassword.value ? form.accessPassword.trim() : undefined,
    allowDownload: form.allowDownload,
    maxDownload: form.allowDownload ? form.maxDownload : undefined,
    allowSave: form.allowSave,
  }
  submitting.value = true
  try {
    const share = await createShare(payload)
    ElMessage.success('分享创建成功')
    emit('created', share)
    emit('update:visible', false)
  } catch {
    // 错误已由拦截器提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.days-input {
  width: 140px;
  margin-left: 12px;
}

.pwd-input {
  width: 200px;
  margin-left: 12px;
}

.hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
