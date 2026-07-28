/// <reference types="vite/client" />

/*
 * .vue 文件的类型声明 —— 让 TypeScript 能识别 .vue 单文件组件。
 * Vue 3 SFC 默认导出为 Component 类型，不加此声明则 import .vue 时 TS 报错。
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

/*
 * 环境变量类型扩展 —— 让 process.env / import.meta.env 有类型提示。
 * 需要与 .env.development / .env.production 中的键一一对应。
 */
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_WS_URL: string
  readonly VITE_APP_TITLE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
