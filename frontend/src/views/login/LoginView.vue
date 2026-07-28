<template>
  <!--
    LoginView —— 登录页面。
    支持用户名/邮箱 + 密码登录，登录成功后根据 redirect 参数跳转目标页面。
  -->
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-position="top"
    size="large"
    @submit.prevent="handleLogin"
  >
    <h2 class="form-title">账号登录</h2>

    <el-form-item label="用户名 / 邮箱" prop="username">
      <el-input
        v-model="form.username"
        placeholder="请输入用户名或邮箱"
        :prefix-icon="User"
        autocomplete="username"
      />
    </el-form-item>

    <el-form-item label="密码" prop="password">
      <el-input
        v-model="form.password"
        type="password"
        placeholder="请输入密码"
        :prefix-icon="Lock"
        show-password
        autocomplete="current-password"
      />
    </el-form-item>

    <el-form-item>
      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        class="submit-btn"
      >
        {{ loading ? '登录中...' : '登 录' }}
      </el-button>
    </el-form-item>

    <div class="form-footer">
      <router-link :to="{ name: 'Register' }">注册账号</router-link>
      <router-link :to="{ name: 'ForgotPassword' }">忘记密码</router-link>
    </div>
  </el-form>
</template>

<script setup lang="ts">
/*
 * 登录页逻辑 —— 表单校验 → 调用 userStore.login → 跳转。
 *
 * 设计思路：
 * 1. 前端做格式校验（非空），后端做最终校验（用户名存在、密码匹配、登录锁定）。
 * 2. 登录成功后读取 route.query.redirect，跳回登录前访问的页面（如 /files?parentId=123）；
 *    无 redirect 则默认跳转 /files。
 * 3. loading 状态防止重复提交。
 * 4. 输入框设置 autocomplete 属性，浏览器可自动填充已保存的密码。
 *
 * 错误处理：
 * - 后端返回的异常信息（如"用户名或密码错误""账号已锁定"）通过 request.ts 拦截器自动弹 Error 提示。
 * - 此处只需 catch 并取消 loading 状态。
 */
import { ref, reactive, shallowRef } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
    })
    const redirect = (route.query.redirect as string) || '/files'
    router.push(redirect)
  } catch {
    // 错误信息已在 request.ts 拦截器中用 ElMessage 提示
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

.submit-btn {
  width: 100%;
}

.form-footer {
  display: flex;
  justify-content: space-between;
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
