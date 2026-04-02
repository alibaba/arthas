package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.StackModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.ThreadUtil;

/**
 * 堆栈信息的终端视图类
 * 负责将线程堆栈信息渲染为易读的文本格式并输出到终端
 *
 * @author gongdewei 2020/4/13
 */
public class StackView extends ResultView<StackModel> {

    /**
     * 绘制堆栈信息视图
     * 将堆栈跟踪信息格式化并输出到命令行进程
     *
     * @param process 命令处理进程，用于输出结果
     * @param result 堆栈模型对象，包含线程信息和堆栈跟踪数据
     */
    @Override
    public void draw(CommandProcess process, StackModel result) {
        // 创建字符串构建器，用于构建输出内容
        StringBuilder sb = new StringBuilder();
        // 添加线程标题信息（包含线程ID、名称、状态等）
        sb.append(ThreadUtil.getThreadTitle(result)).append("\n");

        // 获取堆栈跟踪元素数组
        StackTraceElement[] stackTraceElements = result.getStackTrace();
        // 获取第一个堆栈元素（即当前代码执行位置）
        StackTraceElement locationStackTraceElement = stackTraceElements[0];
        // 格式化位置信息：@类名.方法名()
        String locationString = String.format("    @%s.%s()", locationStackTraceElement.getClassName(),
                locationStackTraceElement.getMethodName());
        sb.append(locationString).append("\n");

        // 从第二个元素开始遍历堆栈（跳过第一个，因为已经单独处理了）
        int skip = 1;
        for (int index = skip; index < stackTraceElements.length; index++) {
            StackTraceElement ste = stackTraceElements[index];
            // 格式化每个堆栈元素：at 类名.方法名(文件名:行号)
            sb.append("        at ")
                    .append(ste.getClassName())
                    .append(".")
                    .append(ste.getMethodName())
                    .append("(")
                    .append(ste.getFileName())
                    .append(":")
                    .append(ste.getLineNumber())
                    .append(")\n");
        }
        // 输出完整的时间戳和堆栈信息
        process.write("ts=" + DateUtils.formatDateTime(result.getTs()) + ";" + sb.toString() + "\n");
    }

}
