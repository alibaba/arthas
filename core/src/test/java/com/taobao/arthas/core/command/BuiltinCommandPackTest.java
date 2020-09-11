package com.taobao.arthas.core.command;

import com.taobao.arthas.core.shell.command.Command;

import java.util.List;

/**
 * @author: junjiexun
 * @date: 2020/9/10 11:53 下午
 * @description:
 */
public class BuiltinCommandPackTest {

    public static void main(String[] args) {
        BuiltinCommandPack builtinCommandPack = new BuiltinCommandPack();
        List<Command> commands = builtinCommandPack.commands();
        System.out.println(commands.size());
    }
}
