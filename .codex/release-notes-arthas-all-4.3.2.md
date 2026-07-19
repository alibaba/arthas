## 变更概览

- fix: 修复 `AdviceListenerManager` 的竞态条件，避免增强监听器丢失，见 #3237
- fix: 避免错误跳过名称中包含 Lambda 标记的普通类，见 #3240
- fix: `RateCounter` 尚无采样值时返回 0，避免出现非数值结果，见 #3228
- fix: `ResultUtils.processClassNames` 正确响应分页处理器的停止信号，见 #3224
- fix: `vmtool` referenceAnalyze MCP 工具正确传递 className，见 #3241
- fix: 补全多 ClassLoader 加载同名类时的方法自动补全结果，见 #3218
- feat: Terminal 支持直接输入 Unicode 字符并正确记录历史命令，见 #3242

## 对比

- https://github.com/alibaba/arthas/compare/arthas-all-4.3.1...arthas-all-4.3.2
