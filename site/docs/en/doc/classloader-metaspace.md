# classloader-metaspace

::: tip
Show metaspace / class metadata statistics by ClassLoader instance.
:::

`classloader-metaspace` uses the JFR `jdk.ClassLoaderStatistics` event to collect metaspace statistics for each ClassLoaderData, then maps those rows back to Arthas ClassLoader hash, type and display name.

> Note: this command reports metaspace / class metadata memory. It does not report the retained size of all Java heap objects reachable from a ClassLoader.

`classloader-metaspace` depends on JFR. If the current JDK does not support JFR, the command is not registered.

## Options

|                  Name | Specification                                                                                                   |
| --------------------: | :-------------------------------------------------------------------------------------------------------------- |
|                `[c:]` | ClassLoader hashcode, same format as `classloader -c`                                                           |
| `[classLoaderClass:]` | Filter by full ClassLoader class name                                                                           |
|         `[duration:]` | JFR sampling duration, `2500ms` by default; supports `ms`, `s`, `m`; a plain number means milliseconds          |
|           `[period:]` | `jdk.ClassLoaderStatistics` period, `500ms` by default; supports `ms`, `s`, `m`                                 |
|            `[limit:]` | Show only the first N rows after sorting by `chunkSize` descending                                              |
|          `[verbose:]` | Show full diagnostic columns, including `classLoaderData`, `hiddenBlockSize` and `type`; `-v` is also supported |

## Output

The default terminal table prioritizes core columns for daily troubleshooting. With `--verbose`, full diagnostic columns are shown. Long text columns, `type` and `name`, are shown on the right and may be truncated or hidden when the terminal is narrow.

|       Field | Specification                                                                   |
| ----------: | :------------------------------------------------------------------------------ |
|      `hash` | Arthas ClassLoader hash                                                         |
|   `classes` | JFR `classCount`, the number of loaded classes                                  |
| `chunkSize` | Total allocated metaspace chunk size for the ClassLoaderData                    |
| `blockSize` | Total used metaspace block size                                                 |
|      `name` | Display name. The command tries JFR name first, then falls back to `toString()` |

`--verbose` also shows:

|             Field | Specification                                                                                                     |
| ----------------: | :---------------------------------------------------------------------------------------------------------------- |
| `classLoaderData` | HotSpot ClassLoaderData pointer                                                                                   |
| `hiddenBlockSize` | Total metaspace block size used by hidden classes; on JDK 11 this is read from the JFR `anonymousBlockSize` field |
|            `type` | ClassLoader class name                                                                                            |

## Usage

### Show all ClassLoader metaspace statistics

```bash
$ classloader-metaspace
 hash      classes  chunkSize  blockSize  name
 68b31f0a  2115     1048576    823296     com.taobao.arthas.agent.ArthasClassloader@68b31f0a
 null      1861     524288     410624     BootstrapClassLoader
Affect(row-cnt:2) cost in 2510 ms.
```

### Filter by ClassLoader type

```bash
$ classloader-metaspace --classLoaderClass demo.TestApp$ModuleClassLoader
 hash      classes  chunkSize  blockSize  name
 6d06d69c  1        6144       1744       order-service's ModuleClassLoader
 7852e922  1        4096       1744       pay-service's ModuleClassLoader
 4e25154f  1        7168       1752       user-service's ModuleClassLoader
```

### Show full diagnostic columns

```bash
$ classloader-metaspace --classLoaderClass demo.TestApp$ModuleClassLoader --verbose
 hash      classLoaderData     classes  chunkSize  blockSize  hiddenBlockSize  type                            name
 6d06d69c  0x000000014e135010  1        6144       1744       0                demo.TestApp$ModuleClassLoader  order-service's ModuleClassLoader
 7852e922  0x000000013c605640  1        4096       1744       0                demo.TestApp$ModuleClassLoader  pay-service's ModuleClassLoader
 4e25154f  0x000000014c717830  1        7168       1752       0                demo.TestApp$ModuleClassLoader  user-service's ModuleClassLoader
```

### Limit output rows

```bash
$ classloader-metaspace --limit 20
```

Rows are sorted by `chunkSize desc, blockSize desc, name asc`, so `--limit` keeps ClassLoaders with larger metaspace chunk allocation.
