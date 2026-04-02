package com.taobao.arthas.core.command.monitor200;


import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.JavaVersionUtils;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.PerfCounterModel;
import com.taobao.arthas.core.command.model.PerfCounterVO;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import sun.management.counter.Counter;
import sun.management.counter.perf.PerfInstrumentation;

/**
 * 性能计数器命令
 * 用于显示和获取 JVM 的性能计数器信息
 *
 * 相关类说明：
 * @see sun.misc.Perf - JDK8 中的性能计数器类
 * @see jdk.internal.perf.Perf - JDK9+ 中的性能计数器类
 * @see sun.management.counter.perf.PerfInstrumentation - 性能计数器工具类
 *
 * @author hengyunabc 2020-02-16
 */
@Name("perfcounter")
@Summary("Display the perf counter information.")
@Description("\nExamples:\n" +
        "  perfcounter\n" +
        "  perfcounter -d\n" +
        Constants.WIKI + Constants.WIKI_HOME + "perfcounter")
public class PerfCounterCommand extends AnnotatedCommand {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(PerfCounterCommand.class);
    // Perf 对象实例（静态缓存，避免重复获取）
    private static Object perfObject;
    // attach 方法（静态缓存，避免重复反射获取）
    private static Method attachMethod;

    // 是否显示详细信息（包括单位和可变性）
    private boolean details;

    /**
     * 设置是否显示详细信息
     * @param details true 显示详细信息，false 只显示基本信息
     */
    @Option(shortName = "d", longName = "details", flag = true)
    @Description("print all perf counter details")
    public void setDetails(boolean details) {
        this.details = details;
    }

    /**
     * 处理命令执行
     * 获取并显示性能计数器信息
     *
     * @param process 命令处理进程
     */
    @Override
    public void process(CommandProcess process) {
        // 获取所有性能计数器
        List<Counter> perfCounters = getPerfCounters();
        // 如果没有获取到任何计数器，给出提示信息
        if (perfCounters.isEmpty()) {
            process.end(1,
                    "please check arthas log. if java version >=9 , try to add jvm options when start your process: "
                            + "--add-opens java.base/jdk.internal.perf=ALL-UNNAMED "
                            + "--add-exports java.base/jdk.internal.perf=ALL-UNNAMED "
                            + "--add-opens java.management/sun.management.counter.perf=ALL-UNNAMED "
                            + "--add-opens java.management/sun.management.counter=ALL-UNNAMED");
            return;
        }

        // 创建性能计数器 VO 列表
        List<PerfCounterVO> perfCounterVOs = new ArrayList<PerfCounterVO>();
        // 遍历所有计数器，转换为 VO 对象
        for (Counter counter : perfCounters) {
            // 创建 VO 对象，包含名称和值
            PerfCounterVO perfCounterVO = new PerfCounterVO(counter.getName(), counter.getValue());
            // 如果需要详细信息，添加单位和可变性
            if (details) {
                perfCounterVO.setUnits(counter.getUnits().toString());
                perfCounterVO.setVariability(counter.getVariability().toString());
            }
            perfCounterVOs.add(perfCounterVO);
        }
        // 将结果追加到进程并结束
        process.appendResult(new PerfCounterModel(perfCounterVOs, details));
        process.end();
    }

    /**
     * 获取性能计数器列表
     * 使用反射方式获取 JVM 的性能计数器信息
     *
     * 实现原理：
     * <pre>
     * Perf p = Perf.getPerf();
     * ByteBuffer buffer = p.attach(pid, "r");
     * </pre>
     *
     * @return 性能计数器列表，如果获取失败则返回空列表
     */
    private static List<Counter> getPerfCounters() {

        /**
         * 实现逻辑说明：
         * 1. 获取 Perf 对象（JDK8 和 JDK9+ 的类路径不同）
         * 2. 调用 attach 方法附加到当前进程
         * 3. 使用 PerfInstrumentation 解析性能数据
         * 4. 返回所有计数器
         */
        try {
            // 如果 Perf 对象还未初始化，进行初始化
            if (perfObject == null) {
                // jdk8 的 Perf 类路径
                String perfClassName = "sun.misc.Perf";
                // jdk 11+ 的 Perf 类路径
                if (!JavaVersionUtils.isLessThanJava9()) {
                    perfClassName = "jdk.internal.perf.Perf";
                }

                // 使用系统类加载器加载 Perf 类
                Class<?> perfClass = ClassLoader.getSystemClassLoader().loadClass(perfClassName);
                // 获取 getPerf 静态方法
                Method getPerfMethod = perfClass.getDeclaredMethod("getPerf");
                // 调用 getPerf 方法获取 Perf 实例
                perfObject = getPerfMethod.invoke(null);
            }

            // 如果 attach 方法还未获取，进行反射获取
            if (attachMethod == null) {
                attachMethod = perfObject.getClass().getDeclaredMethod("attach",
                        new Class<?>[] { int.class, String.class });
            }

            // 调用 attach 方法，附加到当前进程，以只读模式打开
            ByteBuffer buffer = (ByteBuffer) attachMethod.invoke(perfObject,
                    new Object[] { (int) PidUtils.currentLongPid(), "r" });

            // 创建性能计数器工具对象，解析 ByteBuffer
            PerfInstrumentation perfInstrumentation = new PerfInstrumentation(buffer);
            // 返回所有计数器
            return perfInstrumentation.getAllCounters();
        } catch (Throwable e) {
            // 发生异常时记录错误日志
            logger.error("get perf counter error", e);
        }
        // 返回空列表表示获取失败
        return Collections.emptyList();
    }
}
