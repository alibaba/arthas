package com.taobao.arthas.core.command;

import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.util.matcher.AnnotatedCommandSubClassMatcher;
import com.taobao.arthas.core.util.reflect.ArthasReflectUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author beiwei30 on 17/11/2016.
 */
public class BuiltinCommandPack implements CommandResolver {

    private static List<Command> commands = new ArrayList<Command>();

    static {
        try {
            initCommands();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public List<Command> commands() {
        return commands;
    }

    private static void initCommands() throws IOException {
        for (Class commandClazz : ArthasReflectUtils.getClassSetFromPackage(BuiltinCommandPack.class.getPackage().getName(), new AnnotatedCommandSubClassMatcher())) {
            try {
                commands.add(Command.create(commandClazz));
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
