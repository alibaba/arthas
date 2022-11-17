import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import * as path from "path";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const proxyTarget =
    `${env.VITE_ARTHAS_PROXY_IP}:${env.VITE_ARTHAS_PROXY_PORT}`;

  console.log("Arthas proxy :", proxyTarget);
  let outDir, input, root, proxy;

  if (mode === "tunnel") {
    outDir = path.resolve(__dirname, `dist/tunnel`);
    root = "./all/tunnel";
    input = {
      tunnel: path.resolve(__dirname, "all/tunnel/index.html"),
      apps: path.resolve(__dirname, "all/tunnel/apps.html"),
      agents: path.resolve(__dirname, "all/tunnel/agents.html"),
    };
    proxy = {
      "/api": {
        target: `http://${proxyTarget}`,
        changeOrigin: true,
      },
    };
  } else if (mode === "ui") {
    outDir = path.resolve(__dirname, `dist/ui`);
    root = "./all/ui";
    input = {
      main: path.resolve(__dirname, "all/ui/index.html"),
      ui: path.resolve(__dirname, "all/ui/ui/index.html"),
    };
    proxy = {
      "/api": {
        target: `http://${proxyTarget}`,
        changeOrigin: true,
      },
    };
  }

  return {
    plugins: [vue({
      reactivityTransform: path.resolve(__dirname, "all/ui"),
    })],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "all/ui/ui/src"),
        "~": path.resolve(__dirname, "all/share"),
      },
    },
    build: {
      emptyOutDir: true,
      outDir,
      minify: "esbuild",
      rollupOptions: {
        input,
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
    base: ".",
    publicDir: path.resolve(__dirname, "all/share/public"),
    root,
    server: {
      proxy
    },
  };
});
