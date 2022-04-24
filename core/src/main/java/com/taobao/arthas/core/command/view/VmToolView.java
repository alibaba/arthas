package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.VmToolModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * 
 * @author hengyunabc 2022-04-24
 *
 */
public class VmToolView extends ResultView<VmToolModel> {
    @Override
    public void draw(CommandProcess process, VmToolModel model) {
        if (model.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, model.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }

        int expand = model.getExpand();
        Object value = model.getValue();
        String resultStr = StringUtils.objectToString(expand >= 0 ? new ObjectView(value, expand).draw() : value);
        process.write(resultStr).write("\n");
    }
}
