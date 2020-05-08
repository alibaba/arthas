package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MethodModel;
import com.taobao.arthas.core.command.model.MethodVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.text.util.RenderUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static java.lang.String.format;

/**
 * render for SearchMethodCommand
 * @author gongdewei 2020/4/9
 */
public class MethodView extends ResultView<MethodModel> {
    @Override
    public void draw(CommandProcess process, MethodModel result) {
        boolean detail = result.isDetail();
        MethodVO methodInfo = result.getMethodInfo();
        if (methodInfo.isConstructor()) {
            //render constructor
            if (detail) {
                process.write(RenderUtil.render(ClassUtils.renderConstructor(methodInfo), process.width()) + "\n");
            } else {
                //className methodNameWithDescriptor
                String line = format("%s %s%n", methodInfo.getDeclaringClass(), methodInfo.getDescriptor());
                process.write(line);
            }
        } else {
            //render method
            if (detail) {
                process.write(RenderUtil.render(ClassUtils.renderMethod(methodInfo), process.width()) + "\n");
            } else {
                String line = format("%s %s%n", methodInfo.getDeclaringClass(), methodInfo.getDescriptor());
                process.write(line);
            }
        }
    }
}
