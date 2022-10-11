import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import * as path from "path";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const proxyTarget =`${env.VITE_ARTHAS_PROXY_IP}:${env.VITE_ARTHAS_PROXY_PORT}`;
  console.log("Arthas proxy :", proxyTarget);
  return {
    plugins: [vue({
      reactivityTransform: true,
    })],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./ui/src/"),
      },
    },
    build: {
      emptyOutDir: true,
      minify: "esbuild",
      rollupOptions: {
        input: {
          main: path.resolve(__dirname, "index.html"),
          ui: path.resolve(__dirname, "ui/index.html"),
        },
        output: {
          chunkFileNames: "static/js/[name]-[hash].js",
          entryFileNames: "static/js/[name]-[hash].js",
          assetFileNames: "static/[ext]/[name]-[hash].[ext]",
        },
      },
    },
    esbuild: {
      drop: ["console", "debugger"],
    },
    define: {
      "__VUE_OPTIONS_API__": false,
    },
    base: "/",
    server: {
      proxy: {
        "/api": {
          target: `http://${proxyTarget}`,
          changeOrigin: true,
        }
      },
    },
  };
});
