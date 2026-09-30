# Arthas AI Agent Skills

Arthas 提供了一组可复用的 Skill，帮助 AI 编码 Agent 按规范流程诊断常见的 Java 与 JVM 问题。

[English](README.md)

## 可用 Skill

| Skill | 用途 |
| --- | --- |
| `arthas` | 根据问题描述选择最合适的 Arthas 诊断流程。 |
| `arthas-cpu-high` | 从 JVM 概况、热点线程到热点方法，排查 CPU 飙高。 |
| `arthas-eagleeye-traceid` | 使用有限次数的 `watch` 或 `trace` 获取 EagleEye traceId。 |
| `arthas-springcontext-issues-resolve` | 排查 Spring Context、Bean 和配置项问题。 |

仓库以 `plugins/arthas/skills/` 作为唯一 Skill 源目录。同一份文件既可以作为 Codex Plugin 安装，也兼容跨 Agent 的 `skills` CLI 和 GitHub `gh-skill` 扩展。

## 方式一：Codex Plugin

这种方式会一次安装全部四个 Skill，并在 Codex 的插件目录中展示该插件。

先用稀疏检出添加仓库 Marketplace，再安装插件：

```bash
codex plugin marketplace add alibaba/arthas \
  --sparse .agents/plugins \
  --sparse plugins/arthas
codex plugin add arthas@arthas-plugins
```

检查安装结果：

```bash
codex plugin list --json
```

也可以在 ChatGPT 桌面端打开插件目录，选择 **Arthas Plugins**，再安装 **Arthas Diagnostics**。

更多信息请参考官方的 [Plugin 打包指南](https://developers.openai.com/plugins/build/plugins)和 [Codex Plugin 命令](https://learn.chatgpt.com/docs/developer-commands#codex-plugin)。

## 方式二：`skills` CLI

需要在 Codex、Claude Code、Cursor 或其他兼容 Agent 之间复用 Skill 时，可以选择这种方式。

只查看仓库中的 Skill，不执行安装：

```bash
npx skills add alibaba/arthas --list
```

为当前项目的 Codex 安装全部 Arthas Skill：

```bash
npx skills add alibaba/arthas --agent codex --skill '*' --yes
```

追加 `--global` 可以让它们在 Codex 的所有项目中生效。只安装一个诊断流程时，把 `'*'` 替换成对应 Skill 名称，例如 `arthas-cpu-high`。

更新项目级安装：

```bash
npx skills update --project --yes
```

## 方式三：GitHub `gh-skill`

希望使用 GitHub 原生的发现、预览、版本锁定和更新能力时，可以选择这种方式。

交互式浏览可用 Skill：

```bash
gh skill install alibaba/arthas
```

直接预览并安装一个诊断流程：

```bash
gh skill preview alibaba/arthas arthas-cpu-high
gh skill install alibaba/arthas arthas-cpu-high \
  --agent codex \
  --scope user
```

先只读检查更新，再应用全部可用更新：

```bash
gh skill update --dry-run
gh skill update --all
```
