package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ClassMatchesModel;
import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.Collection;

import static com.taobao.text.ui.Element.label;

/**
 * @author gongdewei 2020/4/20
 */
public class ClassMatchesView extends ResultView<ClassMatchesModel> {

    @Override
    public void draw(CommandProcess process, ClassMatchesModel result) {
        Collection<ClassVO> matchedClasses = result.getMatchedClasses();

        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(new LabelElement("NAME").style(Decoration.bold.bold()),
                new LabelElement("HASHCODE").style(Decoration.bold.bold()),
                new LabelElement("CLASSLOADER").style(Decoration.bold.bold()));

        for (ClassVO c : matchedClasses) {
            table.row(label(c.getName()),
                    label(c.getClassLoaderHash()).style(Decoration.bold.fg(Color.red)),
                    TypeRenderUtils.drawClassLoader(c));
        }

        process.write(RenderUtil.render(table, process.width()) + "\n");
    }

}
