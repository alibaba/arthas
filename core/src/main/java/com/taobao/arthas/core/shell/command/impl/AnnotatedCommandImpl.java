package com.taobao.arthas.core.shell.command.impl;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.Command;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.util.UserStatUtil;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.annotations.CLIConfigurator;

import java.util.Collections;

/**
 * @author beiwei30 on 10/11/2016.
 */
public class AnnotatedCommandImpl extends Command {

    private CLI cli;
    private Class<? extends AnnotatedCommand> clazz;
    private Handler<CommandProcess> processHandler = new ProcessHandler();

    public AnnotatedCommandImpl(Class<? extends AnnotatedCommand> clazz) {
        this.clazz = clazz;
        cli = CLIConfigurator.define(clazz, true);
        cli.addOption(new Option().setArgName("help").setFlag(true).setShortName("h").setLongName("help")
                .setDescription("this help").setHelp(true));
    }

    private boolean shouldOverridesName(Class<? extends AnnotatedCommand> clazz) {
        try {
            clazz.getDeclaredMethod("name");
            return true;
        } catch (NoSuchMethodException ignore) {
            return false;
        }
    }

    private boolean shouldOverrideCli(Class<? extends AnnotatedCommand> clazz) {
        try {
            clazz.getDeclaredMethod("cli");
            return true;
        } catch (NoSuchMethodException ignore) {
            return false;
        }
    }

    @Override
    public String name() {
        if (shouldOverridesName(clazz)) {
            try {
                return clazz.newInstance().name();
            } catch (Exception ignore) {
                // Use cli.getName() instead
            }
        }
        return cli.getName();
    }

    @Override
    public CLI cli() {
        if (shouldOverrideCli(clazz)) {
            try {
                return clazz.newInstance().cli();
            } catch (Exception ignore) {
                // Use cli instead
            }
        }
        return cli;
    }

    private void process(CommandProcess process) {
        AnnotatedCommand instance;
        try {
            instance = clazz.newInstance();
        } catch (Exception e) {
            process.end();
            return;
        }
        CLIConfigurator.inject(process.commandLine(), instance);
        instance.process(process);
        UserStatUtil.arthasUsageSuccess(name(), process.args());
    }

    @Override
    public Handler<CommandProcess> processHandler() {
        return processHandler;
    }

    @Override
    public void complete(final Completion completion) {
        final AnnotatedCommand instance;
        try {
            instance = clazz.newInstance();
        } catch (Exception e) {
            super.complete(completion);
            return;
        }

        try {
            instance.complete(completion);
        } catch (Throwable t) {
            completion.complete(Collections.<String>emptyList());
        }
    }

    private class ProcessHandler implements Handler<CommandProcess> {
        @Override
        public void handle(CommandProcess process) {
            process(process);
        }
    }

}
