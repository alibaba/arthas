package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.result.PropertyResult;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.Map;

import static com.taobao.text.ui.Element.label;

/**
 * @author gongdewei 2020/4/2
 */
public class PropertyView extends ResultView<PropertyResult> {

    @Override
    public void draw(CommandProcess process, PropertyResult result) {
        process.write(renderEnv(result.getProps(), process.width()));
    }

    private String renderEnv(Map<String, String> envMap, int width) {
        TableElement table = new TableElement(1, 4).leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("KEY").style(Decoration.bold.bold()), label("VALUE").style(Decoration.bold.bold()));

        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }

        return RenderUtil.render(table, width);
    }
}
