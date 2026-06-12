/**
 * Vue 3 应用入口文件。
 *
 * 负责：
 * 1. 创建 Vue 3 应用实例
 * 2. 全局注册 Element Plus UI 组件库及其样式
 * 3. 加载全局自定义样式
 * 4. 挂载根组件 App.vue 到 #app DOM 节点
 *
 * 设计模式：单一入口（Single Entry Point）—— 所有全局配置集中在此文件完成。
 */

import { createApp } from 'vue'
// Element Plus: 基于 Vue 3 的企业级 UI 组件库，提供表格、表单、对话框、图表容器等组件
import ElementPlus from 'element-plus'
// Element Plus 官方样式文件（包含所有组件的 CSS）
import 'element-plus/dist/index.css'
// 全局自定义样式（覆盖 Element Plus 默认主题色等）
import './style.css'
// 根组件 —— 单文件 SPA，包含三个视图（仪表盘/航班查询/AI 助手）的切换逻辑
import App from './App.vue'

// 创建应用实例，链式调用安装插件并挂载
createApp(App).use(ElementPlus).mount('#app')

