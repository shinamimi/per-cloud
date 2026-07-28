import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

/*
 * 应用入口 —— 挂载全局插件。
 *
 * 设计思路：
 * - Pinia：全局状态管理，替代 Vuex，与 Vue 3 Composition API 原生兼容。
 * - Element Plus：UI 组件库，使用中文语言包。
 * - Element Plus Icons：全局注册图标组件，模板中可直接 <el-icon><User /></el-icon> 使用。
 * - Router：Vue Router v4，Hash 模式（避免生产环境 Nginx 未配置 fallback 时刷新 404）。
 */
const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
