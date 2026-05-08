# classloader-metaspace

::: tip
Show metaspace / class metadata statistics by ClassLoader instance.
:::

`classloader-metaspace` uses the JFR `jdk.ClassLoaderStatistics` event to collect metaspace statistics for each ClassLoaderData, then maps those rows back to Arthas ClassLoader hash, type and display name.

> Note: this command reports metaspace / class metadata memory. It does not report the retained size of all Java heap objects reachable from a ClassLoader.

`classloader-metaspace` depends on JFR. If the current JDK does not support JFR, the command is not registered.

## Options

| Name | Specification |
| ---: | :--- |
| `[c:]` | ClassLoader hashcode, same format as `classloader -c` |
| `[classLoaderClass:]` | Filter by full ClassLoader class name |
| `[field:]` | ClassLoader field used as display name, `moduleName` by default |
| `[duration:]` | JFR sampling duration, `2500ms` by default; supports `ms`, `s`, `m`; a plain number means milliseconds |
| `[period:]` | `jdk.ClassLoaderStatistics` period, `500ms` by default; supports `ms`, `s`, `m` |
| `[limit:]` | Show only the first N rows after sorting by `chunkSize` descending |

## Output

The terminal table prioritizes metaspace statistic columns. Long text columns, `type` and `name`, are shown on the right and may be truncated or hidden when the terminal is narrow. Increase the Arthas client width to inspect full text.

| Field | Specification |
| ---: | :--- |
| `hash` | Arthas ClassLoader hash |
| `classLoaderData` | HotSpot ClassLoaderData pointer |
| `classes` | JFR `classCount`, the number of loaded classes |
| `chunkSize` | Total allocated metaspace chunk size for the ClassLoaderData |
| `blockSize` | Total used metaspace block size |
| `hiddenBlockSize` | Total metaspace block size used by hidden classes |
| `type` | ClassLoader class name |
| `name` | Display name. The command tries `--field` first, then falls back to JFR name / `toString()` |

## Usage

### Show all ClassLoader metaspace statistics

```bash
$ classloader-metaspace
 hash      classLoaderData     classes  chunkSize  blockSize  hiddenBlockSize  type                                      name
 68b31f0a  0x000000012ee25f50  2115     1048576    823296     0                com.taobao.arthas.agent.ArthasClassloader  com.taobao.arthas.agent.ArthasClassloader@68b31f0a
 null      0x000000012e000000  1861     524288     410624     0                BootstrapClassLoader                      BootstrapClassLoader
Affect(row-cnt:2) cost in 2510 ms.
```

### Filter by ClassLoader type

```bash
$ classloader-metaspace --classLoaderClass demo.TestApp$ModuleClassLoader --field moduleName
 hash      classLoaderData     classes  chunkSize  blockSize  hiddenBlockSize  type                            name
 6d06d69c  0x000000014e135010  1        6144       1744       0                demo.TestApp$ModuleClassLoader  order-service
 7852e922  0x000000013c605640  1        4096       1744       0                demo.TestApp$ModuleClassLoader  pay-service
 4e25154f  0x000000014c717830  1        7168       1752       0                demo.TestApp$ModuleClassLoader  user-service
```

### Limit output rows

```bash
$ classloader-metaspace --limit 20
```

Rows are sorted by `chunkSize desc, blockSize desc, name asc`, so `--limit` keeps ClassLoaders with larger metaspace chunk allocation.
