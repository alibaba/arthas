package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MethodModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.text.util.RenderUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static java.lang.String.format;

/**
 * Method info render for SearchMethodCommand
 * @author gongdewei 2020/4/9
 */
public class MethodView extends ResultView<MethodModel> {
    @Override
    public void draw(CommandProcess process, MethodModel result) {
        Class clazz = result.clazz();
        if (result.constructor() != null) {
            //render constructor
            Constructor constructor = result.constructor();
            if (result.detail()) {
                process.write(RenderUtil.render(ClassUtils.renderConstructor(constructor, clazz), process.width()) + "\n");
            } else {
                String methodNameWithDescriptor = org.objectweb.asm.commons.Method.getMethod(constructor).toString();
                String line = format("%s %s%n", clazz.getName(), methodNameWithDescriptor);
                process.write(line);
            }
        } else {
            //render method
            Method method = result.method();
            if (result.detail()) {
                process.write(RenderUtil.render(ClassUtils.renderMethod(method, clazz), process.width()) + "\n");
            } else {
                String methodNameWithDescriptor = org.objectweb.asm.commons.Method.getMethod(method).toString();
                String line = format("%s %s%n", clazz.getName(), methodNameWithDescriptor);
                process.write(line);
            }
        }
    }
}
