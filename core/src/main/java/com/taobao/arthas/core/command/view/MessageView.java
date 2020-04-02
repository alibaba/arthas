package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.result.MessageResult;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/4/2
 */
public class MessageView extends ResultView<MessageResult> {
    @Override
    public void draw(CommandProcess process, MessageResult result) {
        writeln(process, result.getMessage());
    }
}
