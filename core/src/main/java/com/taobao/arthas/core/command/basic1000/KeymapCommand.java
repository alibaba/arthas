package com.taobao.arthas.core.command.basic1000;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.term.impl.Helper;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.text.ui.Element.label;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A command to display all the keymap for the specified connection.
 *
 * @author ralf0131 2016-12-15 17:27.
 * @author hengyunabc 2019-01-18
 */
@Name("keymap")
@Summary("Display all the available keymap for the specified connection.")
@Description(Constants.WIKI + Constants.WIKI_HOME + "keymap")
public class KeymapCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(KeymapCommand.class);

    @Override
    public void process(CommandProcess process) {
        if (!process.session().isTty()) {
            process.end(-1, "Command 'keymap' is only support tty session.");
            return;
        }

        InputStream inputrc = Helper.loadInputRcFile();
        try {
            TableElement table = new TableElement(1, 1, 2).leftCellPadding(1).rightCellPadding(1);
            table.row(true, label("Shortcut").style(Decoration.bold.bold()),
                            label("Description").style(Decoration.bold.bold()),
                            label("Name").style(Decoration.bold.bold()));

            BufferedReader br = new BufferedReader(new InputStreamReader(inputrc));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || "".equals(line)) {
                    continue;
                }
                String[] strings = line.split(":");
                if (strings.length == 2) {
                    table.row(strings[0], translate(strings[0]), strings[1]);
                } else {
                    table.row(line);
                }

            }
            process.write(RenderUtil.render(table, process.width()));
        } catch (IOException e) {
            logger.error("read inputrc file error.", e);
        } finally {
            IOUtils.close(inputrc);
            process.end();
        }
    }

    private String translate(String key) {
        if (key.length() == 6 && key.startsWith("\"\\C-") && key.endsWith("\"")) {
            char ch = key.charAt(4);
            if ((ch >= 'a' && ch <= 'z') || ch == '?') {
                return "Ctrl + " + ch;
            }
        }

        if (key.equals("\"\\e[D\"")) {
            return "Left arrow";
        } else if (key.equals("\"\\e[C\"")) {
            return "Right arrow";
        } else if (key.equals("\"\\e[B\"")) {
            return "Down arrow";
        } else if (key.equals("\"\\e[A\"")) {
            return "Up arrow";
        }

        return key;
    }
}
