package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.RedefineModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 类重定义视图类
 *
 * 用于显示类热重定义（Hot Redefine）操作的结果。
类重定义允许在不重启JVM的情况下重新加载已修改的类。
该视图支持两种模式：
1. 显示匹配的类加载器列表
2. 显示成功重定义的类信息
 *
 * @author gongdewei 2020/4/16
 */
public class RedefineView extends ResultView<RedefineModel> {

    /**
     * 绘制类重定义结果到命令行界面
     *
     * 根据模型对象的内容，有两种输出模式：
     * 1. 如果存在匹配的类加载器列表，则显示类加载器信息
     * 2. 否则，显示成功重定义的类的数量和类名列表
     *
     * @param process 命令进程对象，用于与用户交互和输出内容
     * @param result 重定义模型对象，包含重定义结果或匹配的类加载器信息
     */
    @Override
    public void draw(CommandProcess process, RedefineModel result) {
        // 如果存在匹配的类加载器列表，则显示类加载器信息
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            // 使用ClassLoaderView绘制类加载器列表
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }

        // 构建重定义成功的类列表字符串
        StringBuilder sb = new StringBuilder();
        for (String aClass : result.getRedefinedClasses()) {
            sb.append(aClass).append("\n");
        }

        // 输出重定义成功信息
        // 包含重定义的类数量和所有类的名称列表
        process.write("redefine success, size: " + result.getRedefinitionCount())
                .write(", classes:\n")
                .write(sb.toString());
    }

}
