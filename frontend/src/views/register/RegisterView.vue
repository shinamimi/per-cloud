<template>
  <!--
    RegisterView —— 注册页面。
    邮箱验证码注册流程：填写信息 → 发送验证码 → 提交注册 → 自动登录。
  -->
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-position="top"
    size="large"
    @submit.prevent="handleRegister"
  >
    <h2 class="form-title">注册账号</h2>

    <el-form-item label="用户名" prop="username">
      <el-input
        v-model="form.username"
        placeholder="3-32 位字符"
        :prefix-icon="User"
        autocomplete="username"
      />
    </el-form-item>

    <el-form-item label="邮箱" prop="email">
      <el-input
        v-model="form.email"
        placeholder="请输入邮箱地址"
        :prefix-icon="Message"
        autocomplete="email"
      />
    </el-form-item>

    <el-form-item label="密码" prop="password">
      <el-input
        v-model="form.password"
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
        placeholder="再次输入密码"
        :prefix-icon="Lock"
        show-password
        autocomplete="new-password"
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

    <el-form-item>
      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        class="submit-btn"
      >
        {{ loading ? '注册中...' : '注 册' }}
      </el-button>
    </el-form-item>

    <div class="form-footer">
      <span>已有账号？</span>
      <router-link :to="{ name: 'Login' }">去登录</router-link>
    </div>
  </el-form>
</template>

<script setup lang="ts">
/*
 * 注册页逻辑：
 * 1. 填写基本信息（用户名、邮箱、密码、确认密码）
 * 2. 点击"获取验证码" → 校验邮箱格式 → 调用 sendCodeApi
 * 3. 输入验证码 → 提交 → userStore.register → 自动登录 → 跳转 /files
 *
 * 发送验证码冷却机制：
 * 点击后前端进入 60 秒冷却倒计时（cooldown ref），按钮显示剩余秒数。
 * 此冷却与后端 Redis 60 秒冷却协同，双重保证。
 *
 * 密码校验规则（与后端 RegisterRequest 一致）：
 * - 长度：8-20 位
 * - 必须同时包含字母和数字
 * - 确认密码需要与前一次输入一致
 *
 * 为什么在组件内使用 el-form 自带的校验而非 composable？
 * Element Plus 的 el-form 提供了声明式的 rules 校验，简洁且与 UI 绑定。
 * 对于跨组件复用的校验逻辑（如密码规则），后续可提取到 composables/ 目录。
 */
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { sendCodeApi } from '@/api/auth'
import { CaptchaType } from '@/types/auth'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const codeLoading = ref(false)
const cooldown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  code: '',
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
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度 3-32 位', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
  ],
}

/* ========== 发送验证码 ========== */

async function handleSendCode() {
  const emailValid = await formRef.value?.validateField('email').catch(() => false)
  if (!emailValid) return

  codeLoading.value = true
  try {
    await sendCodeApi({ email: form.email, captchaType: CaptchaType.REGISTER })
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

/* ========== 提交注册 ========== */

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      email: form.email,
      code: form.code,
    })
    router.push('/files')
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
}

.form-footer a {
  color: #409eff;
  text-decoration: none;
}

.form-footer a:hover {
  text-decoration: underline;
}
</style>
