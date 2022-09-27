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
  base:'./',
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8563',
          changeOrigin: true,
        }
      }
    }
})
