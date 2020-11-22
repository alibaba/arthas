package com.taobao.arthas.core.command.view;

import com.sun.management.VMOption;
import com.taobao.arthas.core.command.model.VMOptionModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.List;

import static com.taobao.text.ui.Element.label;

/**
 * @author gongdewei 2020/4/15
 */
public class VMOptionView extends ResultView<VMOptionModel> {

    @Override
    public void draw(CommandProcess process, VMOptionModel result) {
        if (result.getVmOptions() != null) {
            process.write(renderVMOptions(result.getVmOptions(), process.width()));
        } else if (result.getChangeResult() != null) {
            TableElement table = ViewRenderUtil.renderChangeResult(result.getChangeResult());
            process.write(RenderUtil.render(table, process.width()));
        }
    }

    private static String renderVMOptions(List<VMOption> diagnosticOptions, int width) {
        TableElement table = new TableElement(1, 1, 1, 1).leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("KEY").style(Decoration.bold.bold()),
                label("VALUE").style(Decoration.bold.bold()),
                label("ORIGIN").style(Decoration.bold.bold()),
                label("WRITEABLE").style(Decoration.bold.bold()));

        for (VMOption option : diagnosticOptions) {
            table.row(option.getName(), option.getValue(), "" + option.getOrigin(), "" + option.isWriteable());
        }

        return RenderUtil.render(table, width);
    }
}
