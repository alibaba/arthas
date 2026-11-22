package com.taobao.arthas.core.command.monitor200;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

@Name("jcmd")
@Summary("Execute JVM diagnostic commands through DiagnosticCommandMBean.")
@Description(Constants.EXAMPLE + "  jcmd\n"
        + "  jcmd help VM.flags\n"
        + "  jcmd VM.version\n"
        + "  jcmd VM.flags -- -all\n"
        + "  jcmd Thread.print -- -l\n"
        + Constants.WIKI + Constants.WIKI_HOME + "jcmd")
public class JcmdCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(JcmdCommand.class);
    private static final String DIAGNOSTIC_COMMAND = "com.sun.management:type=DiagnosticCommand";

    private String command;
    private List<String> arguments;

    @Argument(index = 0, argName = "command", required = false)
    @Description("Diagnostic command name (defaults to help)")
    public void setCommand(String command) {
        this.command = command;
    }

    @Argument(index = 1, argName = "arguments", required = false)
    @Description("Diagnostic command arguments; use -- before options starting with -")
    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    @Override
    public void process(CommandProcess process) {
        String commandName = command == null ? "help" : command;
        String[] commandArguments = arguments == null ? new String[0] : arguments.toArray(new String[0]);
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName = new ObjectName(DIAGNOSTIC_COMMAND);
            for (MBeanOperationInfo operation : server.getMBeanInfo(objectName).getOperations()) {
                if (!commandName.equals(operation.getDescriptor().getFieldValue("dcmd.name"))) {
                    continue;
                }

                Object result;
                if (operation.getSignature().length == 0) {
                    if (commandArguments.length != 0) {
                        process.end(-1, "Diagnostic command " + commandName + " does not accept arguments.");
                        return;
                    }
                    result = server.invoke(objectName, operation.getName(), new Object[0], new String[0]);
                } else {
                    result = server.invoke(objectName, operation.getName(), new Object[] { commandArguments },
                            new String[] { String[].class.getName() });
                }
                process.appendResult(new MessageModel((String) result));
                process.end();
                return;
            }
            process.end(-1, "Unknown diagnostic command: " + commandName + ". Run 'jcmd' to list available commands.");
        } catch (InstanceNotFoundException e) {
            process.end(-1, "DiagnosticCommand MBean is not available in this JVM.");
        } catch (Throwable t) {
            logger.error("Error executing diagnostic command {}", commandName, t);
            Throwable cause = t;
            if (t instanceof MBeanException) {
                cause = ((MBeanException) t).getTargetException();
            } else if (t instanceof RuntimeMBeanException) {
                cause = ((RuntimeMBeanException) t).getTargetException();
            }
            process.end(-1, "Error executing diagnostic command " + commandName + ": " + cause.getMessage());
        }
    }

    @Override
    public void complete(Completion completion) {
        if (CompletionUtils.detectArgumentIndex(completion) == 1) {
            try {
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                ObjectName objectName = new ObjectName(DIAGNOSTIC_COMMAND);
                List<String> names = new ArrayList<String>();
                for (MBeanOperationInfo operation : server.getMBeanInfo(objectName).getOperations()) {
                    names.add((String) operation.getDescriptor().getFieldValue("dcmd.name"));
                }
                CompletionUtils.complete(completion, names);
                return;
            } catch (Exception e) {
                logger.debug("Unable to complete diagnostic commands", e);
            }
        }
        super.complete(completion);
    }
}
