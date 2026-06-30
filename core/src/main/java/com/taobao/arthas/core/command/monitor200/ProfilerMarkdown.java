package com.taobao.arthas.core.command.monitor200;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 async-profiler 的 collapsed stacktraces 文本，转换为更适合 LLM 读取与检索的 Markdown 报告。
 * <p>
 * 设计目标：
 * 1) 终端可直接输出，便于复制粘贴给 LLM
 * 2) Token 效率高：关键信息结构化、去噪、可 grep
 * 3) 不引入额外依赖，保持实现简单可维护
 */
final class ProfilerMarkdown {

    private ProfilerMarkdown() {
    }

    static class Options {
        private String action;
        private String event;
        private boolean threads;
        private int topN = 10;
        private String collapsed;

        Options action(String action) {
            this.action = action;
            return this;
        }

        Options event(String event) {
            this.event = event;
            return this;
        }

        Options threads(boolean threads) {
            this.threads = threads;
            return this;
        }

        Options topN(int topN) {
            if (topN > 0) {
                this.topN = topN;
            }
            return this;
        }

        Options collapsed(String collapsed) {
            this.collapsed = collapsed;
            return this;
        }
    }

    static String toMarkdown(Options options) {
        String collapsed = options == null ? null : options.collapsed;
        if (collapsed == null) {
            collapsed = "";
        }

        CollapsedProfile profile = parseCollapsed(collapsed, options != null && options.threads);
        List<Entry> hotspots = topHotspots(profile.selfSamples, options == null ? 10 : options.topN);
        List<StackSample> topStacks = topStacks(profile.stacks, options == null ? 10 : options.topN);

        StringBuilder sb = new StringBuilder(8 * 1024);
        sb.append("# Arthas profiler report (Markdown)\n\n");
        sb.append("- action: ").append(nullToDash(options == null ? null : options.action)).append("\n");
        sb.append("- event: ").append(nullToDash(options == null ? null : options.event)).append("\n");
        sb.append("- threads: ").append(options != null && options.threads).append("\n");
        sb.append("- total samples: ").append(profile.totalSamples).append("\n\n");

        sb.append("## Top ").append(options == null ? 10 : options.topN).append(" hotspots (self)\n\n");
        sb.append("| rank | function | self_samples | self_percent |\n");
        sb.append("| ---: | --- | ---: | ---: |\n");
        int rank = 1;
        for (Entry e : hotspots) {
            sb.append("| ").append(rank++).append(" | ")
                    .append(escapePipes(e.key)).append(" | ")
                    .append(e.value).append(" | ")
                    .append(formatPercent(e.value, profile.totalSamples)).append(" |\n");
        }
        if (hotspots.isEmpty()) {
            sb.append("| - | - | 0 | 0.00% |\n");
        }
        sb.append("\n");

        sb.append("## Top ").append(options == null ? 10 : options.topN).append(" stacks\n\n");
        sb.append("| rank | samples | percent | stack |\n");
        sb.append("| ---: | ---: | ---: | --- |\n");
        rank = 1;
        for (StackSample s : topStacks) {
            sb.append("| ").append(rank++).append(" | ")
                    .append(s.samples).append(" | ")
                    .append(formatPercent(s.samples, profile.totalSamples)).append(" | ")
                    .append(escapePipes(s.stack)).append(" |\n");
        }
        if (topStacks.isEmpty()) {
            sb.append("| - | 0 | 0.00% | - |\n");
        }
        sb.append("\n");

        sb.append("## Call tree (top)\n\n");
        sb.append("```text\n");
        sb.append(renderCallTree(profile.callTreeRoot, profile.totalSamples, options == null ? 10 : options.topN));
        sb.append("\n```\n\n");

        sb.append("## Function details\n\n");
        int detailsFunctions = Math.min(hotspots.size(), options == null ? 10 : options.topN);
        for (int i = 0; i < detailsFunctions; i++) {
            Entry e = hotspots.get(i);
            sb.append("### ").append(e.key).append("\n\n");
            sb.append("- self: ").append(e.value)
                    .append(" (").append(formatPercent(e.value, profile.totalSamples)).append(")\n");

            List<StackSample> stacksForFunction = profile.topStacksByLeaf.get(e.key);
            if (stacksForFunction != null && !stacksForFunction.isEmpty()) {
                sb.append("- top stacks:\n");
                for (StackSample s : stacksForFunction) {
                    sb.append("  - ").append(s.samples)
                            .append(" (").append(formatPercent(s.samples, profile.totalSamples)).append(") ")
                            .append(s.stack).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("## Notes\n\n");
        sb.append("- 该报告由 Arthas 基于 async-profiler 的 `collapsed` 输出生成，适合直接复制给 LLM 分析。\n");
        sb.append("- `hotspots (self)` 以栈顶帧为准（近似 self time），有助于快速定位热点函数。\n");
        sb.append("- `stacks` 用于观察最常出现的调用路径；如需火焰图可继续使用 `--format flamegraph`.\n");

        return sb.toString();
    }

    private static String nullToDash(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "-";
        }
        return s.trim();
    }

    private static String formatPercent(long part, long total) {
        if (total <= 0) {
            return "0.00%";
        }
        double p = (double) part * 100.0d / (double) total;
        return String.format(java.util.Locale.ROOT, "%.2f%%", p);
    }

    private static String escapePipes(String s) {
        if (s == null) {
            return "-";
        }
        // Markdown 表格中需要转义 '|'
        return s.replace("|", "\\|");
    }

    private static class CollapsedProfile {
        private long totalSamples;
        private final Map<String, Long> selfSamples = new LinkedHashMap<>();
        private final List<StackSample> stacks = new ArrayList<>();
        private final Map<String, List<StackSample>> topStacksByLeaf = new LinkedHashMap<>();
        private final Node callTreeRoot = new Node();
    }

    private static class StackSample {
        private final String stack;
        private final long samples;

        private StackSample(String stack, long samples) {
            this.stack = stack;
            this.samples = samples;
        }
    }

    private static class Entry {
        private final String key;
        private final long value;

        private Entry(String key, long value) {
            this.key = key;
            this.value = value;
        }
    }

    private static class Node {
        private long samples;
        private final Map<String, Node> children = new LinkedHashMap<>();
    }

    private static CollapsedProfile parseCollapsed(String collapsed, boolean threadsEnabled) {
        CollapsedProfile profile = new CollapsedProfile();

        String[] lines = collapsed.split("\\r?\\n");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // collapsed 格式：frame1;frame2;frame3 <samples>
            int spaceIdx = lastSpaceIndex(line);
            if (spaceIdx < 0) {
                continue;
            }
            String stack = line.substring(0, spaceIdx).trim();
            String countStr = line.substring(spaceIdx + 1).trim();
            if (stack.isEmpty() || countStr.isEmpty()) {
                continue;
            }

            long samples;
            try {
                samples = Long.parseLong(countStr);
            } catch (Throwable ignore) {
                continue;
            }
            if (samples <= 0) {
                continue;
            }

            // threads 模式下，栈最后可能会附带线程名帧；为了减少噪音，这里尽量去掉它。
            String normalizedStack = stripThreadFrame(stack, threadsEnabled);
            profile.totalSamples += samples;
            profile.stacks.add(new StackSample(normalizedStack, samples));

            addToCallTree(profile.callTreeRoot, normalizedStack, samples);

            String topFrame = topFrame(normalizedStack, threadsEnabled);
            if (topFrame == null || topFrame.isEmpty()) {
                continue;
            }
            Long old = profile.selfSamples.get(topFrame);
            profile.selfSamples.put(topFrame, old == null ? samples : old + samples);

            addTopStacksByLeaf(profile.topStacksByLeaf, topFrame, normalizedStack, samples, 3);
        }

        return profile;
    }

    private static int lastSpaceIndex(String s) {
        // 兼容 stack 中包含空格（线程名）时，取最后一个空格作为 count 分隔
        for (int i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) == ' ') {
                return i;
            }
        }
        return -1;
    }

    private static String topFrame(String stack, boolean threadsEnabled) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        int lastSep = stack.lastIndexOf(';');
        String top = lastSep >= 0 ? stack.substring(lastSep + 1) : stack;
        top = top.trim();

        // threads 模式下，async-profiler 会以“线程帧”结束；这类帧对定位热点帮助有限，尽量跳过：
        // 规则：如果 top 看起来是 [thread] 或包含 "(thread)" 等，这里仅做轻量过滤，避免误删正常 frame。
        if (threadsEnabled && looksLikeThreadFrame(top)) {
            // 回退到倒数第二帧
            int prevSep = lastSep >= 0 ? stack.lastIndexOf(';', lastSep - 1) : -1;
            String prev = prevSep >= 0 ? stack.substring(prevSep + 1, lastSep) : "";
            return prev.trim();
        }
        return top;
    }

    private static String stripThreadFrame(String stack, boolean threadsEnabled) {
        if (!threadsEnabled || stack == null || stack.isEmpty()) {
            return stack;
        }
        int lastSep = stack.lastIndexOf(';');
        if (lastSep < 0) {
            return stack;
        }
        String last = stack.substring(lastSep + 1).trim();
        // async-profiler threads 模式下常见线程帧形如：[tid=1234] "thread-name"
        if (last.startsWith("[") && last.contains("tid=")) {
            return stack.substring(0, lastSep);
        }
        return stack;
    }

    private static void addToCallTree(Node root, String stack, long samples) {
        if (root == null || stack == null || stack.isEmpty() || samples <= 0) {
            return;
        }
        root.samples += samples;
        String[] frames = stack.split(";");
        Node current = root;
        for (String frame : frames) {
            String f = frame == null ? "" : frame.trim();
            if (f.isEmpty()) {
                continue;
            }
            Node child = current.children.get(f);
            if (child == null) {
                child = new Node();
                current.children.put(f, child);
            }
            child.samples += samples;
            current = child;
        }
    }

    private static String renderCallTree(Node root, long totalSamples, int topN) {
        if (root == null || root.children.isEmpty() || totalSamples <= 0) {
            return "-";
        }
        int maxDepth = 8;
        int maxChildren = 5;
        int maxRoots = Math.min(topN, 10);
        StringBuilder sb = new StringBuilder(4096);
        List<Map.Entry<String, Node>> roots = sortChildren(root);
        for (int i = 0; i < roots.size() && i < maxRoots; i++) {
            Map.Entry<String, Node> e = roots.get(i);
            renderCallTreeNode(sb, e.getKey(), e.getValue(), totalSamples, 0, maxDepth, maxChildren);
        }
        return sb.toString().trim();
    }

    private static void renderCallTreeNode(StringBuilder sb, String label, Node node, long totalSamples,
                                           int depth, int maxDepth, int maxChildren) {
        if (node == null || label == null) {
            return;
        }
        if (depth >= maxDepth) {
            return;
        }
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        sb.append("(").append(formatPercent(node.samples, totalSamples)).append(") ")
                .append(node.samples).append(" ")
                .append(label).append("\n");

        if (node.children.isEmpty()) {
            return;
        }
        List<Map.Entry<String, Node>> children = sortChildren(node);
        for (int i = 0; i < children.size() && i < maxChildren; i++) {
            Map.Entry<String, Node> child = children.get(i);
            renderCallTreeNode(sb, child.getKey(), child.getValue(), totalSamples, depth + 1, maxDepth, maxChildren);
        }
    }

    private static List<Map.Entry<String, Node>> sortChildren(Node node) {
        if (node == null || node.children.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map.Entry<String, Node>> list = new ArrayList<>(node.children.entrySet());
        list.sort(new Comparator<Map.Entry<String, Node>>() {
            @Override
            public int compare(Map.Entry<String, Node> a, Map.Entry<String, Node> b) {
                return Long.compare(b.getValue().samples, a.getValue().samples);
            }
        });
        return list;
    }

    private static void addTopStacksByLeaf(Map<String, List<StackSample>> topStacksByLeaf,
                                          String leaf, String stack, long samples, int limit) {
        if (topStacksByLeaf == null || leaf == null || leaf.isEmpty() || stack == null || stack.isEmpty()) {
            return;
        }
        List<StackSample> list = topStacksByLeaf.get(leaf);
        if (list == null) {
            list = new ArrayList<>();
            topStacksByLeaf.put(leaf, list);
        }
        list.add(new StackSample(stack, samples));
        list.sort(new Comparator<StackSample>() {
            @Override
            public int compare(StackSample a, StackSample b) {
                return Long.compare(b.samples, a.samples);
            }
        });
        if (list.size() > limit) {
            list.subList(limit, list.size()).clear();
        }
    }

    private static boolean looksLikeThreadFrame(String frame) {
        if (frame == null) {
            return false;
        }
        String f = frame.trim();
        if (f.isEmpty()) {
            return false;
        }
        // async-profiler 线程帧通常形如：[tid=1234] "thread-name"
        if (f.startsWith("[") && f.contains("tid=")) {
            return true;
        }
        if (f.startsWith("java.lang.Thread.run") || f.startsWith("java.base/java.lang.Thread.run")) {
            return true;
        }
        return false;
    }

    private static List<Entry> topHotspots(Map<String, Long> selfSamples, int topN) {
        if (selfSamples == null || selfSamples.isEmpty()) {
            return Collections.emptyList();
        }
        List<Entry> list = new ArrayList<>(selfSamples.size());
        for (Map.Entry<String, Long> e : selfSamples.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                list.add(new Entry(e.getKey(), e.getValue()));
            }
        }
        list.sort(new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                return Long.compare(b.value, a.value);
            }
        });
        if (topN <= 0 || topN >= list.size()) {
            return list;
        }
        return new ArrayList<>(list.subList(0, topN));
    }

    private static List<StackSample> topStacks(List<StackSample> stacks, int topN) {
        if (stacks == null || stacks.isEmpty()) {
            return Collections.emptyList();
        }
        List<StackSample> list = new ArrayList<>(stacks);
        list.sort(new Comparator<StackSample>() {
            @Override
            public int compare(StackSample a, StackSample b) {
                return Long.compare(b.samples, a.samples);
            }
        });
        if (topN <= 0 || topN >= list.size()) {
            return list;
        }
        return new ArrayList<>(list.subList(0, topN));
    }
}
