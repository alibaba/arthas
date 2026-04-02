package com.taobao.arthas.core.command.monitor200;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.HeapDumpModel;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 堆转储命令
 *
 * 该命令用于生成JVM的堆转储文件（heap dump），可以将JVM堆内存中的对象信息导出到文件中。
 * 堆转储文件可以用于分析内存泄漏、对象分布等问题，常与MAT、jhat等工具配合使用。
 *
 * 使用示例：
 *   heapdump                              # 导出到临时文件
 *   heapdump --live                       # 只导出存活对象到临时文件
 *   heapdump --live /tmp/dump.hprof       # 导出存活对象到指定文件
 *
 * @author hengyunabc 2019-09-02
 */
@Name("heapdump")
@Summary("Heap dump")
@Description("\nExamples:\n" + "  heapdump\n" + "  heapdump --live\n" + "  heapdump --live /tmp/dump.hprof\n"
                + Constants.WIKI + Constants.WIKI_HOME + "heapdump")
public class HeapDumpCommand extends AnnotatedCommand {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(HeapDumpCommand.class);

    // 堆转储文件的输出路径
    private String file;

    // 是否只转储存活对象（true表示只转储存活对象，false表示转储所有对象）
    private boolean live;

    /**
     * 设置堆转储文件路径
     *
     * @param file 堆转储文件的输出路径，可选参数
     */
    @Argument(argName = "file", index = 0, required = false)
    @Description("Output file")
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * 设置是否只转储存活对象
     *
     * @param live true表示只转储存活对象，false表示转储堆中所有对象
     */
    @Option(shortName = "l", longName = "live", flag = true)
    @Description("Dump only live objects; if not specified, all objects in the heap are dumped.")
    public void setLive(boolean live) {
        this.live = live;
    }

    /**
     * 执行堆转储命令
     *
     * 该方法处理堆转储命令的核心逻辑：
     * 1. 确定输出文件路径（如未指定则生成临时文件）
     * 2. 调用HotSpotDiagnosticMXBean执行堆转储
     * 3. 将转储结果添加到命令进程
     *
     * @param process 命令进程对象
     */
    @Override
    public void process(CommandProcess process) {
        try {
            // 确定堆转储文件的路径
            String dumpFile = file;
            if (dumpFile == null || dumpFile.isEmpty()) {
                // 如果用户未指定文件路径，则生成临时文件
                // 文件名格式：heapdump + 日期时间 + (-live如果只转储存活对象) + .hprof
                String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());
                File file = File.createTempFile("heapdump" + date + (live ? "-live" : ""), ".hprof");
                dumpFile = file.getAbsolutePath();
                // 删除空文件，dumpHeap会重新创建
                file.delete();
            }

            // 添加进度消息，告知用户正在导出堆
            process.appendResult(new MessageModel("Dumping heap to " + dumpFile + " ..."));

            // 执行实际的堆转储操作
            run(process, dumpFile, live);

            // 堆转储完成，添加成功消息
            process.appendResult(new MessageModel("Heap dump file created"));
            // 添加堆转储结果模型，包含文件路径和是否只转储存活对象的信息
            process.appendResult(new HeapDumpModel(dumpFile, live));
            // 标记命令执行完成
            process.end();
        } catch (Throwable t) {
            // 处理堆转储过程中的异常
            String errorMsg = "heap dump error: " + t.getMessage();
            logger.error(errorMsg, t);
            // 以错误状态码结束命令
            process.end(-1, errorMsg);
        }

    }

    /**
     * 执行堆转储操作
     *
     * 该方法通过HotSpotDiagnosticMXBean执行实际的堆转储。
     * HotSpotDiagnosticMXBean是JDK提供的用于管理HotSpot JVM的MBean。
     *
     * @param process 命令进程对象
     * @param file 堆转储文件的输出路径
     * @param live true表示只转储存活对象，false表示转储所有对象
     * @throws IOException 如果发生IO异常
     */
    private static void run(CommandProcess process, String file, boolean live) throws IOException {
        // 获取HotSpot诊断MBean
        // 该MBean提供了诊断JVM的功能，包括堆转储、VM选项等
        HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory
                        .getPlatformMXBean(HotSpotDiagnosticMXBean.class);

        // 调用dumpHeap方法执行堆转储
        // 参数1: 输出文件路径
        // 参数2: 是否只转储存活对象（true表示只转储可达对象，false表示转储所有对象）
        hotSpotDiagnosticMXBean.dumpHeap(file, live);
    }

}
