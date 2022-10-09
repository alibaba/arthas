import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import * as path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue({
    reactivityTransform: path.resolve(__dirname, "./ui"),
  })],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./ui/src/"),
      "vue": "vue/dist/vue.esm-bundler.js",
    },
  },
  build: {
    rollupOptions: {
      input: {
        main: path.resolve(__dirname, "index.html"),
        ui: path.resolve(__dirname, "ui/index.html"),
      },
    },
  },
  base: "/",
  server: {
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8563",
        changeOrigin: true,
      },
    },
  },
});
