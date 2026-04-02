package com.taobao.arthas.core.command.monitor200;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Profiler性能分析结果Markdown格式转换器
 * <p>
 * 将 async-profiler 的 collapsed stacktraces 文本，转换为更适合 LLM 读取与检索的 Markdown 报告。
 * <p>
 * 设计目标：
 * 1) 终端可直接输出，便于复制粘贴给 LLM
 * 2) Token 效率高：关键信息结构化、去噪、可 grep
 * 3) 不引入额外依赖，保持实现简单可维护
 * <p>
 * Collapsed格式示例：
 * <pre>
 * frame1;frame2;frame3 42
 * frame1;frame2;frame4 10
 * </pre>
 * 每行表示一个调用栈，帧之间用分号分隔，最后是采样次数
 */
final class ProfilerMarkdown {

    /**
     * 私有构造函数，防止实例化
     * 这是一个工具类，所有方法都是静态的
     */
    private ProfilerMarkdown() {
    }

    /**
     * Markdown转换选项配置类
     * 使用建造者模式，支持链式调用
     */
    static class Options {
        /** 性能分析动作类型（如：profile、collect等） */
        private String action;
        /** 性能分析事件类型（如：cpu、alloc、lock等） */
        private String event;
        /** 是否启用了线程级别的性能分析 */
        private boolean threads;
        /** 显示前N个热点方法，默认10 */
        private int topN = 10;
        /** collapsed格式的原始性能分析数据 */
        private String collapsed;

        /**
         * 设置动作类型
         * @param action 动作名称
         * @return 当前Options对象，支持链式调用
         */
        Options action(String action) {
            this.action = action;
            return this;
        }

        /**
         * 设置事件类型
         * @param event 事件名称
         * @return 当前Options对象，支持链式调用
         */
        Options event(String event) {
            this.event = event;
            return this;
        }

        /**
         * 设置是否启用线程模式
         * @param threads true表示启用线程模式
         * @return 当前Options对象，支持链式调用
         */
        Options threads(boolean threads) {
            this.threads = threads;
            return this;
        }

        /**
         * 设置显示的热点方法数量
         * @param topN 要显示的前N个热点方法，必须大于0
         * @return 当前Options对象，支持链式调用
         */
        Options topN(int topN) {
            if (topN > 0) {
                this.topN = topN;
            }
            return this;
        }

        /**
         * 设置collapsed格式的原始数据
         * @param collapsed collapsed格式的字符串
         * @return 当前Options对象，支持链式调用
         */
        Options collapsed(String collapsed) {
            this.collapsed = collapsed;
            return this;
        }
    }

    /**
     * 将性能分析结果转换为Markdown格式的报告
     *
     * @param options 转换选项，包含collapsed数据和配置参数
     * @return 格式化的Markdown报告字符串
     */
    static String toMarkdown(Options options) {
        // 获取collapsed格式的原始数据，如果为null则使用空字符串
        String collapsed = options == null ? null : options.collapsed;
        if (collapsed == null) {
            collapsed = "";
        }

        // 解析collapsed格式的数据，提取性能分析指标
        CollapsedProfile profile = parseCollapsed(collapsed, options != null && options.threads);
        // 获取前N个热点方法（按自身采样次数排序）
        List<Entry> hotspots = topHotspots(profile.selfSamples, options == null ? 10 : options.topN);
        // 获取前N个调用栈（按采样次数排序）
        List<StackSample> topStacks = topStacks(profile.stacks, options == null ? 10 : options.topN);

        // 构建Markdown报告
        StringBuilder sb = new StringBuilder(8 * 1024);
        // 报告标题和基本信息
        sb.append("# Arthas profiler report (Markdown)\n\n");
        sb.append("- action: ").append(nullToDash(options == null ? null : options.action)).append("\n");
        sb.append("- event: ").append(nullToDash(options == null ? null : options.event)).append("\n");
        sb.append("- threads: ").append(options != null && options.threads).append("\n");
        sb.append("- total samples: ").append(profile.totalSamples).append("\n\n");

        // 生成热点方法表格
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

        // 生成调用栈表格
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

        // 生成调用树
        sb.append("## Call tree (top)\n\n");
        sb.append("```text\n");
        sb.append(renderCallTree(profile.callTreeRoot, profile.totalSamples, options == null ? 10 : options.topN));
        sb.append("\n```\n\n");

        // 生成函数详细信息
        sb.append("## Function details\n\n");
        int detailsFunctions = Math.min(hotspots.size(), options == null ? 10 : options.topN);
        for (int i = 0; i < detailsFunctions; i++) {
            Entry e = hotspots.get(i);
            sb.append("### ").append(e.key).append("\n\n");
            sb.append("- self: ").append(e.value)
                    .append(" (").append(formatPercent(e.value, profile.totalSamples)).append(")\n");

            // 获取该函数的top调用栈
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

        // 添加使用说明
        sb.append("## Notes\n\n");
        sb.append("- 该报告由 Arthas 基于 async-profiler 的 `collapsed` 输出生成，适合直接复制给 LLM 分析。\n");
        sb.append("- `hotspots (self)` 以栈顶帧为准（近似 self time），有助于快速定位热点函数。\n");
        sb.append("- `stacks` 用于观察最常出现的调用路径；如需火焰图可继续使用 `--format flamegraph`.\n");

        return sb.toString();
    }

    /**
     * 将null或空字符串转换为破折号
     * 用于在报告中显示空值
     *
     * @param s 输入字符串
     * @return 如果输入为null或空字符串返回"-"，否则返回去除首尾空格的字符串
     */
    private static String nullToDash(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "-";
        }
        return s.trim();
    }

    /**
     * 格式化百分比显示
     *
     * @param part 部分值
     * @param total 总值
     * @return 格式化后的百分比字符串，保留两位小数
     */
    private static String formatPercent(long part, long total) {
        if (total <= 0) {
            return "0.00%";
        }
        // 计算百分比：部分值 * 100 / 总值
        double p = (double) part * 100.0d / (double) total;
        // 使用ROOT locale确保格式一致性
        return String.format(java.util.Locale.ROOT, "%.2f%%", p);
    }

    /**
     * 转义Markdown表格中的管道符
     * Markdown表格使用|作为列分隔符，因此需要转义内容中的|
     *
     * @param s 输入字符串
     * @return 转义后的字符串，null返回"-"
     */
    private static String escapePipes(String s) {
        if (s == null) {
            return "-";
        }
        // Markdown 表格中需要转义 '|'
        return s.replace("|", "\\|");
    }

    /**
     * 解析后的性能分析数据结构
     * 包含从collapsed格式中提取的所有性能指标
     */
    private static class CollapsedProfile {
        /** 总采样次数 */
        private long totalSamples;
        /** 各函数的自身采样次数（不包含子调用），key为函数名，value为采样次数 */
        private final Map<String, Long> selfSamples = new LinkedHashMap<>();
        /** 所有调用栈样本列表 */
        private final List<StackSample> stacks = new ArrayList<>();
        /** 按叶子函数（栈顶）分组的调用栈列表，用于展示每个函数的top调用栈 */
        private final Map<String, List<StackSample>> topStacksByLeaf = new LinkedHashMap<>();
        /** 调用树的根节点，用于构建调用层次结构 */
        private final Node callTreeRoot = new Node();
    }

    /**
     * 调用栈样本
     * 表示一个完整的调用路径及其采样次数
     */
    private static class StackSample {
        /** 调用栈字符串，帧之间用分号分隔 */
        private final String stack;
        /** 该调用栈的采样次数 */
        private final long samples;

        /**
         * 创建调用栈样本
         * @param stack 调用栈字符串
         * @param samples 采样次数
         */
        private StackSample(String stack, long samples) {
            this.stack = stack;
            this.samples = samples;
        }
    }

    /**
     * 键值对条目
     * 用于存储热点函数的名称和采样次数
     */
    private static class Entry {
        /** 函数名或标识符 */
        private final String key;
        /** 采样次数或其他数值 */
        private final long value;

        /**
         * 创建条目
         * @param key 键
         * @param value 值
         */
        private Entry(String key, long value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * 调用树节点
     * 用于构建函数调用的层次结构
     */
    private static class Node {
        /** 该节点（函数）的采样次数 */
        private long samples;
        /** 子节点集合，key为函数名，value为对应的子节点 */
        private final Map<String, Node> children = new LinkedHashMap<>();
    }

    /**
     * 解析collapsed格式的性能分析数据
     * Collapsed格式：frame1;frame2;frame3 <samples>
     * 每行表示一个调用栈及其采样次数
     *
     * @param collapsed collapsed格式的原始数据
     * @param threadsEnabled 是否启用了线程模式
     * @return 解析后的性能分析数据结构
     */
    private static CollapsedProfile parseCollapsed(String collapsed, boolean threadsEnabled) {
        CollapsedProfile profile = new CollapsedProfile();

        // 按行分割数据
        String[] lines = collapsed.split("\\r?\\n");
        for (String line : lines) {
            // 跳过空行
            if (line == null) {
                continue;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // collapsed 格式：frame1;frame2;frame3 <samples>
            // 查找最后一个空格，前面是调用栈，后面是采样次数
            int spaceIdx = lastSpaceIndex(line);
            if (spaceIdx < 0) {
                continue;
            }
            String stack = line.substring(0, spaceIdx).trim();
            String countStr = line.substring(spaceIdx + 1).trim();
            if (stack.isEmpty() || countStr.isEmpty()) {
                continue;
            }

            // 解析采样次数
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

            // 将调用栈添加到调用树中
            addToCallTree(profile.callTreeRoot, normalizedStack, samples);

            // 获取栈顶帧（当前执行的函数）
            String topFrame = topFrame(normalizedStack, threadsEnabled);
            if (topFrame == null || topFrame.isEmpty()) {
                continue;
            }
            // 累加该函数的自身采样次数
            Long old = profile.selfSamples.get(topFrame);
            profile.selfSamples.put(topFrame, old == null ? samples : old + samples);

            // 将该调用栈添加到对应函数的top调用栈列表中
            addTopStacksByLeaf(profile.topStacksByLeaf, topFrame, normalizedStack, samples, 3);
        }

        return profile;
    }

    /**
     * 查找字符串中最后一个空格的位置
     * 用于分离调用栈和采样次数
     * 兼容调用栈中包含空格（如线程名）的情况
     *
     * @param s 输入字符串
     * @return 最后一个空格的索引，不存在返回-1
     */
    private static int lastSpaceIndex(String s) {
        // 兼容 stack 中包含空格（线程名）时，取最后一个空格作为 count 分隔
        for (int i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) == ' ') {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取调用栈的栈顶帧（当前执行的函数）
     * 在线程模式下，会跳过线程相关的帧，返回真正的业务函数
     *
     * @param stack 调用栈字符串
     * @param threadsEnabled 是否启用线程模式
     * @return 栈顶帧的函数名
     */
    private static String topFrame(String stack, boolean threadsEnabled) {
        if (stack == null || stack.isEmpty()) {
            return “”;
        }
        // 查找最后一个分号，分号后的是栈顶帧
        int lastSep = stack.lastIndexOf(';');
        String top = lastSep >= 0 ? stack.substring(lastSep + 1) : stack;
        top = top.trim();

        // threads 模式下，async-profiler 会以”线程帧”结束；这类帧对定位热点帮助有限，尽量跳过：
        // 规则：如果 top 看起来是 [thread] 或包含 “(thread)” 等，这里仅做轻量过滤，避免误删正常 frame。
        if (threadsEnabled && looksLikeThreadFrame(top)) {
            // 回退到倒数第二帧
            int prevSep = lastSep >= 0 ? stack.lastIndexOf(';', lastSep - 1) : -1;
            String prev = prevSep >= 0 ? stack.substring(prevSep + 1, lastSep) : “”;
            return prev.trim();
        }
        return top;
    }

    /**
     * 移除调用栈中的线程帧
     * 在线程模式下，async-profiler会在调用栈末尾添加线程信息帧
     * 这个帧对性能分析帮助不大，可以移除以减少噪音
     *
     * @param stack 调用栈字符串
     * @param threadsEnabled 是否启用线程模式
     * @return 移除线程帧后的调用栈
     */
    private static String stripThreadFrame(String stack, boolean threadsEnabled) {
        if (!threadsEnabled || stack == null || stack.isEmpty()) {
            return stack;
        }
        int lastSep = stack.lastIndexOf(';');
        if (lastSep < 0) {
            return stack;
        }
        String last = stack.substring(lastSep + 1).trim();
        // async-profiler threads 模式下常见线程帧形如：[tid=1234] “thread-name”
        if (last.startsWith(“[“) && last.contains(“tid=”)) {
            return stack.substring(0, lastSep);
        }
        return stack;
    }

    /**
     * 将调用栈添加到调用树中
     * 构建函数调用的层次结构，每个节点记录其采样次数
     *
     * @param root 调用树的根节点
     * @param stack 调用栈字符串
     * @param samples 该调用栈的采样次数
     */
    private static void addToCallTree(Node root, String stack, long samples) {
        if (root == null || stack == null || stack.isEmpty() || samples <= 0) {
            return;
        }
        // 累加根节点的采样次数
        root.samples += samples;
        // 按分号分割调用栈，获取每一帧
        String[] frames = stack.split(";");
        Node current = root;
        // 遍历每一帧，构建或更新调用树
        for (String frame : frames) {
            String f = frame == null ? "" : frame.trim();
            if (f.isEmpty()) {
                continue;
            }
            // 获取或创建子节点
            Node child = current.children.get(f);
            if (child == null) {
                child = new Node();
                current.children.put(f, child);
            }
            // 累加子节点的采样次数
            child.samples += samples;
            current = child;
        }
    }

    /**
     * 渲染调用树为文本格式
     * 只显示前N个最热门的根调用，每层最多显示5个子调用
     *
     * @param root 调用树的根节点
     * @param totalSamples 总采样次数
     * @param topN 显示前N个热点
     * @return 格式化的调用树文本
     */
    private static String renderCallTree(Node root, long totalSamples, int topN) {
        if (root == null || root.children.isEmpty() || totalSamples <= 0) {
            return "-";
        }
        // 限制最大深度和每层显示的子节点数量，避免输出过大
        int maxDepth = 8;
        int maxChildren = 5;
        int maxRoots = Math.min(topN, 10);
        StringBuilder sb = new StringBuilder(4096);
        // 按采样次数排序子节点
        List<Map.Entry<String, Node>> roots = sortChildren(root);
        for (int i = 0; i < roots.size() && i < maxRoots; i++) {
            Map.Entry<String, Node> e = roots.get(i);
            // 递归渲染每个子树
            renderCallTreeNode(sb, e.getKey(), e.getValue(), totalSamples, 0, maxDepth, maxChildren);
        }
        return sb.toString().trim();
    }

    /**
     * 递归渲染调用树节点
     * 每个节点显示其百分比和采样次数，并根据深度缩进
     *
     * @param sb 输出字符串构建器
     * @param label 节点标签（函数名）
     * @param node 调用树节点
     * @param totalSamples 总采样次数
     * @param depth 当前深度
     * @param maxDepth 最大深度限制
     * @param maxChildren 每层最大子节点数
     */
    private static void renderCallTreeNode(StringBuilder sb, String label, Node node, long totalSamples,
                                           int depth, int maxDepth, int maxChildren) {
        if (node == null || label == null) {
            return;
        }
        // 超过最大深度则停止
        if (depth >= maxDepth) {
            return;
        }
        // 根据深度添加缩进
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        // 输出节点信息：百分比、采样次数、函数名
        sb.append("(").append(formatPercent(node.samples, totalSamples)).append(") ")
                .append(node.samples).append(" ")
                .append(label).append("\n");

        // 如果没有子节点，直接返回
        if (node.children.isEmpty()) {
            return;
        }
        // 按采样次数排序子节点
        List<Map.Entry<String, Node>> children = sortChildren(node);
        // 递归渲染子节点，最多渲染maxChildren个
        for (int i = 0; i < children.size() && i < maxChildren; i++) {
            Map.Entry<String, Node> child = children.get(i);
            renderCallTreeNode(sb, child.getKey(), child.getValue(), totalSamples, depth + 1, maxDepth, maxChildren);
        }
    }

    /**
     * 对节点的子节点按采样次数降序排序
     * 采样次数多的排在前面
     *
     * @param node 调用树节点
     * @return 排序后的子节点列表
     */
    private static List<Map.Entry<String, Node>> sortChildren(Node node) {
        if (node == null || node.children.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map.Entry<String, Node>> list = new ArrayList<>(node.children.entrySet());
        // 按采样次数降序排序
        list.sort(new Comparator<Map.Entry<String, Node>>() {
            @Override
            public int compare(Map.Entry<String, Node> a, Map.Entry<String, Node> b) {
                return Long.compare(b.getValue().samples, a.getValue().samples);
            }
        });
        return list;
    }

    /**
     * 将调用栈添加到对应叶子函数的top调用栈列表中
     * 每个函数保留top N个最热门的调用栈
     *
     * @param topStacksByLeaf 按叶子函数分组的调用栈映射
     * @param leaf 叶子函数名（栈顶帧）
     * @param stack 完整的调用栈
     * @param samples 采样次数
     * @param limit 每个函数保留的最大调用栈数量
     */
    private static void addTopStacksByLeaf(Map<String, List<StackSample>> topStacksByLeaf,
                                          String leaf, String stack, long samples, int limit) {
        if (topStacksByLeaf == null || leaf == null || leaf.isEmpty() || stack == null || stack.isEmpty()) {
            return;
        }
        // 获取或创建该函数的调用栈列表
        List<StackSample> list = topStacksByLeaf.get(leaf);
        if (list == null) {
            list = new ArrayList<>();
            topStacksByLeaf.put(leaf, list);
        }
        // 添加新的调用栈样本
        list.add(new StackSample(stack, samples));
        // 按采样次数降序排序
        list.sort(new Comparator<StackSample>() {
            @Override
            public int compare(StackSample a, StackSample b) {
                return Long.compare(b.samples, a.samples);
            }
        });
        // 限制列表大小，只保留top N
        if (list.size() > limit) {
            list.subList(limit, list.size()).clear();
        }
    }

    /**
     * 判断一个帧是否看起来像线程帧
     * 线程帧对性能分析帮助不大，通常需要过滤掉
     *
     * @param frame 帧名称
     * @return 如果是线程帧返回true
     */
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
        // Thread.run也是线程相关的方法
        if (f.startsWith("java.lang.Thread.run") || f.startsWith("java.base/java.lang.Thread.run")) {
            return true;
        }
        return false;
    }

    /**
     * 获取采样次数最多的前N个热点函数
     *
     * @param selfSamples 函数名到采样次数的映射
     * @param topN 返回前N个热点
     * @return 按采样次数降序排序的条目列表
     */
    private static List<Entry> topHotspots(Map<String, Long> selfSamples, int topN) {
        if (selfSamples == null || selfSamples.isEmpty()) {
            return Collections.emptyList();
        }
        // 转换为Entry列表
        List<Entry> list = new ArrayList<>(selfSamples.size());
        for (Map.Entry<String, Long> e : selfSamples.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                list.add(new Entry(e.getKey(), e.getValue()));
            }
        }
        // 按采样次数降序排序
        list.sort(new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                return Long.compare(b.value, a.value);
            }
        });
        // 只返回前N个
        if (topN <= 0 || topN >= list.size()) {
            return list;
        }
        return new ArrayList<>(list.subList(0, topN));
    }

    /**
     * 获取采样次数最多的前N个调用栈
     *
     * @param stacks 所有调用栈样本列表
     * @param topN 返回前N个
     * @return 按采样次数降序排序的调用栈列表
     */
    private static List<StackSample> topStacks(List<StackSample> stacks, int topN) {
        if (stacks == null || stacks.isEmpty()) {
            return Collections.emptyList();
        }
        List<StackSample> list = new ArrayList<>(stacks);
        // 按采样次数降序排序
        list.sort(new Comparator<StackSample>() {
            @Override
            public int compare(StackSample a, StackSample b) {
                return Long.compare(b.samples, a.samples);
            }
        });
        // 只返回前N个
        if (topN <= 0 || topN >= list.size()) {
            return list;
        }
        return new ArrayList<>(list.subList(0, topN));
    }
}
