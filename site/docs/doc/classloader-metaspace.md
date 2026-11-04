# classloader-metaspace

::: tip
按 ClassLoader 实例统计 metaspace / class metadata 相关内存。
:::

`classloader-metaspace` 使用 JFR 的 `jdk.ClassLoaderStatistics` 事件采集每个 ClassLoaderData 的 metaspace 统计信息，并把这些统计信息映射回 Arthas 可识别的 ClassLoader hash、类型和显示名。

> 注意：这个命令统计的是 metaspace / class metadata 相关内存，不是从 ClassLoader 可达的所有 Java heap 对象的 retained size。

`classloader-metaspace` 依赖 JFR。JDK 不支持 JFR 时不会注册该命令。

## 参数说明

|              参数名称 | 参数说明                                                                           |
| --------------------: | :--------------------------------------------------------------------------------- |
|                `[c:]` | ClassLoader 的 hashcode，格式和 `classloader -c` 一致                              |
| `[classLoaderClass:]` | 按 ClassLoader 完整类名过滤                                                        |
|         `[duration:]` | JFR 采样时长，默认 `2500ms`，支持 `ms`、`s`、`m`，裸数字按毫秒处理                 |
|           `[period:]` | `jdk.ClassLoaderStatistics` 采样周期，默认 `500ms`，支持 `ms`、`s`、`m`            |
|            `[limit:]` | 只输出按 `chunkSize` 降序排序后的前 N 行                                           |
|          `[verbose:]` | 输出完整诊断列，包括 `classLoaderData`、`hiddenBlockSize` 和 `type`；也可使用 `-v` |

## 输出字段

默认终端表格优先展示日常排查需要的核心列。使用 `--verbose` 时会输出完整诊断列，`type` 和 `name` 这类长文本列放在右侧；如果终端宽度不足，长文本列可能被截断或隐藏。

|        字段 | 说明                                                 |
| ----------: | :--------------------------------------------------- |
|      `hash` | Arthas ClassLoader hash                              |
|   `classes` | JFR `classCount`，已加载类数量                       |
| `chunkSize` | 该 ClassLoaderData 已分配的 metaspace chunk 总大小   |
| `blockSize` | 已使用的 metaspace block 总大小                      |
|      `name` | 显示名，优先使用 JFR name，失败时回退到 `toString()` |

`--verbose` 额外输出：

|              字段 | 说明                                                                                         |
| ----------------: | :------------------------------------------------------------------------------------------- |
| `classLoaderData` | HotSpot 内部 ClassLoaderData 指针                                                            |
| `hiddenBlockSize` | hidden class 使用的 metaspace block 总大小；在 JDK 11 上兼容读取 JFR 的 `anonymousBlockSize` |
|            `type` | ClassLoader 类名                                                                             |

## 使用参考

### 查看所有 ClassLoader metaspace 统计

```bash
$ classloader-metaspace
 hash      classes  chunkSize  blockSize  name
 68b31f0a  2115     1048576    823296     com.taobao.arthas.agent.ArthasClassloader@68b31f0a
 null      1861     524288     410624     BootstrapClassLoader
Affect(row-cnt:2) cost in 2510 ms.
```

### 按 ClassLoader 类型过滤

```bash
$ classloader-metaspace --classLoaderClass demo.TestApp$ModuleClassLoader
 hash      classes  chunkSize  blockSize  name
 6d06d69c  1        6144       1744       order-service's ModuleClassLoader
 7852e922  1        4096       1744       pay-service's ModuleClassLoader
 4e25154f  1        7168       1752       user-service's ModuleClassLoader
```

### 输出完整诊断列

```bash
$ classloader-metaspace --classLoaderClass demo.TestApp$ModuleClassLoader --verbose
 hash      classLoaderData     classes  chunkSize  blockSize  hiddenBlockSize  type                            name
 6d06d69c  0x000000014e135010  1        6144       1744       0                demo.TestApp$ModuleClassLoader  order-service's ModuleClassLoader
 7852e922  0x000000013c605640  1        4096       1744       0                demo.TestApp$ModuleClassLoader  pay-service's ModuleClassLoader
 4e25154f  0x000000014c717830  1        7168       1752       0                demo.TestApp$ModuleClassLoader  user-service's ModuleClassLoader
```

### 限制输出行数

```bash
$ classloader-metaspace --limit 20
```

默认按 `chunkSize desc, blockSize desc, name asc` 排序，因此 `--limit` 会保留 metaspace chunk 分配较大的 ClassLoader。
