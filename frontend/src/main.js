import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router/index.js'
import { i18n } from './i18n/index.js'

// 前端应用入口：依次挂载路由、多语言和 Element Plus 组件库。
createApp(App).use(router).use(i18n).use(ElementPlus).mount('#app')

