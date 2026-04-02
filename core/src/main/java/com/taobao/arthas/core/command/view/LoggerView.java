package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.logger.LoggerHelper;
import com.taobao.arthas.core.command.model.LoggerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.List;
import java.util.Map;

import static com.taobao.text.ui.Element.label;

/**
 * logger命令的视图类
 * 负责将日志框架的信息渲染为表格形式展示给用户
 *
 * @author gongdewei 2020/4/22
 */
public class LoggerView extends ResultView<LoggerModel> {

    /**
     * 绘制logger命令的执行结果
     *
     * @param process 命令处理进程，用于输出结果
     * @param result logger命令的执行结果模型
     */
    @Override
    public void draw(CommandProcess process, LoggerModel result) {
        // 如果结果中包含匹配的类加载器信息，则显示类加载器列表
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }
        // 否则渲染并输出日志框架的详细信息
        process.write(renderLoggerInfo(result.getLoggerInfoMap(), process.width()));
    }

    /**
     * 渲染日志框架的详细信息为表格字符串
     *
     * @param loggerInfos 日志信息映射表，key为日志名称，value为日志的详细属性信息
     * @param width 输出终端的宽度，用于表格渲染时的自适应
     * @return 渲染后的表格字符串
     */
    private String renderLoggerInfo(Map<String, Map<String, Object>> loggerInfos, int width) {
        // 创建字符串构建器，初始容量8192字节
        StringBuilder sb = new StringBuilder(8192);

        // 遍历每个日志配置
        for (Map.Entry<String, Map<String, Object>> entry : loggerInfos.entrySet()) {
            Map<String, Object> info = entry.getValue();

            // 创建主信息表格，2列10行的表格布局，左右内边距为1
            TableElement table = new TableElement(2, 10).leftCellPadding(1).rightCellPadding(1);
            // 创建Appender子表格，用于展示输出器信息
            TableElement appendersTable = new TableElement().rightCellPadding(1);

            // 获取日志类信息
            Class<?> clazz = (Class<?>) info.get(LoggerHelper.clazz);
            // 添加日志的基本信息行：名称、类名、类加载器、类加载器哈希值、日志级别
            table.row(label(LoggerHelper.name).style(Decoration.bold.bold()), label("" + info.get(LoggerHelper.name)))
                    .row(label(LoggerHelper.clazz).style(Decoration.bold.bold()), label("" + clazz.getName()))
                    .row(label(LoggerHelper.classLoader).style(Decoration.bold.bold()),
                            label("" + info.get(LoggerHelper.classLoader)))
                    .row(label(LoggerHelper.classLoaderHash).style(Decoration.bold.bold()),
                            label("" + info.get(LoggerHelper.classLoaderHash)))
                    .row(label(LoggerHelper.level).style(Decoration.bold.bold()),
                            label("" + info.get(LoggerHelper.level)));
            // 如果存在有效级别（effective level），则添加该行
            if (info.get(LoggerHelper.effectiveLevel) != null) {
                table.row(label(LoggerHelper.effectiveLevel).style(Decoration.bold.bold()),
                        label("" + info.get(LoggerHelper.effectiveLevel)));
            }

            // 如果存在配置信息，则添加配置行
            if (info.get(LoggerHelper.config) != null) {
                table.row(label(LoggerHelper.config).style(Decoration.bold.bold()),
                        label("" + info.get(LoggerHelper.config)));
            }

            // 添加追加性（additivity）和代码来源（codeSource）信息
            table.row(label(LoggerHelper.additivity).style(Decoration.bold.bold()),
                    label("" + info.get(LoggerHelper.additivity)))
                    .row(label(LoggerHelper.codeSource).style(Decoration.bold.bold()),
                            label("" + info.get(LoggerHelper.codeSource)));

            // 获取Appender列表，Appender是日志的输出目的地
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> appenders = (List<Map<String, Object>>) info.get(LoggerHelper.appenders);
            // 如果存在Appender，则逐个渲染其信息
            if (appenders != null && !appenders.isEmpty()) {

                for (Map<String, Object> appenderInfo : appenders) {
                    // 获取Appender的类信息
                    Class<?> appenderClass = (Class<?>) appenderInfo.get(LoggerHelper.clazz);

                    // 添加Appender的基本信息：名称、类名、类加载器、类加载器哈希值
                    appendersTable.row(label(LoggerHelper.name).style(Decoration.bold.bold()),
                            label("" + appenderInfo.get(LoggerHelper.name)));
                    appendersTable.row(label(LoggerHelper.clazz), label("" + appenderClass.getName()));
                    appendersTable.row(label(LoggerHelper.classLoader), label("" + info.get(LoggerHelper.classLoader)));
                    appendersTable.row(label(LoggerHelper.classLoaderHash),
                            label("" + info.get(LoggerHelper.classLoaderHash)));
                    // 如果配置了文件输出，则添加文件路径
                    if (appenderInfo.get(LoggerHelper.file) != null) {
                        appendersTable.row(label(LoggerHelper.file), label("" + appenderInfo.get(LoggerHelper.file)));
                    }
                    // 如果配置了输出目标，则添加目标信息
                    if (appenderInfo.get(LoggerHelper.target) != null) {
                        appendersTable.row(label(LoggerHelper.target),
                                label("" + appenderInfo.get(LoggerHelper.target)));
                    }
                    // 如果配置了阻塞策略，则添加阻塞信息
                    if (appenderInfo.get(LoggerHelper.blocking) != null) {
                        appendersTable.row(label(LoggerHelper.blocking),
                                label("" + appenderInfo.get(LoggerHelper.blocking)));
                    }
                    // 如果存在Appender引用，则添加引用信息
                    if (appenderInfo.get(LoggerHelper.appenderRef) != null) {
                        appendersTable.row(label(LoggerHelper.appenderRef),
                                label("" + appenderInfo.get(LoggerHelper.appenderRef)));
                    }
                }

                // 将Appender表格作为主表格的一行添加
                table.row(label("appenders").style(Decoration.bold.bold()), appendersTable);
            }

            // 渲染表格并添加到结果字符串
            sb.append(RenderUtil.render(table, width)).append('\n');
        }
        return sb.toString();
    }
}
