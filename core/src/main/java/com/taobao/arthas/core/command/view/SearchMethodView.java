package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SearchMethodModel;
import com.taobao.arthas.core.command.model.MethodVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.text.util.RenderUtil;


/**
 * render for SearchMethodCommand
 * @author gongdewei 2020/4/9
 */
public class SearchMethodView extends ResultView<SearchMethodModel> {
    @Override
    public void draw(CommandProcess process, SearchMethodModel result) {
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }

        boolean detail = result.isDetail();
        MethodVO methodInfo = result.getMethodInfo();

        if (detail) {
            if (methodInfo.isConstructor()) {
                //render constructor
                process.write(RenderUtil.render(ClassUtils.renderConstructor(methodInfo), process.width()) + "\n");
            } else {
                //render method
                process.write(RenderUtil.render(ClassUtils.renderMethod(methodInfo), process.width()) + "\n");
            }
        } else {
            //java.util.List indexOf(Ljava/lang/Object;)I
            //className methodName+Descriptor
            process.write(methodInfo.getDeclaringClass())
                    .write(" ")
                    .write(methodInfo.getMethodName())
                    .write(methodInfo.getDescriptor())
                    .write("\n");
        }

    }
}
