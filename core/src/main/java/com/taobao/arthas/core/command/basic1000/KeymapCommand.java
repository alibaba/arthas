package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.term.impl.Helper;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A command to display all the keymap for the specified connection.
 * @author ralf0131 2016-12-15 17:27.
 */
@Name("keymap")
@Summary("Display all the available keymap for the specified connection.")
public class KeymapCommand extends AnnotatedCommand {

    @Override
    public void process(CommandProcess process) {
        InputStream inputrc = Helper.loadInputRcFile();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputrc));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || "".equals(line.trim())) {
                    continue;
                }
                sb.append(line + "\n");
            }
        } catch (Exception e) {
            sb.append(e.getMessage());
        }
        process.write(sb.toString());
        process.end();
    }
}
