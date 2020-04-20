package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.GetStaticModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * @author gongdewei 2020/4/20
 */
public class GetStaticView extends ResultView<GetStaticModel> {

    @Override
    public void draw(CommandProcess process, GetStaticModel result) {
        int expand = result.expand();
        String valueStr = StringUtils.objectToString(expand >= 0 ? new ObjectView(result.getFieldValue(), expand).draw() : result.getFieldValue());
        process.write("field: " + result.getFieldName() + "\n" + valueStr + "\n");
    }
}
