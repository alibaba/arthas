package com.taobao.arthas.core.shell.command.internal;

import com.taobao.arthas.core.command.basic1000.TeeCommand;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.annotations.CLIConfigurator;

import java.io.*;
import java.util.List;

/**
 * Tee输出处理器
 * <p>
 * 该类实现了类似Linux系统中'tee'命令的功能，将输出数据同时发送到标准输出和文件。
 * 继承自StdoutHandler并实现CloseFunction接口，用于处理管道输出的文件重定向。
 * </p>
 *
 * @author min.yang
 */
public class TeeHandler extends StdoutHandler implements CloseFunction {
    /** 命令名称 */
    public static final String NAME = "tee";

    /** 用于写入文件的PrintWriter对象 */
    private PrintWriter out;

    /** CLI配置对象，用于解析命令行参数 */
    private static CLI cli = null;

    /**
     * 构造函数
     * <p>
     * 创建一个TeeHandler实例，将输出写入指定文件。
     * 如果文件不存在会自动创建，如果父目录不存在也会创建。
     * </p>
     *
     * @param filePath 要写入的文件路径
     * @param append 是否追加模式，true表示追加到文件末尾，false表示覆盖
     * @throws IOException 如果文件路径是目录或文件创建失败
     */
    public TeeHandler(String filePath, boolean append) throws IOException {
        // 如果文件路径为空，直接返回
        if (StringUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);

        // 检查文件路径是否是目录
        if (file.isDirectory()) {
            throw new IOException(filePath + ": Is a directory");
        }

        // 如果文件不存在，创建父目录
        if (!file.exists()) {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
        }
        // 创建PrintWriter用于写入文件
        out = new PrintWriter(new BufferedWriter(new FileWriter(file, append)));
    }

    /**
     * 根据命令行token注入TeeHandler实例
     * <p>
     * 该方法解析命令行参数，创建TeeCommand对象并注入参数，
     * 然后根据解析结果创建TeeHandler实例。
     * </p>
     *
     * @param tokens 命令行token列表
     * @return TeeHandler实例
     * @throws RuntimeException 如果参数注入失败或IO异常
     */
    public static StdoutHandler inject(List<CliToken> tokens) {
        // 解析命令行参数
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);

        // 创建TeeCommand对象
        TeeCommand teeCommand = new TeeCommand();
        // 懒加载CLI配置
        if (cli == null) {
            cli = CLIConfigurator.define(TeeCommand.class);
        }
        // 解析命令行参数
        CommandLine commandLine = cli.parse(args, true);

        try {
            // 将解析的参数注入到TeeCommand对象
            CLIConfigurator.inject(commandLine, teeCommand);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // 获取文件路径和追加模式参数
        String filePath = teeCommand.getFilePath();
        boolean append = teeCommand.isAppend();
        try {
            return new TeeHandler(filePath, append);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理数据并写入文件
     * <p>
     * 该方法将接收到的数据写入文件，同时将数据返回给调用者继续传递。
     * 这样实现了同时输出到文件和标准输出的效果。
     * </p>
     *
     * @param data 要处理的数据
     * @return 原始数据（不变）
     */
    @Override
    public String apply(String data) {
        // 先调用父类的处理方法
        data = super.apply(data);
        // 将数据写入文件
        if (out != null) {
            out.write(data);
            out.flush();
        }
        // 返回原始数据以继续传递
        return data;
    }

    /**
     * 关闭输出流
     * <p>
     * 关闭文件输出流，释放相关资源。
     * 实现CloseFunction接口的方法。
     * </p>
     */
    @Override
    public void close() {
        if (out != null) {
            out.close();
        }
    }
}
