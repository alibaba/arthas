package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.HelpDetailModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.usage.StyledUsageFormatter;
import com.taobao.middleware.cli.CLI;

/**
 * @author gongdewei 2020/4/3
 */
public class HelpDetailView extends ResultView<HelpDetailModel> {

    @Override
    public void draw(CommandProcess process, HelpDetailModel result) {
        String message = commandHelp(result.cli(), process.width());
        process.write(message);
    }

    private static String commandHelp(CLI command, int width) {
        return StyledUsageFormatter.styledUsage(command, width);
    }
}
