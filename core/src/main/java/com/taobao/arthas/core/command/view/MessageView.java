package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/4/2
 */
public class MessageView extends ResultView<MessageModel> {
    @Override
    public void draw(CommandProcess process, MessageModel result) {
        writeln(process, result.getMessage());
    }
}
