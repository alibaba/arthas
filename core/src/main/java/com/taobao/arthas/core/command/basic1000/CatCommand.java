package com.taobao.arthas.core.command.basic1000;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.logger.Logger;

@Name("cat")
@Summary("Concatenate and print files")
public class CatCommand extends AnnotatedCommand {
    private static final Logger logger = LogUtil.getArthasLogger();
    private List<String> files;
    private String encoding;

    @Argument(argName = "files", index = 0)
    @Description("files")
    public void setFiles(List<String> files) {
        this.files = files;
    }

    @Option(longName = "encoding")
    @Description("File encoding")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public void process(CommandProcess process) {
        for (String file : files) {
            File f = new File(file);
            if (!f.exists()) {
                process.write("cat " + file + ": No such file or directory\n");
                process.end();
                return;
            }
            if (f.isDirectory()) {
                process.write("cat " + file + ": Is a directory\n");
                process.end();
                return;
            }
        }

        for (String file : files) {
            File f = new File(file);
            if (f.length() > 1024 * 1024 * 8) {
                process.write("cat " + file + ": Is to large, size: " + f.length() + '\n');
                process.end();
                return;
            }
            try {
                String fileToString = FileUtils.readFileToString(f,
                                encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
                process.write(fileToString);
            } catch (IOException e) {
                logger.error(null, "cat read file error. name: " + file, e);
                process.write("cat read file error: " + e.getMessage() + '\n');
                process.end(1);
                return;
            }
        }

        process.end();
    }

    @Override
    public void complete(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String token = tokens.get(tokens.size() - 1).value();

        File dir = null;
        String partName = "";
        if (StringUtils.isBlank(token)) {
            dir = new File("").getAbsoluteFile();
            token = "";
        } else if (token.endsWith("/")) {
            dir = new File(token);
        } else {
            File parent = new File(token).getAbsoluteFile().getParentFile();
            if (parent != null && parent.exists()) {
                dir = parent;
                partName = new File(token).getName();
            }
        }

        File tokenFile = new File(token);

        String tokenFileName = null;
        if (token.endsWith("/")) {
            tokenFileName = "";
        } else {
            tokenFileName = tokenFile.getName();
        }

        if (dir == null) {
            super.complete(completion);
            return;
        }

        File[] listFiles = dir.listFiles();

        ArrayList<String> names = new ArrayList<String>();
        for (File child : listFiles) {
            if (child.getName().startsWith(partName)) {
                if (child.isDirectory()) {
                    names.add(child.getName() + "/");
                } else {
                    names.add(child.getName());
                }
            }
        }

        if (names.size() == 1 && names.get(0).endsWith("/")) {
            String name = names.get(0);
            // 这个函数补全后不会有空格，并且只能传入要补全的内容
            completion.complete(name.substring(tokenFileName.length(), name.length()), false);
            return;
        }

        String prefix = null;
        if (token.endsWith("/")) {
            prefix = token;
        } else {
            prefix = token.substring(0, token.length() - new File(token).getName().length());
        }

        ArrayList<String> namesWithPrefix = new ArrayList<String>();
        for (String name : names) {
            namesWithPrefix.add(prefix + name);
        }
        // 这个函数需要保留前缀
        CompletionUtils.complete(completion, namesWithPrefix);
    }

}
