package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SqlProfilerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;

/**
 * Term view for SqlProfilerModel
 *
 * @author yangxiaobing 2021/8/4
 */
public class SqlProfilerView extends ResultView<SqlProfilerModel> {

    @Override
    public void draw(CommandProcess process, SqlProfilerModel model) {
        process.write(String.format("ts=%s; [method=%s.%s] [cost=%sms]\n" +
                        "sql: %s\n" +
                        "args: %s\n",
                DateUtils.formatDate(model.getTs()),
                model.getClassName(),
                model.getMethodName(),
                model.getCost(),
                model.getSql(),
                model.getParams() == null ? "" : StringUtils.join(model.getParams().toArray(), ",")));
    }
}
