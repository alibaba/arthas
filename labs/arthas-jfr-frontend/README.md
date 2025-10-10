# Arthas JFR Frontend - Java Flight Recorder 分析前端

基于 React 18 + TypeScript + Vite 的现代化 JFR (Java Flight Recorder) 文件分析前端应用，提供直观的火焰图可视化和多维度性能分析功能。

##  功能特性

### 核心功能
- **文件管理**: 完整的 JFR 文件上传、列表展示、删除功能
- **火焰图可视化**: 基于自定义组件的交互式火焰图展示
- **多维度分析**: 支持 17+ 种性能维度的深度分析
- **实时分析**: 快速生成分析结果，支持大文件处理
- **响应式设计**: 支持桌面和移动端访问

### 分析维度
- **CPU 性能**: CPU 时间、CPU 采样、原生执行采样
- **内存分析**: 内存分配次数、分配大小统计
- **I/O 操作**: 文件读写时间、网络读写时间
- **线程分析**: 线程同步、线程等待、线程睡眠时间
- **类加载**: 类加载次数、类加载时间统计
- **时钟分析**: 墙钟时间、CPU 时间对比分析

### 用户界面
- **现代化 UI**: 基于 Ant Design 5.2 的美观界面
- **交互式图表**: 支持缩放、搜索、过滤等操作
- **实时统计**: 显示分析结果的详细统计信息
- **类型安全**: 完整的 TypeScript 类型定义

##  技术栈

### 前端技术
- **React 18.2.0**: UI 框架，使用函数组件和 Hooks
- **TypeScript 5.4.5**: 类型安全，提供完整的类型定义
- **Vite 5.2.8**: 现代化构建工具，快速热更新
- **Ant Design 5.13.7**: UI 组件库，提供丰富的组件
- **React Router 6.22.3**: 客户端路由管理
- **Axios 1.6.0**: HTTP 客户端，用于 API 调用

### 开发工具
- **@vitejs/plugin-react**: Vite React 插件
- **Less 4.3.0**: CSS 预处理器
- **ESBuild**: 快速的代码编译和压缩

### 核心依赖
- `react` + `react-dom` - React 核心库
- `antd` + `@ant-design/icons` - UI 组件库
- `react-router-dom` - 路由管理
- `axios` - HTTP 请求库

##  项目结构

```
arthas-jfr-frontend/
├── src/
│   ├── components/          # React 组件
│   │   ├── FileUpload/      # 文件上传组件
│   │   │   ├── FileUpload.tsx
│   │   │   └── index.tsx
│   │   ├── FileTable/       # 文件列表组件
│   │   │   ├── FileTable.tsx
│   │   │   └── index.tsx
│   │   └── FlameGraph/      # 火焰图组件
│   │       ├── FlameStats.tsx
│   │       └── ReactFlameGraphWrapper.tsx
│   ├── pages/               # 页面组件
│   │   ├── Home/            # 首页
│   │   │   ├── Home.tsx
│   │   │   └── index.tsx
│   │   └── Analysis/        # 分析页面
│   │       ├── Analysis.tsx
│   │       └── index.tsx
│   ├── layouts/             # 布局组件
│   │   └── BasicLayout.tsx
│   ├── services/            # API 服务
│   │   ├── api.ts           # API 基础配置
│   │   ├── fileService.ts   # 文件服务
│   │   ├── jfr.ts           # JFR 相关服务
│   │   └── jfrService.ts    # JFR 分析服务
│   ├── stores/              # 状态管理
│   │   └── FileContext.tsx  # 文件上下文
│   ├── hooks/               # 自定义 Hooks
│   │   └── useWindowSize.ts
│   ├── utils/               # 工具函数
│   │   ├── color.ts         # 颜色工具
│   │   ├── format.ts        # 格式化工具
│   │   └── formatFlamegraph.ts # 火焰图格式化
│   ├── App.tsx              # 根组件
│   ├── main.tsx             # 入口文件
│   └── global.less          # 全局样式
├── public/                  # 静态资源
├── dist/                    # 构建输出目录
├── index.html               # HTML 模板
├── package.json             # 依赖配置
├── vite.config.ts           # Vite 配置
├── tsconfig.json            # TypeScript 配置
└── tsconfig.node.json       # Node.js TypeScript 配置
```

##  快速开始

### 环境要求
- **Node.js 18+**: 推荐使用 Node.js 18 或更高版本
- **npm 9+**: 包管理器
- **现代浏览器**: 支持 ES2020 的现代浏览器

### 1. 安装依赖
```bash
cd arthas/labs/arthas-jfr-frontend
npm install
```

### 2. 配置后端 API
确保后端服务已启动在 `http://localhost:8200`，前端已配置代理：

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8200',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
```

### 3. 启动开发服务器
```bash
npm run dev
```

前端应用将在 `http://localhost:5173` 启动

### 4. 构建生产版本
```bash
# 构建生产版本
npm run build

# 预览构建结果
npm run preview
```

### 5. 验证应用
- 前端应用: `http://localhost:5173`
- 确保后端服务运行在: `http://localhost:8200`
- 检查浏览器控制台是否有错误信息

##  使用指南

### 1. 上传 JFR 文件
- 在首页点击"上传文件"按钮
- 选择 `.jfr` 文件进行上传（支持最大 1GB）
- 系统会自动验证文件格式并显示上传进度

### 2. 文件管理
- 在文件列表中查看已上传的 JFR 文件
- 支持按文件名、上传时间排序
- 可以删除不需要的文件

### 3. 开始分析
- 在文件列表中选择要分析的文件
- 点击"分析"按钮进入分析页面
- 选择分析维度（如 CPU Time、Memory Allocation 等）

### 4. 查看火焰图
- 火焰图会显示方法调用栈和性能热点
- 支持缩放、搜索、过滤等交互操作
- 点击节点查看详细的方法信息
- 支持不同性能维度的切换

##  开发指南

### 开发环境设置
```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 类型检查
npx tsc --noEmit

# 构建生产版本
npm run build

# 预览构建结果
npm run preview
```

### 项目配置

#### Vite 配置
```typescript
// vite.config.ts
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8200'
    }
  },
  build: {
    outDir: 'dist',
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          antd: ['antd', '@ant-design/icons']
        }
      }
    }
  }
})
```

#### TypeScript 配置
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "jsx": "react-jsx",
    "strict": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  }
}
```

### 代码规范
- 使用 TypeScript 严格模式
- 组件使用函数式组件 + Hooks
- 使用 Ant Design 组件库
- 遵循 React 最佳实践
- 使用 ESLint 进行代码检查

### 添加新功能

#### 1. 添加新页面
```typescript
// src/pages/NewPage/NewPage.tsx
import React from 'react';

const NewPage: React.FC = () => {
  return <div>New Page</div>;
};

export default NewPage;
```

#### 2. 添加新组件
```typescript
// src/components/NewComponent/NewComponent.tsx
import React from 'react';
import { Button } from 'antd';

interface NewComponentProps {
  title: string;
  onClick: () => void;
}

const NewComponent: React.FC<NewComponentProps> = ({ title, onClick }) => {
  return <Button onClick={onClick}>{title}</Button>;
};

export default NewComponent;
```

#### 3. 添加 API 服务
```typescript
// src/services/newService.ts
import { api } from './api';

export const newService = {
  getData: () => api.get('/new-endpoint'),
  postData: (data: any) => api.post('/new-endpoint', data)
};
```

##  参考项目

本项目参考了以下优秀的开源项目：

- **[Java Mission Control (JMC)](https://github.com/openjdk/jmc)** - Oracle 官方的 Java 性能监控工具
- **[JProfiler](https://www.ej-technologies.com/products/jprofiler/overview.html)** - 商业 Java 性能分析工具
- **[VisualVM](https://visualvm.github.io/)** - 免费的 Java 性能分析工具
- **[FlameGraph](https://github.com/brendangregg/FlameGraph)** - 火焰图生成工具
- **[Jifa](https://github.com/eclipse-jifa/jifa)** - Java 应用诊断工具

##  相关技术文档

### 前端技术
- [React 官方文档](https://react.dev/)
- [TypeScript 官方文档](https://www.typescriptlang.org/)
- [Vite 官方文档](https://vitejs.dev/)
- [Ant Design 组件库](https://ant.design/)

### 后端技术
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Java Mission Control 文档](https://github.com/openjdk/jmc)
- [JFR 文件格式规范](https://openjdk.org/projects/jdk/8/)




