package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SqlProfilerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;

import java.util.List;

/**
 * Term view for SqlProfilerModel
 *
 * @author yangxiaobing 2021/8/4
 */
public class SqlProfilerView extends ResultView<SqlProfilerModel> {

    @Override
    public void draw(CommandProcess process, SqlProfilerModel model) {
        StringBuilder outputBuilder = new StringBuilder(String.format("ts=%s; [method=%s.%s] [cost=%sms]\n" +
                        "sql: %s\n" +
                        "args: ",
                DateUtils.formatDate(model.getTs()),
                model.getClassName(),
                model.getMethodName(),
                model.getCost(),
                model.getSql()));
        if (model.getBatchParams() != null) {
            outputBuilder.append("\n");
            for (List<String> batchParam : model.getBatchParams()) {
                outputBuilder.append(String.format("--(%s)\n", StringUtils.join(batchParam.toArray(), ",")));
            }
        } else if (model.getParams() != null) {
            outputBuilder.append(StringUtils.join(model.getParams().toArray(), ","));
            outputBuilder.append("\n");
        } else {
            outputBuilder.append("\n");
        }

        process.write(outputBuilder.toString());
    }
}
