import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import * as path from "path"

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
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
          target: 'localhost:',
          changeOrigin: true,
          // configure: (proxy, options) => {
          //   // proxy 是 'http-proxy' 的实例
          // }
        }
      }
    }
})
