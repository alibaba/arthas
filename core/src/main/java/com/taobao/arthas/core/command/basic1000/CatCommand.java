package com.taobao.arthas.core.command.basic1000;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.taobao.arthas.core.command.model.CatModel;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
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

/**
 * Cat命令类
 * 用于连接并打印文件内容，类似于Unix的cat命令
 */
@Name("cat")
@Summary("Concatenate and print files")
public class CatCommand extends AnnotatedCommand {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(CatCommand.class);
    // 要读取的文件列表
    private List<String> files;
    // 文件编码
    private String encoding;
    // 文件大小限制，默认128KB
    private Integer sizeLimit = 128 * 1024;
    // 最大文件大小限制：8MB
    private int maxSizeLimit = 8 * 1024 * 1024;

    /**
     * 设置要读取的文件列表
     * @param files 文件路径列表
     */
    @Argument(argName = "files", index = 0)
    @Description("files")
    public void setFiles(List<String> files) {
        this.files = files;
    }

    /**
     * 设置文件编码
     * @param encoding 文件编码格式
     */
    @Option(longName = "encoding")
    @Description("File encoding")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * 设置文件大小限制
     * @param sizeLimit 文件大小限制（字节），默认128*1024，最大8*1024*1024
     */
    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (128 * 1024 by default, the maximum value is 8 * 1024 * 1024)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    /**
     * 处理Cat命令
     * @param process 命令处理进程
     */
    @Override
    public void process(CommandProcess process) {
        // 验证选项参数
        if (!verifyOptions(process)) {
            return;
        }

        // 第一轮检查：验证所有文件是否存在且不是目录
        for (String file : files) {
            File f = new File(file);
            // 检查文件是否存在
            if (!f.exists()) {
                process.end(-1, "cat " + file + ": No such file or directory");
                return;
            }
            // 检查是否是目录
            if (f.isDirectory()) {
                process.end(-1, "cat " + file + ": Is a directory");
                return;
            }
        }

        // 第二轮处理：读取并输出文件内容
        for (String file : files) {
            File f = new File(file);
            // 检查文件大小是否超过限制
            if (f.length() > sizeLimit) {
                process.end(-1, "cat " + file + ": Is too large, size: " + f.length());
                return;
            }
            try {
                // 读取文件内容，使用指定编码或默认编码
                String fileToString = FileUtils.readFileToString(f,
                        encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
                // 将文件内容添加到结果中
                process.appendResult(new CatModel(file, fileToString));
            } catch (IOException e) {
                // 处理读取异常
                logger.error("cat read file error. name: " + file, e);
                process.end(1, "cat read file error: " + e.getMessage());
                return;
            }
        }

        // 命令执行成功
        process.end();
    }

    /**
     * 验证命令选项参数
     * @param process 命令处理进程
     * @return 验证通过返回true，否则返回false
     */
    private boolean verifyOptions(CommandProcess process) {
        // 检查sizeLimit是否超过最大值
        if (sizeLimit > maxSizeLimit) {
            process.end(-1, "sizeLimit cannot be large than: " + maxSizeLimit);
            return false;
        }

        // 目前不支持过滤，限制http请求执行的文件大小
        int maxSizeLimitOfNonTty = 128 * 1024;
        // 在非tty会话中执行时，限制文件大小
        if (!process.session().isTty() && sizeLimit > maxSizeLimitOfNonTty) {
            process.end(-1, "When executing in non-tty session, sizeLimit cannot be large than: " + maxSizeLimitOfNonTty);
            return false;
        }
        return true;
    }

    /**
     * 命令自动补全功能
     * @param completion 补全对象
     */
    @Override
    public void complete(Completion completion) {
        // 尝试进行文件路径补全
        if (!CompletionUtils.completeFilePath(completion)) {
            // 如果文件路径补全失败，使用默认补全
            super.complete(completion);
        }
    }

}
