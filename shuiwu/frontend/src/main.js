import { createApp } from 'vue'
import { createPinia } from 'pinia'
// 仅按需引入实际用到的图标（菜单/大屏按字符串动态渲染的集合）
// 命名导入可被 tree-shaking，避免把 @element-plus/icons-vue 全量(~300个)打进包
import {
  Monitor, Odometer, DataBoard, Platform, SetUp, User, Cpu, Bell,
  Aim, DataAnalysis, ChatDotRound, Document, DataLine, PieChart, Grid,
  Menu, Cellphone,
  Search, Plus, Refresh, Setting, Download, Lock, SwitchButton
} from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)

// 全局注册 Element Plus 图标（菜单/大屏图标来自后端，按字符串动态渲染）
const iconComponents = {
  Monitor, Odometer, DataBoard, Platform, SetUp, User, Cpu, Bell,
  Aim, DataAnalysis, ChatDotRound, Document, DataLine, PieChart, Grid,
  Menu, Cellphone,
  Search, Plus, Refresh, Setting, Download, Lock, SwitchButton
}
for (const [name, comp] of Object.entries(iconComponents)) {
  app.component(name, comp)
}

app.use(createPinia())
app.use(router)

app.mount('#app')
