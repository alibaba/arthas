package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.LineModel;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * Term view for LineModel
 */
public class LineView extends ResultView<LineModel> {

    @Override
    public void draw(CommandProcess process, LineModel model) {
        ObjectVO objectVO = model.getValue();
        int sizeLimit = ObjectView.normalizeMaxObjectLength(model.getSizeLimit());
        String result = StringUtils.objectToString(
                objectVO.needExpand() ? new ObjectView(sizeLimit, objectVO).draw() : objectVO.getObject());

        StringBuilder sb = new StringBuilder();
        sb.append("ts=").append(DateUtils.formatDateTime(model.getTs()))
                .append("; [thread=").append(model.getThreadName())
                .append("(").append(model.getThreadId()).append(")")
                .append(" cost=").append(model.getCost()).append("ms] ")
                .append(model.getClassName()).append(".").append(model.getMethodName())
                .append(model.getMethodDesc()).append(":").append(model.getLineNumber()).append("\n");
        sb.append("result=").append(result).append("\n");
        StackTraceElement[] stackTrace = model.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            sb.append("stack=\n");
            for (StackTraceElement stackTraceElement : stackTrace) {
                sb.append("    at ").append(stackTraceElement.getClassName()).append(".")
                        .append(stackTraceElement.getMethodName()).append("(")
                        .append(stackTraceElement.getFileName()).append(":")
                        .append(stackTraceElement.getLineNumber()).append(")\n");
            }
        }

        process.write(sb.toString());
    }
}
