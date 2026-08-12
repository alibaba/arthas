## 变更概览

- fix: Docker 构建基础镜像切换为 Amazon Corretto 8 Alpine，修复多架构镜像构建兼容性
- feat: 新增 MCP `upload_file` 工具，支持将 `.class`、`.java` 和 `.jfc` 小文件上传到目标 JVM 文件系统，供后续诊断工具使用，见 #3258

## 对比

- https://github.com/alibaba/arthas/compare/arthas-all-4.3.2...arthas-all-4.3.3
