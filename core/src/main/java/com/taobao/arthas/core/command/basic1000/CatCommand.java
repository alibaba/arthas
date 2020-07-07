package com.taobao.arthas.core.command.basic1000;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.taobao.arthas.core.command.model.CatModel;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.command.ExitStatus;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

@Name("cat")
@Summary("Concatenate and print files")
public class CatCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(CatCommand.class);
    private List<String> files;
    private String encoding;
    private Integer sizeLimit = 128 * 1024;
    private int maxSizeLimit = 8 * 1024 * 1024;

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

    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (128 * 1024 by default, the maximum value is 8 * 1024 * 1024)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Override
    public ExitStatus process(CommandProcess process) {
        if (sizeLimit > maxSizeLimit) {
            return ExitStatus.failure(-1, "sizeLimit cannot be large than: " + maxSizeLimit);
        }

        //目前不支持过滤，限制http请求执行的文件大小
        int maxSizeLimitOfNonTty = 128 * 1024;
        if (!process.session().isTty() && sizeLimit > maxSizeLimitOfNonTty) {
            return ExitStatus.failure(-1, "When executing in non-tty session, sizeLimit cannot be large than: " + maxSizeLimitOfNonTty);
        }

        for (String file : files) {
            File f = new File(file);
            if (!f.exists()) {
                return ExitStatus.failure(-1, "cat " + file + ": No such file or directory");
            }
            if (f.isDirectory()) {
                return ExitStatus.failure(-1, "cat " + file + ": Is a directory");
            }
        }

        for (String file : files) {
            File f = new File(file);
            if (f.length() > sizeLimit) {
                return ExitStatus.failure(-1, "cat " + file + ": Is too large, size: " + f.length());
            }
            try {
                String fileToString = FileUtils.readFileToString(f,
                        encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
                process.appendResult(new CatModel(file, fileToString));
            } catch (IOException e) {
                logger.error("cat read file error. name: " + file, e);
                return ExitStatus.failure(1, "cat read file error: " + e.getMessage());
            }
        }

        return ExitStatus.success();
    }

    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

}
