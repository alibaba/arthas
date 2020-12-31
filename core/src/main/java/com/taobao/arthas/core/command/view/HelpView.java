package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.CommandVO;
import com.taobao.arthas.core.command.model.HelpModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.usage.StyledUsageFormatter;
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
public class HelpView extends ResultView<HelpModel> {

    @Override
    public void draw(CommandProcess process, HelpModel result) {
        if (result.getCommands() != null) {
            String message = RenderUtil.render(mainHelp(result.getCommands()), process.width());
            process.write(message);
        } else if (result.getDetailCommand() != null) {
            process.write(commandHelp(result.getDetailCommand().cli(), process.width()));
        }
    }

    private static Element mainHelp(List<CommandVO> commands) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(new LabelElement("NAME").style(Style.style(Decoration.bold)), new LabelElement("DESCRIPTION"));
        for (CommandVO commandVO : commands) {
            table.add(row().add(label(commandVO.getName()).style(Style.style(Color.green))).add(label(commandVO.getSummary())));
        }
        return table;
    }

    private static String commandHelp(CLI command, int width) {
        return StyledUsageFormatter.styledUsage(command, width);
    }
}
