package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.logger.LoggerHelper;
import com.taobao.arthas.core.command.model.LoggerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.List;
import java.util.Map;

import static com.taobao.text.ui.Element.label;

/**
 * View of 'logger' command
 *
 * @author gongdewei 2020/4/22
 */
public class LoggerView extends ResultView<LoggerModel> {

    @Override
    public void draw(CommandProcess process, LoggerModel result) {
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }
        process.write(renderLoggerInfo(result.getLoggerInfoMap(), process.width()));
    }

    private String renderLoggerInfo(Map<String, Map<String, Object>> loggerInfos, int width) {
        StringBuilder sb = new StringBuilder(8192);

        for (Map.Entry<String, Map<String, Object>> entry : loggerInfos.entrySet()) {
            Map<String, Object> info = entry.getValue();

            TableElement table = new TableElement(2, 10).leftCellPadding(1).rightCellPadding(1);
            TableElement appendersTable = new TableElement().rightCellPadding(1);

            Class<?> clazz = (Class<?>) info.get(LoggerHelper.clazz);
            table.row(label(LoggerHelper.name).style(Decoration.bold.bold()), label("" + info.get(LoggerHelper.name)))
                    .row(label(LoggerHelper.clazz).style(Decoration.bold.bold()), label("" + clazz.getName()))
                    .row(label(LoggerHelper.classLoader).style(Decoration.bold.bold()),
                            label("" + info.get(LoggerHelper.classLoader)))
                    .row(label(LoggerHelper.classLoaderHash).style(Decoration.bold.bold()),
                            label("" + info.get(LoggerHelper.classLoaderHash)))
                    .row(label(LoggerHelper.level).style(Decoration.bold.bold()),
                            label("" + info.get(LoggerHelper.level)));
            if (info.get(LoggerHelper.effectiveLevel) != null) {
                table.row(label(LoggerHelper.effectiveLevel).style(Decoration.bold.bold()),
                        label("" + info.get(LoggerHelper.effectiveLevel)));
            }

            if (info.get(LoggerHelper.config) != null) {
                table.row(label(LoggerHelper.config).style(Decoration.bold.bold()),
                        label("" + info.get(LoggerHelper.config)));
            }

            table.row(label(LoggerHelper.additivity).style(Decoration.bold.bold()),
                    label("" + info.get(LoggerHelper.additivity)))
                    .row(label(LoggerHelper.codeSource).style(Decoration.bold.bold()),
                            label("" + info.get(LoggerHelper.codeSource)));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> appenders = (List<Map<String, Object>>) info.get(LoggerHelper.appenders);
            if (appenders != null && !appenders.isEmpty()) {

                for (Map<String, Object> appenderInfo : appenders) {
                    Class<?> appenderClass = (Class<?>) appenderInfo.get(LoggerHelper.clazz);

                    appendersTable.row(label(LoggerHelper.name).style(Decoration.bold.bold()),
                            label("" + appenderInfo.get(LoggerHelper.name)));
                    appendersTable.row(label(LoggerHelper.clazz), label("" + appenderClass.getName()));
                    appendersTable.row(label(LoggerHelper.classLoader), label("" + info.get(LoggerHelper.classLoader)));
                    appendersTable.row(label(LoggerHelper.classLoaderHash),
                            label("" + info.get(LoggerHelper.classLoaderHash)));
                    if (appenderInfo.get(LoggerHelper.file) != null) {
                        appendersTable.row(label(LoggerHelper.file), label("" + appenderInfo.get(LoggerHelper.file)));
                    }
                    if (appenderInfo.get(LoggerHelper.target) != null) {
                        appendersTable.row(label(LoggerHelper.target),
                                label("" + appenderInfo.get(LoggerHelper.target)));
                    }
                    if (appenderInfo.get(LoggerHelper.blocking) != null) {
                        appendersTable.row(label(LoggerHelper.blocking),
                                label("" + appenderInfo.get(LoggerHelper.blocking)));
                    }
                    if (appenderInfo.get(LoggerHelper.appenderRef) != null) {
                        appendersTable.row(label(LoggerHelper.appenderRef),
                                label("" + appenderInfo.get(LoggerHelper.appenderRef)));
                    }
                }

                table.row(label("appenders").style(Decoration.bold.bold()), appendersTable);
            }

            sb.append(RenderUtil.render(table, width)).append('\n');
        }
        return sb.toString();
    }
}
