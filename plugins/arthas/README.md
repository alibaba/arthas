# Arthas AI Agent Skills

Arthas provides reusable skills that guide AI coding agents through common Java and JVM diagnostic workflows.

[中文说明](README_CN.md)

## Available skills

| Skill | Purpose |
| --- | --- |
| `arthas` | Routes a diagnostic request to the most relevant Arthas workflow. |
| `arthas-cpu-high` | Investigates high CPU usage from JVM state to hot threads and methods. |
| `arthas-eagleeye-traceid` | Captures an EagleEye trace ID with bounded `watch` or `trace` commands. |
| `arthas-springcontext-issues-resolve` | Investigates Spring contexts, beans, and configuration values. |

The repository uses one canonical skill tree at `plugins/arthas/skills/`. The same files can be installed as a Codex plugin, with the cross-agent `skills` CLI, or with the GitHub `gh-skill` extension.

## Option 1: Codex plugin

This option installs all four skills together and makes the plugin available in Codex's plugin directory.

Add the repository marketplace with a sparse checkout, then install the plugin:

```bash
codex plugin marketplace add alibaba/arthas \
  --sparse .agents/plugins \
  --sparse plugins/arthas
codex plugin add arthas@arthas-plugins
```

Verify the installation:

```bash
codex plugin list --json
```

You can also open the Plugins Directory in the ChatGPT desktop app, choose **Arthas Plugins**, and install **Arthas Diagnostics**.

See the official [plugin packaging guide](https://developers.openai.com/plugins/build/plugins) and [Codex plugin commands](https://learn.chatgpt.com/docs/developer-commands#codex-plugin) for more details.

## Option 2: `skills` CLI

Use this option when you want the same skills across Codex, Claude Code, Cursor, or another supported agent.

List the skills without installing them:

```bash
npx skills add alibaba/arthas --list
```

Install every Arthas skill for Codex in the current project:

```bash
npx skills add alibaba/arthas --agent codex --skill '*' --yes
```

Add `--global` to make them available to Codex in every project. To install only one workflow, replace `'*'` with its skill name, for example `arthas-cpu-high`.

Update project-scoped installations with:

```bash
npx skills update --project --yes
```

## Option 3: GitHub `gh-skill`

Use this option when you prefer GitHub-native discovery, preview, version pinning, and updates.

Browse the available skills interactively:

```bash
gh skill install alibaba/arthas
```

Preview and install one workflow directly:

```bash
gh skill preview alibaba/arthas arthas-cpu-high
gh skill install alibaba/arthas arthas-cpu-high \
  --agent codex \
  --scope user
```

Check for updates without changing local files, then apply all available updates:

```bash
gh skill update --dry-run
gh skill update --all
```
