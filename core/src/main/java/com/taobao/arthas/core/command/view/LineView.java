package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.LineModel;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * Term view for LineModel
 *
 */
public class LineView extends ResultView<LineModel> {

    @Override
    public void draw(CommandProcess process, LineModel model) {
        ObjectVO objectVO = model.getValue();
        String result = StringUtils.objectToString(
                objectVO.needExpand() ? new ObjectView(model.getSizeLimit(), objectVO).draw() : objectVO.getObject());
        process.write("method=" + model.getClassName() + "." + model.getMethodName() + " line=" + model.getAccessPoint() + "\n");
        process.write("ts=" + DateUtils.formatDateTime(model.getTs()) + "; result=" + result + "\n");
    }
}
