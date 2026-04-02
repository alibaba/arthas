package com.taobao.arthas.core.command.basic1000;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ChangeResultVO;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.VMOptionModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * JVM选项命令类
 *
 * 用于查看和修改JVM诊断选项（VM Options）。
 * 这些选项通常用于JVM调试和性能调优，例如GC日志打印等。
 * 注意：只有可写的JVM选项才能被修改，只读选项无法修改。
 *
 * 使用示例：
 * - vmoption: 显示所有可修改的JVM诊断选项
 * - vmoption PrintGC: 查看指定选项的值
 * - vmoption PrintGC true: 修改指定选项的值
 * - vmoption PrintGCDetails true: 启用GC详细信息输出
 *
 * @author hengyunabc 2019-09-02
 *
 */
// @formatter:off
@Name("vmoption")
@Summary("Display, and update the vm diagnostic options.")
@Description("\nExamples:\n" +
        "  vmoption\n" +
        "  vmoption PrintGC\n" +
        "  vmoption PrintGC true\n" +
        "  vmoption PrintGCDetails true\n" +
        Constants.WIKI + Constants.WIKI_HOME + "vmoption")
//@formatter:on
public class VMOptionCommand extends AnnotatedCommand {
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(VMOptionCommand.class);

    /** JVM选项名称 */
    private String name;
    /** JVM选项值 */
    private String value;

    /**
     * 设置JVM选项名称
     *
     * @param name JVM选项名称，例如 "PrintGC"
     */
    @Argument(index = 0, argName = "name", required = false)
    @Description("VMOption name")
    public void setOptionName(String name) {
        this.name = name;
    }

    /**
     * 设置JVM选项值
     *
     * @param value JVM选项值，例如 "true" 或 "false"
     */
    @Argument(index = 1, argName = "value", required = false)
    @Description("VMOption value")
    public void setOptionValue(String value) {
        this.value = value;
    }

    /**
     * 处理命令执行逻辑
     *
     * 调用run方法执行具体的命令逻辑。
     *
     * @param process 命令处理进程对象
     */
    @Override
    public void process(CommandProcess process) {
        run(process, name, value);
    }

    /**
     * 执行命令的私有方法
     *
     * 根据参数不同执行不同操作：
     * 1. 无参数：显示所有可修改的JVM诊断选项
     * 2. 只有选项名：显示指定选项的值
     * 3. 选项名和选项值：修改JVM选项的值
     *
     * @param process 命令处理进程对象
     * @param name JVM选项名称
     * @param value JVM选项值
     */
    private static void run(CommandProcess process, String name, String value) {
        try {
            // 获取HotSpot诊断MXBean，用于访问和修改JVM选项
            HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory
                            .getPlatformMXBean(HotSpotDiagnosticMXBean.class);

            // 如果既没有指定选项名，也没有指定选项值，则显示所有可修改的JVM诊断选项
            if (StringUtils.isBlank(name) && StringUtils.isBlank(value)) {
                // 显示所有可修改的选项
                process.appendResult(new VMOptionModel(hotSpotDiagnosticMXBean.getDiagnosticOptions()));
            } else if (StringUtils.isBlank(value)) {
                // 只指定了选项名，查看指定的JVM选项
                VMOption option = hotSpotDiagnosticMXBean.getVMOption(name);
                if (option == null) {
                    // 选项不存在，返回错误信息
                    process.end(-1, "In order to change the system properties, you must specify the property value.");
                    return;
                } else {
                    // 返回该选项的详细信息
                    process.appendResult(new VMOptionModel(Collections.singletonList(option)));
                }
            } else {
                // 同时指定了选项名和选项值，修改JVM选项
                VMOption vmOption = hotSpotDiagnosticMXBean.getVMOption(name);
                // 保存原始值
                String originValue = vmOption.getValue();

                // 修改JVM选项
                hotSpotDiagnosticMXBean.setVMOption(name, value);
                // 返回成功消息
                process.appendResult(new MessageModel("Successfully updated the vm option."));
                // 返回修改前后的值对比
                process.appendResult(new VMOptionModel(new ChangeResultVO(name, originValue,
                        hotSpotDiagnosticMXBean.getVMOption(name).getValue())));
            }
            // 结束命令处理
            process.end();
        } catch (Throwable t) {
            // 捕获异常并记录日志
            logger.error("Error during setting vm option", t);
            // 返回错误信息
            process.end(-1, "Error during setting vm option: " + t.getMessage());
        }
    }

    /**
     * 命令自动补全功能
     *
     * 从所有可修改的JVM诊断选项中查找匹配的补全建议。
     *
     * @param completion 补全对象，用于提供补全建议
     */
    @Override
    public void complete(Completion completion) {
        // 获取HotSpot诊断MXBean
        HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory
                        .getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        // 获取所有可修改的诊断选项
        List<VMOption> diagnosticOptions = hotSpotDiagnosticMXBean.getDiagnosticOptions();
        // 创建选项名称列表
        List<String> names = new ArrayList<String>(diagnosticOptions.size());
        // 遍历所有选项，提取选项名称
        for (VMOption option : diagnosticOptions) {
            names.add(option.getName());
        }
        // 使用选项名称列表进行补全
        CompletionUtils.complete(completion, names);
    }
}
