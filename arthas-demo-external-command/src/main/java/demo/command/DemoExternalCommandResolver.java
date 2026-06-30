package demo.command;

import java.util.Collections;
import java.util.List;

import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandResolver;

public class DemoExternalCommandResolver implements CommandResolver {

    @Override
    public List<Command> commands() {
        return Collections.singletonList(Command.create(DemoExternalCommand.class));
    }
}
