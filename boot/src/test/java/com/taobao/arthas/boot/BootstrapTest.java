package com.taobao.arthas.boot;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;

public class BootstrapTest {

    @Test
    public void testCommandLocationsOption() {
        CLI cli = CLIConfigurator.define(Bootstrap.class);
        CommandLine commandLine = cli.parse(Arrays.asList("--command-locations",
                        "/tmp/ext-command.jar,/tmp/ext-commands"));

        Bootstrap bootstrap = new Bootstrap();
        CLIConfigurator.inject(commandLine, bootstrap);

        assertEquals("/tmp/ext-command.jar,/tmp/ext-commands", bootstrap.getCommandLocations());
    }
}
