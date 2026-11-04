package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gongdewei 2020/4/3
 */
public class HelpModel extends ResultModel {

    //list
    private List<CommandVO> commands;

    //details
    private CommandVO detailCommand;

    public HelpModel() {
    }

    public HelpModel(List<CommandVO> commands) {
        this.commands = commands;
    }

    public HelpModel(CommandVO command) {
        this.detailCommand = command;
    }

    public void addCommandVO(CommandVO commandVO){
        if (commands == null) {
            commands = new ArrayList<CommandVO>();
        }
        this.commands.add(commandVO);
    }

    public List<CommandVO> getCommands() {
        return commands;
    }

    public void setCommands(List<CommandVO> commands) {
        this.commands = commands;
    }

    public CommandVO getDetailCommand() {
        return detailCommand;
    }

    @Override
    public String getType() {
        return "help";
    }
}
