package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ProfilerModel;
import com.taobao.arthas.core.command.monitor200.ProfilerCommand.ProfilerAction;
import com.taobao.arthas.core.shell.command.CommandProcess;


/**
 * ProfilerModel 的终端视图类
 * 用于展示 profiler 命令的执行结果，包括支持的操作、执行结果和输出文件信息
 *
 * @author gongdewei 2020/4/27
 */
public class ProfilerView extends ResultView<ProfilerModel> {
    /**
     * 绘制 profiler 命令的执行结果到命令行
     * 根据不同的操作类型（start、stop等）展示不同的信息
     *
     * @param process 命令进程对象，用于输出信息
     * @param model profiler 命令的模型对象，包含执行结果
     */
    @Override
    public void draw(CommandProcess process, ProfilerModel model) {
        // 如果有支持的操作列表，则展示支持的操作
        if (model.getSupportedActions() != null) {
            process.write("Supported Actions: " + model.getSupportedActions()).write("\n");
            return;
        }

        // 绘制执行结果
        drawExecuteResult(process, model);

        // 如果是启动操作
        if (ProfilerAction.start.name().equals(model.getAction())) {
            // 如果设置了持续时间，则展示自动停止提示和输出文件路径
            if (model.getDuration() != null) {
                process.write(String.format("profiler will silent stop after %d seconds.\n", model.getDuration().longValue()));
                process.write("profiler output file will be: " + model.getOutputFile() + "\n");
            }
        } else if (ProfilerAction.stop.name().equals(model.getAction())) {
            // 如果是停止操作，展示输出文件路径（markdown 格式时除外）
            // markdown 输出时，额外的提示行会影响复制粘贴给 LLM 的效果
            if (model.getOutputFile() != null && !isMarkdown(model.getFormat())) {
                process.write("profiler output file: " + model.getOutputFile() + "\n");
            }
        }

    }

    /**
     * 绘制执行结果到命令行
     * 将执行结果字符串写入命令行，确保以换行符结尾
     *
     * @param process 命令进程对象，用于输出信息
     * @param model profiler 命令的模型对象，包含执行结果
     */
    private void drawExecuteResult(CommandProcess process, ProfilerModel model) {
        if (model.getExecuteResult() != null) {
            // 写入执行结果
            process.write(model.getExecuteResult());
            // 如果结果字符串没有以换行符结尾，则添加换行符
            if (!model.getExecuteResult().endsWith("\n")) {
                process.write("\n");
            }
        }
    }

    /**
     * 判断输出格式是否为 Markdown 格式
     *
     * @param format 输出格式字符串
     * @return 如果是 Markdown 格式返回 true，否则返回 false
     */
    private boolean isMarkdown(String format) {
        return format != null && format.toLowerCase().startsWith("md");
    }
}
