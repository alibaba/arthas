package demo.command;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

@Name("demo-external")
@Summary("Demo external command loaded from arthas.home/commands")
@Description("Examples:\n"
        + "  demo-external\n"
        + "  demo-external Codex\n")
public class DemoExternalCommand extends AnnotatedCommand {

    private String message;

    @Argument(index = 0, argName = "message", required = false)
    @Description("message printed by the demo external command")
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void process(CommandProcess process) {
        String value = message;
        if (value == null || value.trim().isEmpty()) {
            value = "hello";
        }
        process.write("demo external command loaded: " + value + "\n");
        process.end();
    }
}
