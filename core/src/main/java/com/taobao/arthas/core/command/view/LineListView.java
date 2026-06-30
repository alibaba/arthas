package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.LineListModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Term view for LineListModel
 */
public class LineListView extends ResultView<LineListModel> {

    @Override
    public void draw(CommandProcess process, LineListModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("class=").append(model.getClassName());
        if (model.getSourceFile() != null) {
            sb.append(" source=").append(model.getSourceFile());
        }
        sb.append("\n");
        sb.append("method=").append(model.getMethodName()).append(model.getMethodDesc())
                .append(" lines=").append(model.getLines()).append("\n");
        process.write(sb.toString());
    }
}
