package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MemoryCompilerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 内存编译器命令的视图类
 * 负责将内存编译的结果展示给用户
 *
 * @author gongdewei 2020/4/20
 */
public class MemoryCompilerView extends ResultView<MemoryCompilerModel> {
    /**
     * 绘制内存编译器命令的执行结果
     *
     * @param process 命令处理进程，用于输出结果
     * @param result 内存编译器命令的执行结果模型
     */
    @Override
    public void draw(CommandProcess process, MemoryCompilerModel result) {
        // 如果结果中包含匹配的类加载器信息，则显示类加载器列表
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }
        // 输出内存编译器输出的文件列表
        process.write("Memory compiler output:\n");
        for (String file : result.getFiles()) {
            process.write(file + '\n');
        }
    }
}
