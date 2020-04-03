package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.shell.command.Command;
import com.taobao.middleware.cli.CLI;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gongdewei 2020/4/3
 */
public class HelpListModel extends ResultModel {

    private List<CommandVO> commands = new ArrayList<CommandVO>();
    private List<CLI> clis = new ArrayList<CLI>();

    public HelpListModel() {
    }

    public HelpListModel(List<CommandVO> commands) {
        this.commands = commands;
    }

    public void addCommandVO(CommandVO commandVO, CLI cli){
        this.commands.add(commandVO);
        this.clis.add(cli);
    }

    public List<CommandVO> getCommands() {
        return commands;
    }

    public void setCommands(List<CommandVO> commands) {
        this.commands = commands;
    }

    public List<CLI> clis() {
        return clis;
    }

    @Override
    public String getType() {
        return "help";
    }
}
