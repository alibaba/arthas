![arthas-roadmap](TODO/arthas-roadmap.png)

### 设计原则

* microservices envy: 理想状态下，所有数据通过 /metrics, /health, /config 暴露
* endpoint 可扩展
* endpoint 数据本身就应该可以判定子系统有无问题，而平台只是做展示和聚合。有无平台并不妨碍用户获取数据 (devops)
* 但是平台要成为开发的第一入口，因为其他方式太麻烦了
* 集群数据、单机历史数据作为单机诊断的依据，单纯的展示没有意义
* 场景优先，有助于曝光的场景优先实施
