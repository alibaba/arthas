package com.taobao.arthas.core.command.model;

import com.taobao.middleware.cli.CLI;

/**
 * @author gongdewei 2020/4/3
 */
public class HelpDetailModel extends ResultModel {
    private CommandVO command;
    private transient CLI cli;

    public HelpDetailModel() {
    }

    public HelpDetailModel(CommandVO command, CLI cli) {
        this.command = command;
        this.cli = cli;
    }

    public CommandVO getCommand() {
        return command;
    }

    //no serialization
    public CLI cli() {
        return cli;
    }

    @Override
    public String getType() {
        return "help_detail";
    }
}
