/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package one.profiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 进程内性能分析的 Java API
 * 作为 async-profiler 原生库的包装器，提供 Java 层面的性能分析功能
 * 该类是单例模式，首次调用 {@link #getInstance()} 时会加载 libasyncProfiler.so
 *
 * async-profiler 是一个低开销的 Java 性能分析工具，可以：
 * - 分析 CPU 性能热点
 * - 内存分配分析
 * - 锁竞争分析
 * - 生成火焰图等多种格式的报告
 */
public class AsyncProfiler implements AsyncProfilerMXBean {
    /**
     * 单例实例，确保整个 JVM 中只有一个 AsyncProfiler 实例
     */
    private static AsyncProfiler instance;

    /**
     * 私有构造函数，防止外部直接创建实例
     * 确保只能通过 getInstance() 方法获取单例
     */
    private AsyncProfiler() {
    }

    /**
     * 获取 AsyncProfiler 单例实例
     * 使用默认方式加载本地库
     *
     * @return AsyncProfiler 单例实例
     */
    public static AsyncProfiler getInstance() {
        return getInstance(null);
    }

    /**
     * 获取 AsyncProfiler 单例实例（带参数）
     * 支持指定本地库路径或使用默认加载方式
     * 该方法是线程安全的，使用 synchronized 保证单例的唯一性
     *
     * @param libPath 本地库的路径，如果为 null 则使用默认加载方式
     * @return AsyncProfiler 单例实例
     */
    public static synchronized AsyncProfiler getInstance(String libPath) {
        // 如果实例已存在，直接返回
        if (instance != null) {
            return instance;
        }

        // 创建新的 profiler 实例
        AsyncProfiler profiler = new AsyncProfiler();

        // 如果指定了库路径，直接加载该库
        if (libPath != null) {
            System.load(libPath);
        } else {
            try {
                // 尝试调用 getVersion() 来验证库是否已通过 -agentpath 参数预加载
                // 如果已预加载，这个调用会成功
                profiler.getVersion();
            } catch (UnsatisfiedLinkError e) {
                // 库未加载，尝试其他方式加载

                // 检查系统属性是否指定了库路径
                String libraryPath = System.getProperty("one.profiler.libraryPath");
                if (libraryPath != null && !libraryPath.isEmpty()) {
                    // 使用系统属性指定的路径加载库
                    System.load(new File(libraryPath).getAbsolutePath());
                } else {
                    // 尝试从嵌入资源中提取库文件
                    File file = extractEmbeddedLib();
                    if (file != null) {
                        try {
                            // 加载提取的临时库文件
                            System.load(file.getAbsolutePath());
                        } finally {
                            // 加载完成后删除临时文件
                            file.delete();
                        }
                    } else {
                        // 使用标准方式加载库（从 java.library.path）
                        System.loadLibrary("asyncProfiler");
                    }
                }

            }
        }

        // 保存实例引用
        instance = profiler;
        return profiler;
    }

    /**
     * 从嵌入的资源中提取本地库文件
     * 该方法会从 JAR 包中提取对应平台的 libasyncProfiler.so 文件到临时目录
     *
     * @return 提取的临时文件，如果资源不存在则返回 null
     */
    private static File extractEmbeddedLib() {
        // 构建资源名称路径，格式为 /平台标识/libasyncProfiler.so
        String resourceName = "/" + getPlatformTag() + "/libasyncProfiler.so";

        // 从类路径中获取资源输入流
        InputStream in = AsyncProfiler.class.getResourceAsStream(resourceName);
        if (in == null) {
            // 资源不存在，返回 null
            return null;
        }

        try {
            // 获取系统属性指定的提取路径
            String extractPath = System.getProperty("one.profiler.extractPath");

            // 创建临时文件，文件名前缀为 libasyncProfiler-，后缀为 .so
            // 如果指定了提取路径，则在指定目录创建；否则在系统默认临时目录创建
            File file = File.createTempFile("libasyncProfiler-", ".so",
                    extractPath == null || extractPath.isEmpty() ? null : new File(extractPath));

            // 使用 try-with-resources 自动关闭输出流
            try (FileOutputStream out = new FileOutputStream(file)) {
                // 创建缓冲区，每次读取 32KB
                byte[] buf = new byte[32000];

                // 循环读取输入流并写入输出流
                for (int bytes; (bytes = in.read(buf)) >= 0; ) {
                    out.write(buf, 0, bytes);
                }
            }

            // 返回提取的临时文件
            return file;
        } catch (IOException e) {
            // 提取失败，抛出异常
            throw new IllegalStateException(e);
        } finally {
            // 确保关闭输入流
            try {
                in.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 获取当前平台的标识符
     * 根据操作系统和 CPU 架构返回对应的平台标签
     * 用于确定加载哪个版本的本机库
     *
     * @return 平台标识符，如 "linux-x64"、"macos" 等
     * @throws UnsupportedOperationException 如果平台不支持
     */
    private static String getPlatformTag() {
        // 获取操作系统名称并转为小写
        String os = System.getProperty("os.name").toLowerCase();

        // 获取 CPU 架构并转为小写
        String arch = System.getProperty("os.arch").toLowerCase();

        // 检查是否是 Linux 系统
        if (os.contains("linux")) {
            // 检查各种 x86_64 架构的别名
            if (arch.equals("amd64") || arch.equals("x86_64") || arch.contains("x64")) {
                return "linux-x64";
            // 检查 ARM64 架构
            } else if (arch.equals("aarch64") || arch.contains("arm64")) {
                return "linux-arm64";
            // 检查 ARM32 架构
            } else if (arch.equals("aarch32") || arch.contains("arm")) {
                return "linux-arm32";
            // 检查 x86 架构（32位）
            } else if (arch.contains("86")) {
                return "linux-x86";
            // 检查 PowerPC 64位小端序架构
            } else if (arch.contains("ppc64")) {
                return "linux-ppc64le";
            }
        // 检查是否是 macOS 系统
        } else if (os.contains("mac")) {
            return "macos";
        }

        // 不支持的平台，抛出异常
        throw new UnsupportedOperationException("Unsupported platform: " + os + "-" + arch);
    }

    /**
     * 启动性能分析
     * 开始收集性能数据，会重置之前收集的数据
     *
     * @param event    性能分析事件类型，参见 {@link Events}
     *                 常见事件包括：cpu、alloc、lock、cache-misses 等
     * @param interval 采样间隔，对于 CPU 事件单位为纳秒
     *                 间隔越小，采样频率越高，数据越精确，但开销也越大
     * @throws IllegalStateException 如果性能分析器已在运行
     */
    @Override
    public void start(String event, long interval) throws IllegalStateException {
        // 检查事件参数是否为 null
        if (event == null) {
            throw new NullPointerException();
        }

        // 调用本地方法启动性能分析，reset 参数为 true 表示重置数据
        start0(event, interval, true);
    }

    /**
     * 恢复或继续性能分析，不重置已收集的数据
     * 注意：事件类型和采样间隔可能与之前的性能分析会话不同
     *
     * @param event    性能分析事件类型，参见 {@link Events}
     * @param interval 采样间隔，对于 CPU 事件单位为纳秒
     * @throws IllegalStateException 如果性能分析器已在运行
     */
    @Override
    public void resume(String event, long interval) throws IllegalStateException {
        // 检查事件参数是否为 null
        if (event == null) {
            throw new NullPointerException();
        }

        // 调用本地方法启动性能分析，reset 参数为 false 表示保留之前的数据
        start0(event, interval, false);
    }

    /**
     * 停止性能分析（不导出结果）
     * 停止数据收集，但不会自动导出结果
     * 可以继续使用 dump 系列方法导出已收集的数据
     *
     * @throws IllegalStateException 如果性能分析器未在运行
     */
    @Override
    public void stop() throws IllegalStateException {
        // 调用本地方法停止性能分析
        stop0();
    }

    /**
     * 获取性能分析期间收集的样本数量
     * 样本数量反映了性能分析的精度和持续时间
     *
     * @return 收集的样本总数
     */
    @Override
    public native long getSamples();

    /**
     * 获取性能分析器代理版本号
     * 例如："1.0"、"2.0" 等
     *
     * @return 版本字符串
     */
    @Override
    public String getVersion() {
        try {
            // 执行 version 命令获取版本信息
            return execute0("version");
        } catch (IOException e) {
            // IO 异常转换为 IllegalStateException
            throw new IllegalStateException(e);
        }
    }

    /**
     * 执行代理兼容的性能分析命令
     * 命令是在 arguments.cpp 中定义的逗号分隔参数列表
     *
     * 支持的命令示例：
     * - "start,event=cpu,interval=1000000" - 启动 CPU 性能分析
     * - "stop" - 停止性能分析
     * - "dump,file=/tmp/profile.jfr" - 导出 JFR 格式文件
     *
     * @param command 性能分析命令字符串
     * @return 命令执行结果
     * @throws IllegalArgumentException 如果命令解析失败
     * @throws IOException              如果创建输出文件失败
     */
    @Override
    public String execute(String command) throws IllegalArgumentException, IllegalStateException, IOException {
        // 检查命令参数是否为 null
        if (command == null) {
            throw new NullPointerException();
        }

        // 调用本地方法执行命令
        return execute0(command);
    }

    /**
     * 以"折叠堆栈"（collapsed stacktraces）格式导出性能分析数据
     * 这种格式常用于生成火焰图
     * 每行代表一个调用栈及其样本数
     *
     * @param counter 要在输出中显示的计数器类型
     *                例如：SAMPLES（样本数）、TOTAL（总时间）等
     * @return 性能分析数据的文本表示
     */
    @Override
    public String dumpCollapsed(Counter counter) {
        try {
            // 执行 collapsed 命令，指定计数器类型
            return execute0("collapsed," + counter.name().toLowerCase());
        } catch (IOException e) {
            // IO 异常转换为 IllegalStateException
            throw new IllegalStateException(e);
        }
    }

    /**
     * 导出收集到的堆栈跟踪
     * 以文本格式展示每个采样点的调用栈
     *
     * @param maxTraces 要导出的堆栈跟踪最大数量
     *                 0 表示无限制，导出所有堆栈跟踪
     * @return 性能分析数据的文本表示
     */
    @Override
    public String dumpTraces(int maxTraces) {
        try {
            // 如果 maxTraces 为 0，导出所有堆栈；否则限制数量
            return execute0(maxTraces == 0 ? "traces" : "traces=" + maxTraces);
        } catch (IOException e) {
            // IO 异常转换为 IllegalStateException
            throw new IllegalStateException(e);
        }
    }

    /**
     * 导出平面性能分析数据，即最热方法的直方图
     * 每个方法一行，显示其样本数或耗时
     * 不包含调用关系，只展示方法自身的统计数据
     *
     * @param maxMethods 要导出的方法最大数量
     *                  0 表示无限制，导出所有方法
     * @return 性能分析数据的文本表示
     */
    @Override
    public String dumpFlat(int maxMethods) {
        try {
            // 如果 maxMethods 为 0，导出所有方法；否则限制数量
            return execute0(maxMethods == 0 ? "flat" : "flat=" + maxMethods);
        } catch (IOException e) {
            // IO 异常转换为 IllegalStateException
            throw new IllegalStateException(e);
        }
    }

    /**
     * 以 OTLP（OpenTelemetry Protocol）格式导出收集的数据
     * OTLP 是 OpenTelemetry 项目的标准协议，用于与可观测性平台集成
     *
     * <p>注意：此 API 是不稳定的（UNSTABLE），
     * 可能在 async-profiler 的下一个版本中更改或删除</p>
     *
     * @return 性能分析数据的 OTLP 格式表示（字节数组）
     */
    @Override
    public byte[] dumpOtlp() {
        try {
            // 执行 otlp 命令，返回字节数组
            return execute1("otlp");
        } catch (IOException e) {
            // IO 异常转换为 IllegalStateException
            throw new IllegalStateException(e);
        }
    }

    /**
     * 将指定线程添加到性能分析线程集合中
     * 用于只分析特定线程的性能
     *
     * <p>注意：必须启用 'filter' 选项才能使用此方法</p>
     *
     * @param thread 要包含在性能分析中的线程
     */
    public void addThread(Thread thread) {
        // 调用内部方法，enable 参数为 true 表示启用该线程的过滤
        filterThread(thread, true);
    }

    /**
     * 从性能分析线程集合中移除指定线程
     * 该线程将不再被性能分析
     *
     * <p>注意：必须启用 'filter' 选项才能使用此方法</p>
     *
     * @param thread 要从性能分析中排除的线程
     */
    public void removeThread(Thread thread) {
        // 调用内部方法，enable 参数为 false 表示禁用该线程的过滤
        filterThread(thread, false);
    }

    /**
     * 线程过滤的内部实现
     * 处理线程添加和移除的通用逻辑
     *
     * @param thread 要过滤的线程，如果为 null 或当前线程则表示分析所有线程
     * @param enable true 表示添加到过滤集合，false 表示从过滤集合移除
     */
    private void filterThread(Thread thread, boolean enable) {
        // 如果线程为 null 或是当前线程，传递 null 给本地方法表示分析所有线程
        if (thread == null || thread == Thread.currentThread()) {
            filterThread0(null, enable);
        } else {
            // 需要获取线程锁，避免与线程状态变化产生竞态条件
            synchronized (thread) {
                // 获取线程当前状态
                Thread.State state = thread.getState();

                // 只有线程处于活跃状态（NEW 和 TERMINATED 除外）才能添加到过滤集合
                if (state != Thread.State.NEW && state != Thread.State.TERMINATED) {
                    // 调用本地方法设置线程过滤
                    filterThread0(thread, enable);
                }
            }
        }
    }

    /**
     * 启动性能分析的本地方法
     *
     * @param event 性能分析事件类型
     * @param interval 采样间隔
     * @param reset 是否重置之前收集的数据
     * @throws IllegalStateException 如果性能分析器已在运行
     */
    private native void start0(String event, long interval, boolean reset) throws IllegalStateException;

    /**
     * 停止性能分析的本地方法
     *
     * @throws IllegalStateException 如果性能分析器未在运行
     */
    private native void stop0() throws IllegalStateException;

    /**
     * 执行性能分析命令的本地方法
     *
     * @param command 要执行的命令字符串
     * @return 命令执行结果
     * @throws IllegalArgumentException 如果命令解析失败
     * @throws IllegalStateException 如果性能分析器状态异常
     * @throws IOException 如果 IO 操作失败
     */
    private native String execute0(String command) throws IllegalArgumentException, IllegalStateException, IOException;

    /**
     * 执行性能分析命令并返回字节数组的本地方法
     * 用于返回二进制格式的数据（如 OTLP 格式）
     *
     * @param command 要执行的命令字符串
     * @return 命令执行结果（字节数组）
     * @throws IllegalArgumentException 如果命令解析失败
     * @throws IllegalStateException 如果性能分析器状态异常
     * @throws IOException 如果 IO 操作失败
     */
    private native byte[] execute1(String command) throws IllegalArgumentException, IllegalStateException, IOException;

    /**
     * 设置线程过滤的本地方法
     *
     * @param thread 要过滤的线程，null 表示分析所有线程
     * @param enable true 表示启用过滤，false 表示禁用过滤
     */
    private native void filterThread0(Thread thread, boolean enable);
}