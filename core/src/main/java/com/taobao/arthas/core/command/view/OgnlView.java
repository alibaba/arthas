package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.OgnlModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * @author gongdewei 2020/4/29
 */
public class OgnlView extends ResultView<OgnlModel> {
    @Override
    public void draw(CommandProcess process, OgnlModel result) {
        int expand = result.getExpand();
        Object value = result.getValue();
        String resultStr = StringUtils.objectToString(expand >= 0 ? new ObjectView(value, expand).draw() : value);
        process.write(resultStr).write("\n");
    }
}
