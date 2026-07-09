## 变更概览

- fix: 使用 writer-idle websocket ping，避免空闲检测写事件干扰，见 #3225
- fix: 设置 agent 绑定线程的优先级和 daemon 属性，见 #1972
- fix: 修复 `vmtool` backtrace limit 参数校验，见 #3226
- chore: 删除未使用的 `GaStack` collection 包，见 #3229

## 对比

- https://github.com/alibaba/arthas/compare/arthas-all-4.3.0...arthas-all-4.3.1
