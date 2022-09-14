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
        // '/api': {
        //   target: 'http://172.24.254.212:8563',
        //   changeOrigin: true,
        // },
        '/api': {
          target: 'http://localhost:8888',
          changeOrigin: true,
        }
      }
    }
})
