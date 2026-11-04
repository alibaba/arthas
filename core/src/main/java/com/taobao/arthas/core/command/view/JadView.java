package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.JadModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.lang.LangRenderUtil;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.util.RenderUtil;

/**
 * @author gongdewei 2020/4/22
 */
public class JadView extends ResultView<JadModel> {

    @Override
    public void draw(CommandProcess process, JadModel result) {
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }

        int width = process.width();
        if (result.getMatchedClasses() != null) {
            Element table = ClassUtils.renderMatchedClasses(result.getMatchedClasses());
            process.write(RenderUtil.render(table, width)).write("\n");
        } else {
            ClassVO classInfo = result.getClassInfo();
            if (classInfo != null) {
                process.write("\n");
                process.write(RenderUtil.render(new LabelElement("ClassLoader: ").style(Decoration.bold.fg(Color.red)), width));
                process.write(RenderUtil.render(TypeRenderUtils.drawClassLoader(classInfo), width) + "\n");
            }
            if (result.getLocation() != null) {
                process.write(RenderUtil.render(new LabelElement("Location: ").style(Decoration.bold.fg(Color.red)), width));
                process.write(RenderUtil.render(new LabelElement(result.getLocation()).style(Decoration.bold.fg(Color.blue)), width) + "\n");
            }
            process.write(LangRenderUtil.render(result.getSource()) + "\n");
            process.write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
        }
    }

}
