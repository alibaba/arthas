package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.DumpClassModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.List;

import static com.taobao.text.ui.Element.label;

/**
 * @author gongdewei 2020/4/21
 */
public class DumpClassView extends ResultView<DumpClassModel> {

    @Override
    public void draw(CommandProcess process, DumpClassModel result) {
        List<ClassVO> classFiles = result.getClassFiles();

        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(new LabelElement("HASHCODE").style(Decoration.bold.bold()),
                new LabelElement("CLASSLOADER").style(Decoration.bold.bold()),
                new LabelElement("LOCATION").style(Decoration.bold.bold()));

        for (ClassVO clazz : classFiles) {
            table.row(label(clazz.getClassLoaderHash()).style(Decoration.bold.fg(Color.red)),
                    TypeRenderUtils.drawClassLoader(clazz),
                    label(clazz.getLocation()).style(Decoration.bold.fg(Color.red)));
        }

        process.write(RenderUtil.render(table, process.width()))
                .write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
    }

}
