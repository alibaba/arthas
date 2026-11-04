package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.DumpClassModel;
import com.taobao.arthas.core.command.model.DumpClassVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
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
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }
        if (result.getDumpedClasses() != null) {
            drawDumpedClasses(process, result.getDumpedClasses());

        } else if (result.getMatchedClasses() != null) {
            Element table = ClassUtils.renderMatchedClasses(result.getMatchedClasses());
            process.write(RenderUtil.render(table)).write("\n");
        }
    }

    private void drawDumpedClasses(CommandProcess process, List<DumpClassVO> classVOs) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(new LabelElement("HASHCODE").style(Decoration.bold.bold()),
                new LabelElement("CLASSLOADER").style(Decoration.bold.bold()),
                new LabelElement("LOCATION").style(Decoration.bold.bold()));

        for (DumpClassVO clazz : classVOs) {
            table.row(label(clazz.getClassLoaderHash()).style(Decoration.bold.fg(Color.red)),
                    TypeRenderUtils.drawClassLoader(clazz),
                    label(clazz.getLocation()).style(Decoration.bold.fg(Color.red)));
        }

        process.write(RenderUtil.render(table, process.width()))
                .write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
    }

}
