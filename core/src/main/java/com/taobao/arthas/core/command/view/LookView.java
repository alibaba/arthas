package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.LookModel;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * Term view for LookModel
 *
 */
public class LookView extends ResultView<LookModel> {

    @Override
    public void draw(CommandProcess process, LookModel model) {
        ObjectVO objectVO = model.getValue();
        String result = StringUtils.objectToString(
                objectVO.needExpand() ? new ObjectView(model.getSizeLimit(), objectVO).draw() : objectVO.getObject());
        process.write("method=" + model.getClassName() + "." + model.getMethodName() + " location=" + model.getAccessPoint() + "\n");
        process.write("ts=" + DateUtils.formatDate(model.getTs()) + "; result=" + result + "\n");
    }
}
