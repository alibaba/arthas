package com.taobao.arthas.core.shell.command.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.taobao.arthas.core.util.LogUtil;

/**
 * 重定向处理类
 * <p>
 * 该类继承自PlainTextHandler，实现了将输出重定向到文件的功能。
 * 支持文件的追加模式和覆盖模式，并实现了CloseFunction接口以支持资源释放。
 * 在Arthas中用于将命令执行结果保存到文件中，便于后续分析和存档。
 * </p>
 *
 * @author gehui 2017年7月27日 上午11:38:40
 * @author hengyunabc 2019-02-06
 */
public class RedirectHandler extends PlainTextHandler implements CloseFunction {
    /**
     * 打印写入器
     * <p>
     * 用于将输出内容写入到目标文件，采用缓冲写入以提高性能。
     * 如果未指定输出文件，该字段为null，此时会将内容输出到日志。
     * </p>
     */
    private PrintWriter out;

    /**
     * 目标文件对象
     * <p>
     * 保存输出重定向的目标文件引用，用于后续获取文件路径等信息。
     * </p>
     */
    private File file;

    /**
     * 默认构造函数
     * <p>
     * 创建一个未绑定文件的重定向处理器。
     * 此时输出将被重定向到日志系统而非文件。
     * </p>
     */
    public RedirectHandler() {

    }

    /**
     * 带参数的构造函数
     * <p>
     * 创建一个绑定到指定文件的重定向处理器。
     * 会自动创建必要的父目录，并处理文件存在性检查。
     * </p>
     *
     * @param name 目标文件路径，可以是相对路径或绝对路径
     * @param append 是否使用追加模式，true表示追加到文件末尾，false表示覆盖文件
     * @throws IOException 如果指定的路径是一个目录，或者文件创建/打开失败
     */
    public RedirectHandler(String name, boolean append) throws IOException {
        File file = new File(name);

        // 检查路径是否为目录，如果是则抛出异常
        if (file.isDirectory()) {
            throw new IOException(name + ": Is a directory");
        }

        // 如果文件不存在，创建必要的父目录
        if (!file.exists()) {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
        }

        this.file = file;
        // 使用缓冲写入器提高性能，支持追加模式
        out = new PrintWriter(new BufferedWriter(new FileWriter(file, append)));
    }

    /**
     * 处理输出数据并写入文件
     * <p>
     * 该方法首先调用父类方法将数据转换为纯文本格式，
     * 然后将转换后的数据写入到文件中并立即刷新缓冲区。
     * 如果未指定输出文件，则将数据输出到日志系统。
     * </p>
     *
     * @param data 需要处理的输出数据，可能包含ANSI转义序列
     * @return 处理后的纯文本数据
     */
    @Override
    public String apply(String data) {
        // 先调用父类方法转换为纯文本
        data = super.apply(data);

        if (out != null) {
            // 写入文件并立即刷新，确保数据及时落盘
            out.write(data);
            out.flush();
        } else {
            // 未指定文件时，输出到日志系统
            LogUtil.getResultLogger().info(data);
        }
        return data;
    }

    /**
     * 关闭输出流并释放资源
     * <p>
     * 该方法会关闭底层的PrintWriter，释放文件句柄和相关资源。
     * 在完成所有输出操作后应调用此方法以确保数据正确写入。
     * </p>
     */
    @Override
    public void close() {
        if (out != null) {
            out.close();
        }
    }

    /**
     * 获取输出文件的绝对路径
     * <p>
     * 返回当前重定向处理器绑定的目标文件的完整绝对路径。
     * </p>
     *
     * @return 文件的绝对路径字符串，如果未绑定文件则返回null
     */
    public String getFilePath() {
        return file.getAbsolutePath();
    }
}
