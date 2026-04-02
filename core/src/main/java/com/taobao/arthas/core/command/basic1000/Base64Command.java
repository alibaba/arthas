package com.taobao.arthas.core.command.basic1000;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.Base64Model;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.CharsetUtil;

/**
 * Base64编码/解码命令类
 * 用于对文件进行Base64编码或解码操作
 *
 * @author hengyunabc 2021-01-05
 *
 */
@Name("base64")
@Summary("Encode and decode using Base64 representation")
@Description(Constants.EXAMPLE +
        "  base64 /tmp/test.txt\n" +
        "  base64 --input /tmp/test.txt --output /tmp/result.txt\n" +
        "  base64 -d /tmp/result.txt\n"
        + Constants.WIKI + Constants.WIKI_HOME + "base64")
public class Base64Command extends AnnotatedCommand {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(Base64Command.class);
    // 要处理的文件路径
    private String file;
    // 文件大小限制，默认128KB
    private int sizeLimit = 128 * 1024;
    // 最大文件大小限制：8MB
    private static final int MAX_SIZE_LIMIT = 8 * 1024 * 1024;

    // 是否进行解码操作（true为解码，false为编码）
    private boolean decode;

    // 输入文件路径
    private String input;
    // 输出文件路径
    private String output;

    /**
     * 设置要处理的文件路径
     * @param file 文件路径
     */
    @Argument(argName = "file", index = 0, required = false)
    @Description("file")
    public void setFiles(String file) {
        this.file = file;
    }

    /**
     * 设置是否进行解码操作
     * @param decode true表示解码，false表示编码
     */
    @Option(shortName = "d", longName = "decode", flag = true)
    @Description("decodes input")
    public void setDecode(boolean decode) {
        this.decode = decode;
    }

    /**
     * 设置输入文件路径
     * @param input 输入文件路径
     */
    @Option(shortName = "i", longName = "input")
    @Description("input file")
    public void setInput(String input) {
        this.input = input;
    }

    /**
     * 设置输出文件路径
     * @param output 输出文件路径
     */
    @Option(shortName = "o", longName = "output")
    @Description("output file")
    public void setOutput(String output) {
        this.output = output;
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
     * 处理Base64编码/解码命令
     * @param process 命令处理进程
     */
    @Override
    public void process(CommandProcess process) {
        // 验证选项参数
        if (!verifyOptions(process)) {
            return;
        }

        // 确认输入文件
        if (file == null) {
            if (this.input != null) {
                // 如果没有指定file参数，使用input参数
                file = input;
            } else {
                // 既没有file也没有input参数，报错返回
                process.end(-1, ": No file, nor input");
                return;
            }
        }

        // 创建文件对象
        File f = new File(file);
        // 检查文件是否存在
        if (!f.exists()) {
            process.end(-1, file + ": No such file or directory");
            return;
        }
        // 检查是否是目录
        if (f.isDirectory()) {
            process.end(-1, file + ": Is a directory");
            return;
        }

        // 检查文件大小是否超过限制
        if (f.length() > sizeLimit) {
            process.end(-1, file + ": Is too large, size: " + f.length());
            return;
        }

        // 输入流和转换结果
        InputStream input = null;
        ByteBuf convertResult = null;

        try {
            // 打开文件输入流
            input = new FileInputStream(f);
            // 读取文件内容到字节数组
            byte[] bytes = IOUtils.getBytes(input);

            // 根据decode标志进行编码或解码
            if (this.decode) {
                // 解码操作
                convertResult = Base64.decode(Unpooled.wrappedBuffer(bytes));
            } else {
                // 编码操作
                convertResult = Base64.encode(Unpooled.wrappedBuffer(bytes));
            }

            // 判断是否指定了输出文件
            if (this.output != null) {
                // 将结果写入输出文件
                int readableBytes = convertResult.readableBytes();
                OutputStream out = new FileOutputStream(this.output);
                convertResult.readBytes(out, readableBytes);
                // 返回空结果，因为已经写入文件
                process.appendResult(new Base64Model(null));
            } else {
                // 将结果转换为字符串并返回
                String base64Str = convertResult.toString(CharsetUtil.UTF_8);
                process.appendResult(new Base64Model(base64Str));
            }
        } catch (IOException e) {
            // 处理IO异常
            logger.error("read file error. name: " + file, e);
            process.end(1, "read file error: " + e.getMessage());
            return;
        } finally {
            // 释放资源
            if (convertResult != null) {
                convertResult.release();
            }
            IOUtils.close(input);
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
        // 检查是否指定了文件或输入参数
        if(this.file == null && this.input == null) {
            process.end(-1);
            return false;
        }

        // 检查sizeLimit是否超过最大值
        if (sizeLimit > MAX_SIZE_LIMIT) {
            process.end(-1, "sizeLimit cannot be large than: " + MAX_SIZE_LIMIT);
            return false;
        }

        // 目前不支持过滤，限制http请求执行的文件大小
        int maxSizeLimitOfNonTty = 128 * 1024;
        // 在非tty会话中执行时，限制文件大小
        if (!process.session().isTty() && sizeLimit > maxSizeLimitOfNonTty) {
            process.end(-1,
                    "When executing in non-tty session, sizeLimit cannot be large than: " + maxSizeLimitOfNonTty);
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
