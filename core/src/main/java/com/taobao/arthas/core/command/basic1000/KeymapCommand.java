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
 * 键盘映射命令类
 * 用于显示所有可用的键盘快捷键映射
 *
 * @author ralf0131 2016-12-15 17:27.
 * @author hengyunabc 2019-01-18
 */
@Name("keymap")
@Summary("Display all the available keymap for the specified connection.")
@Description(Constants.WIKI + Constants.WIKI_HOME + "keymap")
public class KeymapCommand extends AnnotatedCommand {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(KeymapCommand.class);

    @Override
    public void process(CommandProcess process) {
        // 只在tty会话中支持此命令
        if (!process.session().isTty()) {
            process.end(-1, "Command 'keymap' is only support tty session.");
            return;
        }

        // 加载inputrc配置文件
        InputStream inputrc = Helper.loadInputRcFile();
        try {
            // 创建表格，包含3列：快捷键、描述、名称
            TableElement table = new TableElement(1, 1, 2).leftCellPadding(1).rightCellPadding(1);
            // 添加表头
            table.row(true, label("Shortcut").style(Decoration.bold.bold()),
                            label("Description").style(Decoration.bold.bold()),
                            label("Name").style(Decoration.bold.bold()));

            // 逐行读取配置文件
            BufferedReader br = new BufferedReader(new InputStreamReader(inputrc));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // 跳过注释行和空行
                if (line.startsWith("#") || "".equals(line)) {
                    continue;
                }
                // 按冒号分割，获取快捷键和名称
                String[] strings = line.split(":");
                if (strings.length == 2) {
                    // 翻译快捷键并添加到表格
                    table.row(strings[0], translate(strings[0]), strings[1]);
                } else {
                    // 格式不正确的行也添加到表格
                    table.row(line);
                }

            }
            // 渲染表格并输出
            process.write(RenderUtil.render(table, process.width()));
        } catch (IOException e) {
            logger.error("read inputrc file error.", e);
        } finally {
            // 关闭输入流并结束处理
            IOUtils.close(inputrc);
            process.end();
        }
    }

    /**
     * 翻译快捷键为可读格式
     * @param key 快捷键字符串
     * @return 可读的快捷键描述
     */
    private String translate(String key) {
        // 处理Ctrl组合键：格式为 "\C-x"
        if (key.length() == 6 && key.startsWith("\"\\C-") && key.endsWith("\"")) {
            char ch = key.charAt(4);
            if ((ch >= 'a' && ch <= 'z') || ch == '?') {
                return "Ctrl + " + ch;
            }
        }

        // 处理方向键
        if (key.equals("\"\\e[D\"")) {
            return "Left arrow";
        } else if (key.equals("\"\\e[C\"")) {
            return "Right arrow";
        } else if (key.equals("\"\\e[B\"")) {
            return "Down arrow";
        } else if (key.equals("\"\\e[A\"")) {
            return "Up arrow";
        }

        // 其他情况返回原始字符串
        return key;
    }
}
