package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ChangeResultModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.text.ui.Element.label;

/**
 * @author gongdewei 2020/4/16
 */
public class ChangeResultView extends ResultView<ChangeResultModel> {

    @Override
    public void draw(CommandProcess process, ChangeResultModel result) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("NAME").style(Decoration.bold.bold()),
                label("BEFORE-VALUE").style(Decoration.bold.bold()),
                label("AFTER-VALUE").style(Decoration.bold.bold()));
        table.row(result.getName(), StringUtils.objectToString(result.getBeforeValue()),
                StringUtils.objectToString(result.getAfterValue()));
        process.write(RenderUtil.render(table, process.width()));
    }

}
