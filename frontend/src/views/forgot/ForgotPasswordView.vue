<template>
  <!--
    ForgotPasswordView —— 找回密码页面。
    两步流程：
    Step 1: 输入邮箱 → 点击"发送验证码" → 后端校验邮箱是否已注册并发送验证码
    Step 2: 输入验证码 + 新密码 + 确认密码 → 提交重置
  -->
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-position="top"
    size="large"
    @submit.prevent="handleReset"
  >
    <h2 class="form-title">找回密码</h2>

    <el-form-item label="邮箱" prop="email">
      <el-input
        v-model="form.email"
        placeholder="请输入注册时使用的邮箱"
        :prefix-icon="Message"
        autocomplete="email"
      />
    </el-form-item>

    <el-form-item label="验证码" prop="code">
      <div class="code-row">
        <el-input
          v-model="form.code"
          placeholder="输入邮箱验证码"
          class="code-input"
        />
        <el-button
          :disabled="cooldown > 0"
          :loading="codeLoading"
          @click="handleSendCode"
        >
          {{ cooldown > 0 ? `${cooldown}s` : '获取验证码' }}
        </el-button>
      </div>
    </el-form-item>

    <el-form-item label="新密码" prop="newPassword">
      <el-input
        v-model="form.newPassword"
        type="password"
        placeholder="8-20 位，需包含字母和数字"
        :prefix-icon="Lock"
        show-password
        autocomplete="new-password"
      />
    </el-form-item>

    <el-form-item label="确认密码" prop="confirmPassword">
      <el-input
        v-model="form.confirmPassword"
        type="password"
        placeholder="再次输入新密码"
        :prefix-icon="Lock"
        show-password
        autocomplete="new-password"
      />
    </el-form-item>

    <el-form-item>
      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        class="submit-btn"
      >
        {{ loading ? '重置中...' : '重 置 密 码' }}
      </el-button>
    </el-form-item>

    <div class="form-footer">
      <span>想起密码了？</span>
      <router-link :to="{ name: 'Login' }">去登录</router-link>
    </div>
  </el-form>
</template>

<script setup lang="ts">
/*
 * 找回密码页逻辑：
 *
 * 流程说明（与后端 forgot-password + reset-password 接口对应）：
 * 1. 用户输入邮箱 → 点击"获取验证码" → POST /api/auth/forgot-password
 *    （此接口会先校验邮箱是否已注册，未注册返回错误）
 * 2. 用户输入验证码 + 新密码 + 确认密码
 * 3. 点击"重置密码" → POST /api/auth/reset-password
 * 4. 成功 → 提示用户 → 跳转登录页
 *
 * 与注册页的验证码区别：
 * - 注册页验证码类型为 REGISTER
 * - 找回密码页验证码类型为 RESET_PASSWORD
 * 两者 Redis Key 不同，防止串用攻击。
 */
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Lock, Message } from '@element-plus/icons-vue'
import { forgotPasswordApi, resetPasswordApi } from '@/api/auth'
import { CaptchaType } from '@/types/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const codeLoading = ref(false)
const cooldown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

/* ========== 表单校验规则 ========== */

const validatePassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (value.length < 8 || value.length > 20) {
    callback(new Error('密码长度需 8-20 位'))
  } else if (!/(?=.*[A-Za-z])(?=.*\d)/.test(value)) {
    callback(new Error('密码必须包含字母和数字'))
  } else {
    callback()
  }
}

const validateConfirm = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

/* ========== 发送验证码 ========== */

async function handleSendCode() {
  const emailValid = await formRef.value?.validateField('email').catch(() => false)
  if (!emailValid) return

  codeLoading.value = true
  try {
    await forgotPasswordApi({ email: form.email, captchaType: CaptchaType.RESET_PASSWORD })
    ElMessage.success('验证码已发送到邮箱，请查收')
    startCooldown()
  } catch {
    // 错误已在拦截器中提示
  } finally {
    codeLoading.value = false
  }
}

function startCooldown() {
  cooldown.value = 60
  timer = setInterval(() => {
    cooldown.value--
    if (cooldown.value <= 0) {
      if (timer) clearInterval(timer)
    }
  }, 1000)
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

/* ========== 重置密码 ========== */

async function handleReset() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await resetPasswordApi({
      email: form.email,
      code: form.code,
      newPassword: form.newPassword,
    })
    ElMessage.success('密码重置成功，请重新登录')
    router.push({ name: 'Login' })
  } catch {
    // 错误已在拦截器中提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.form-title {
  margin: 0 0 24px;
  font-size: 20px;
  font-weight: 600;
  text-align: center;
  color: #303133;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-input {
  flex: 1;
}

.submit-btn {
  width: 100%;
}

.form-footer {
  display: flex;
  justify-content: center;
  gap: 4px;
  font-size: 14px;
  color: #606266;
}

.form-footer a {
  color: #409eff;
  text-decoration: none;
}

.form-footer a:hover {
  text-decoration: underline;
}
</style>
