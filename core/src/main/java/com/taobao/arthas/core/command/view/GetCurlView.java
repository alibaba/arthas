package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.monitor200.curl.GetCurlModel;
import com.taobao.arthas.core.command.view.ResultView;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;

/**
 * @author zhaoyuening
 */
public class GetCurlView extends ResultView<GetCurlModel> {
    @Override
    public void draw(CommandProcess process, GetCurlModel model) {
        process.write("ts=" + DateUtils.formatDate(model.getTs()) + "\n");
        process.write("method=" + model.getClassName() + "." + model.getMethodName() + "\n");
        process.write("curl=[" + model.getCurl() + " ]\n\n");
    }
}
