<h1>Arthas 文档网站<sub>(power by vuepress)</sub></h1>

## 项目运行

```shell
# 安装依赖
yarn install

# 启动项目
yarn docs:dev

# 发布项目（打包到 docs/.vuepress/dist 目录）
yarn docs:build
```

> `package.json` 中的 `resolutions` 把 vuepress 依赖的 vite 固定到 5.4.21，用于修复
> CVE-2025-30208 / CVE-2025-31125 / CVE-2025-31486 等 dev server 文件读取问题
> （vuepress 2.0.0-beta.51 依赖的 vite 3.x 没有对应修复版本）。
> 等 vuepress 升级到自身依赖安全版 vite 之后，可以移除该配置。
