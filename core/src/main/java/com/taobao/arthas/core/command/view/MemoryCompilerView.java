package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MemoryCompilerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/4/20
 */
public class MemoryCompilerView extends ResultView<MemoryCompilerModel> {
    @Override
    public void draw(CommandProcess process, MemoryCompilerModel result) {
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }
        process.write("Memory compiler output:\n");
        for (String file : result.getFiles()) {
            process.write(file + '\n');
        }
    }
}
