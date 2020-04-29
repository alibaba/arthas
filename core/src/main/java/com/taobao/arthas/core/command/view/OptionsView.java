package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ChangeResultVO;
import com.taobao.arthas.core.command.model.OptionVO;
import com.taobao.arthas.core.command.model.OptionsModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.Collection;

import static com.taobao.text.ui.Element.label;

/**
 * @author gongdewei 2020/4/15
 */
public class OptionsView extends ResultView<OptionsModel> {
    @Override
    public void draw(CommandProcess process, OptionsModel result) {
        if (result.getOptions() != null) {
            process.write(RenderUtil.render(drawShowTable(result.getOptions()), process.width()));
        } else if (result.getChangeResult() != null) {
            drawChangeResult(process, result.getChangeResult());
        }
    }

    private Element drawShowTable(Collection<OptionVO> options) {
        TableElement table = new TableElement(1, 1, 2, 1, 3, 6)
                .leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("LEVEL").style(Decoration.bold.bold()),
                label("TYPE").style(Decoration.bold.bold()),
                label("NAME").style(Decoration.bold.bold()),
                label("VALUE").style(Decoration.bold.bold()),
                label("SUMMARY").style(Decoration.bold.bold()),
                label("DESCRIPTION").style(Decoration.bold.bold()));

        for (final OptionVO optionVO : options) {
            table.row("" + optionVO.getLevel(),
                    optionVO.getType(),
                    optionVO.getName(),
                    optionVO.getValue(),
                    optionVO.getSummary(),
                    optionVO.getDescription());
        }
        return table;
    }

    public static void drawChangeResult(CommandProcess process, ChangeResultVO result) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("NAME").style(Decoration.bold.bold()),
                label("BEFORE-VALUE").style(Decoration.bold.bold()),
                label("AFTER-VALUE").style(Decoration.bold.bold()));
        table.row(result.getName(), StringUtils.objectToString(result.getBeforeValue()),
                StringUtils.objectToString(result.getAfterValue()));
        process.write(RenderUtil.render(table, process.width()));
    }

}
