package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ProfilerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/4/27
 */
public class ProfilerView extends ResultView<ProfilerModel> {
    @Override
    public void draw(CommandProcess process, ProfilerModel model) {
        Object result = model.getResult();
        if (result != null) {
            process.write(result +"");
            if (result instanceof String) {
                String str = (String) result;
                if (!str.endsWith("\n")) {
                    process.write("\n");
                }
            } else {
                process.write("\n");
            }
        }
    }
}
