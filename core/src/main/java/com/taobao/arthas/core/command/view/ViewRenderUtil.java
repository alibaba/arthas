package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ChangeResultVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.Map;

import static com.taobao.text.ui.Element.label;

/**
 * view render util for term/tty
 * @author gongdewei 2020/6/22
 */
public class ViewRenderUtil {

    /**
     * Render key-value table
     * @param map
     * @param width
     * @return
     */
    public static String renderKeyValueTable(Map<String, String> map, int width) {
        TableElement table = new TableElement(1, 4).leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("KEY").style(Decoration.bold.bold()), label("VALUE").style(Decoration.bold.bold()));

        for (Map.Entry<String, String> entry : map.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }

        return RenderUtil.render(table, width);
    }

    /**
     * Render change result vo
     * @param process
     * @param result
     */
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
