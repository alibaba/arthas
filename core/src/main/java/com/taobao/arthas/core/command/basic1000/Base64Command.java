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
    private static final Logger logger = LoggerFactory.getLogger(Base64Command.class);
    private String file;
    private int sizeLimit = 128 * 1024;
    private static final int MAX_SIZE_LIMIT = 8 * 1024 * 1024;

    private boolean decode;

    private String input;
    private String output;

    @Argument(argName = "file", index = 0, required = false)
    @Description("file")
    public void setFiles(String file) {
        this.file = file;
    }

    @Option(shortName = "d", longName = "decode", flag = true)
    @Description("decodes input")
    public void setDecode(boolean decode) {
        this.decode = decode;
    }

    @Option(shortName = "i", longName = "input")
    @Description("input file")
    public void setInput(String input) {
        this.input = input;
    }

    @Option(shortName = "o", longName = "output")
    @Description("output file")
    public void setOutput(String output) {
        this.output = output;
    }

    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (128 * 1024 by default, the maximum value is 8 * 1024 * 1024)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Override
    public void process(CommandProcess process) {
        if (!verifyOptions(process)) {
            return;
        }

        // 确认输入
        if (file == null) {
            if (this.input != null) {
                file = input;
            } else {
                process.end(-1, ": No file, nor input");
                return;
            }
        }

        File f = new File(file);
        if (!f.exists()) {
            process.end(-1, file + ": No such file or directory");
            return;
        }
        if (f.isDirectory()) {
            process.end(-1, file + ": Is a directory");
            return;
        }

        if (f.length() > sizeLimit) {
            process.end(-1, file + ": Is too large, size: " + f.length());
            return;
        }

        InputStream input = null;

        try {
            input = new FileInputStream(f);
            byte[] bytes = IOUtils.getBytes(input);

            ByteBuf convertResult = null;
            if (this.decode) {
                convertResult = Base64.decode(Unpooled.wrappedBuffer(bytes));
            } else {
                convertResult = Base64.encode(Unpooled.wrappedBuffer(bytes));
            }

            if (this.output != null) {
                int readableBytes = convertResult.readableBytes();
                OutputStream out = new FileOutputStream(this.output);
                convertResult.readBytes(out, readableBytes);
                process.appendResult(new Base64Model(null));
            } else {
                String base64Str = convertResult.toString(CharsetUtil.UTF_8);
                process.appendResult(new Base64Model(base64Str));
            }
        } catch (IOException e) {
            logger.error("read file error. name: " + file, e);
            process.end(1, "read file error: " + e.getMessage());
            return;
        } finally {
            IOUtils.close(input);
        }

        process.end();
    }

    private boolean verifyOptions(CommandProcess process) {
        if(this.file == null && this.input == null) {
            process.end(-1);
            return false;
        }

        if (sizeLimit > MAX_SIZE_LIMIT) {
            process.end(-1, "sizeLimit cannot be large than: " + MAX_SIZE_LIMIT);
            return false;
        }

        // 目前不支持过滤，限制http请求执行的文件大小
        int maxSizeLimitOfNonTty = 128 * 1024;
        if (!process.session().isTty() && sizeLimit > maxSizeLimitOfNonTty) {
            process.end(-1,
                    "When executing in non-tty session, sizeLimit cannot be large than: " + maxSizeLimitOfNonTty);
            return false;
        }
        return true;
    }

    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

}
