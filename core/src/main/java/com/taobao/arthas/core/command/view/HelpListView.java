package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.HelpListModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.CLI;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.List;

import static com.taobao.text.ui.Element.label;
import static com.taobao.text.ui.Element.row;

/**
 * @author gongdewei 2020/4/3
 */
public class HelpListView extends ResultView<HelpListModel> {

    @Override
    public void draw(CommandProcess process, HelpListModel result) {
        String message = RenderUtil.render(mainHelp(result.clis()), process.width());
        process.write(message);
    }

    private static Element mainHelp(List<CLI> commands) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(new LabelElement("NAME").style(Style.style(Decoration.bold)), new LabelElement("DESCRIPTION"));
        for (CLI cli : commands) {
            // com.taobao.arthas.core.shell.impl.BuiltinCommandResolver doesn't have CLI instance
            if (cli == null || cli.isHidden()) {
                continue;
            }
            table.add(row().add(label(cli.getName()).style(Style.style(Color.green))).add(label(cli.getSummary())));
        }
        return table;
    }

}
