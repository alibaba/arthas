import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import * as path from "path"

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue({
    reactivityTransform: true
  })],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src/'),
      "vue": "vue/dist/vue.esm-bundler.js"
    }
  },
    server: {
      proxy: {
        // 使用 proxy 实例
        '/api': {
          target: 'http://127.0.0.1:8563',
          changeOrigin: true,
        }
      }
    }
})
