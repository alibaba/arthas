package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ProfilerModel;
import com.taobao.arthas.core.command.monitor200.ProfilerCommand.ProfilerAction;
import com.taobao.arthas.core.shell.command.CommandProcess;


/**
 * Term view for ProfilerModel
 *
 * @author gongdewei 2020/4/27
 */
public class ProfilerView extends ResultView<ProfilerModel> {
    @Override
    public void draw(CommandProcess process, ProfilerModel model) {
        if (model.getSupportedActions() != null) {
            process.write("Supported Actions: " + model.getSupportedActions()).write("\n");
            return;
        }

        drawExecuteResult(process, model);

        if (ProfilerAction.start.name().equals(model.getAction())) {
            if (model.getDuration() != null) {
                process.write(String.format("profiler will silent stop after %d seconds.\n", model.getDuration().longValue()));
                process.write("profiler output file will be: " + model.getOutputFile() + "\n");
            }
        } else if (ProfilerAction.stop.name().equals(model.getAction())) {
            process.write("profiler output file: " + model.getOutputFile() + "\n");
        }

    }

    private void drawExecuteResult(CommandProcess process, ProfilerModel model) {
        if (model.getExecuteResult() != null) {
            process.write(model.getExecuteResult());
            if (!model.getExecuteResult().endsWith("\n")) {
                process.write("\n");
            }
        }
    }
}
