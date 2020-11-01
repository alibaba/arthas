package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.GetStaticModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.object.ObjectExpandUtils;
import com.taobao.text.ui.Element;
import com.taobao.text.util.RenderUtil;

/**
 * @author gongdewei 2020/4/20
 */
public class GetStaticView extends ResultView<GetStaticModel> {

    @Override
    public void draw(CommandProcess process, GetStaticModel result) {
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }
        if (result.getFieldName() != null) {
            String valueStr = ObjectExpandUtils.toString(result.getFieldValue());
            process.write("field: " + result.getFieldName() + "\n" + valueStr + "\n");
        } else if (result.getMatchedClasses() != null) {
            Element table = ClassUtils.renderMatchedClasses(result.getMatchedClasses());
            process.write(RenderUtil.render(table)).write("\n");
        }
    }
}
