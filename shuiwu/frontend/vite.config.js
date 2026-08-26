import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

// 将 element-plus 桶导入(element-plus/es)改为组件深路径导入。
// 桶文件 es/index.mjs 会引入"全量组件数组"(defaults.mjs)，一旦保留会拖入所有未使用组件；
// 深路径可让 Rollup 只打包真正用到的组件及其依赖。
// 复合组件（table-column / option / menu-item 等）没有独立目录，需映射到父组件路径，
// 父组件 index.mjs 会一并导出它们（如 table/index.mjs 导出 ElTable 与 ElTableColumn）。
const COMPOSITE_PARENT = {
  aside: 'container', footer: 'container', header: 'container', main: 'container',
  'breadcrumb-item': 'breadcrumb',
  'dropdown-item': 'dropdown', 'dropdown-menu': 'dropdown',
  'form-item': 'form',
  'menu-item': 'menu', 'menu-item-group': 'menu', 'sub-menu': 'menu',
  option: 'select', 'option-group': 'select',
  'radio-button': 'radio', 'radio-group': 'radio',
  'table-column': 'table',
  'tab-pane': 'tabs',
  'checkbox-button': 'checkbox', 'checkbox-group': 'checkbox',
  'button-group': 'button',
  'collapse-item': 'collapse',
  'carousel-item': 'carousel',
  'descriptions-item': 'descriptions',
  'avatar-group': 'avatar',
  'anchor-link': 'anchor',
  'skeleton-item': 'skeleton',
  'timeline-item': 'timeline',
  step: 'steps',
  'splitter-panel': 'splitter',
  'tree-node': 'tree'
}

function deepElementPlusResolver(options = {}) {
  const resolvers = ElementPlusResolver(options)
  const toDeep = (resolved) => {
    if (!resolved) return resolved
    if (resolved.from === 'element-plus/es' || resolved.from === 'element-plus/lib') {
      let comp = String(resolved.name || '').replace(/^El/, '')
      comp = comp.replace(/Directive$/, '')
      const kebab = comp.replace(/([A-Z])/g, (m) => '-' + m.toLowerCase()).replace(/^-/, '')
      const parent = COMPOSITE_PARENT[kebab] || kebab
      resolved.from = `element-plus/es/components/${parent}/index.mjs`
    }
    return resolved
  }
  return resolvers.map((r) => ({ ...r, resolve: async (name) => toDeep(await r.resolve(name)) }))
}

export default defineConfig({
  plugins: [
    vue(),
    // 按需自动引入 ElMessage / ElMessageBox / ElLoading 等 API（深路径 + 样式）
    AutoImport({
      dts: false,
      resolvers: [deepElementPlusResolver()]
    }),
    // Element Plus 组件按需引入：模板中的 <el-*> 自动按需加载组件与样式
    Components({
      dts: false,
      resolvers: [deepElementPlusResolver({ importStyle: 'css' })]
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://127.0.0.1:8080',
        ws: true,
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        // 按三方库拆分独立 chunk，利用浏览器长期缓存
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
          if (
            /[\\/]vue[\\/]/.test(id) ||
            id.includes('vue-router') ||
            id.includes('pinia') ||
            id.includes('@vue') ||
            id.includes('axios') ||
            id.includes('dayjs')
          ) {
            return 'vue-vendor'
          }
          return undefined
        }
      }
    }
  }
})
