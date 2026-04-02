package com.taobao.arthas.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetOptionHandler;
import org.apache.commons.net.telnet.WindowSizeOptionHandler;

import com.taobao.arthas.common.OSUtils;
import com.taobao.arthas.common.UsageRender;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.UsageMessageFormatter;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.CLIConfigurator;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import jline.Terminal;
import jline.TerminalSupport;
import jline.UnixTerminal;
import jline.console.ConsoleReader;
import jline.console.KeyMap;

/**
 * Arthas Telnet 客户端控制台
 *
 * 这是一个用于连接到 Arthas 服务器的 Telnet 客户端实现。
 * 它提供了两种工作模式：
 * 1. 交互模式：用户可以在终端中直接输入命令与 Arthas 服务器交互
 * 2. 批处理模式：执行指定的命令或从文件中批量读取命令执行
 *
 * 该客户端支持终端窗口大小协商、Ctrl+C 中断处理、命令超时等功能。
 *
 * @author ralf0131 2016-12-29 11:55.
 * @author hengyunabc 2018-11-01
 */
@Name("arthas-client")
@Summary("Arthas Telnet Client")
@Description("EXAMPLES:\n" + "  java -jar arthas-client.jar 127.0.0.1 3658\n"
        + "  java -jar arthas-client.jar -c 'dashboard -n 1' \n"
        + "  java -jar arthas-client.jar -f batch.as 127.0.0.1\n")
public class TelnetConsole {
    // Arthas 命令提示符前缀，用于识别命令执行完成的标志
    private static final String PROMPT = "[arthas@"; // [arthas@49603]$

    // 默认连接超时时间：5000 毫秒（5秒）
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000; // 5000 ms

    // Ctrl+C 控制字符的 ASCII 码值
    private static final byte CTRL_C = 0x03;

    // ------- 状态码定义 ------- //
    /**
     * 执行成功状态码
     */
    public static final int STATUS_OK = 0;
    /**
     * 通用错误状态码
     */
    public static final int STATUS_ERROR = 1;
    /**
     * 命令执行超时状态码
     */
    public static final int STATUS_EXEC_TIMEOUT = 100;
    /**
     * 命令执行错误状态码
     */
    public static final int STATUS_EXEC_ERROR = 101;


    // 是否显示帮助信息
    private boolean help = false;

    // 目标服务器的 IP 地址，默认为本地回环地址
    private String targetIp = "127.0.0.1";

    // 目标服务器的端口号，默认为 3658（Arthas 默认端口）
    private int port = 3658;

    // 要执行的命令字符串（多个命令用分号分隔）
    private String command;

    // 批处理文件路径
    private String batchFile;

    // 命令执行超时时间（毫秒），-1 表示不限制
    private int executionTimeout = -1;

    // 终端宽度（字符数），null 表示使用默认值
    private Integer width = null;

    // 终端高度（行数），null 表示使用默认值
    private Integer height = null;

    /**
     * 设置目标 IP 地址
     */
    @Argument(argName = "target-ip", index = 0, required = false)
    @Description("Target ip")
    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    /**
     * 设置目标服务器端口
     */
    @Argument(argName = "port", index = 1, required = false)
    @Description("The remote server port")
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 设置是否显示帮助信息
     */
    @Option(longName = "help", flag = true)
    @Description("Print usage")
    public void setHelp(boolean help) {
        this.help = help;
    }

    /**
     * 设置要执行的命令
     * 多个命令用分号分隔，例如: "dashboard -n 1;thread"
     */
    @Option(shortName = "c", longName = "command")
    @Description("Command to execute, multiple commands separated by ;")
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * 设置批处理文件路径
     * 文件中每行一个命令
     */
    @Option(shortName = "f", longName = "batch-file")
    @Description("The batch file to execute")
    public void setBatchFile(String batchFile) {
        this.batchFile = batchFile;
    }

    /**
     * 设置命令执行超时时间
     * @param executionTimeout 超时时间（毫秒）
     */
    @Option(shortName = "t", longName = "execution-timeout")
    @Description("The timeout (ms) of execute commands or batch file ")
    public void setExecutionTimeout(int executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    /**
     * 设置终端宽度
     * @param width 终端宽度（字符数）
     */
    @Option(shortName = "w", longName = "width")
    @Description("The terminal width")
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * 设置终端高度
     * @param height 终端高度（行数）
     */
    @Option(shortName = "h", longName = "height")
    @Description("The terminal height")
    public void setheight(int height) {
        this.height = height;
    }

    /**
     * 默认构造函数
     */
    public TelnetConsole() {
    }

    /**
     * 读取批处理文件中的所有行
     *
     * @param batchFile 批处理文件对象
     * @return 文件中所有行的列表，每行作为一个字符串元素
     */
    private static List<String> readLines(File batchFile) {
        List<String> list = new ArrayList<String>();
        BufferedReader br = null;
        try {
            // 创建缓冲读取器读取文件
            br = new BufferedReader(new FileReader(batchFile));
            String line = br.readLine();
            // 逐行读取文件内容
            while (line != null) {
                list.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            // 读取失败时打印堆栈跟踪
            e.printStackTrace();
        } finally {
            // 确保关闭读取器
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // 忽略关闭时的异常
                }
            }
        }
        return list;
    }

    /**
     * 程序入口点
     *
     * 解析命令行参数，连接到 Arthas 服务器，执行命令或进入交互模式。
     * 执行完成后根据返回的状态码退出程序。
     *
     * @param args 命令行参数
     * @throws Exception 执行过程中可能抛出的异常
     */
    public static void main(String[] args) throws Exception {

        try {
            // 调用 process 方法处理命令，并注册退出回调
            int status = process(args, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 正常退出，返回成功状态码
                    System.exit(STATUS_OK);
                }
            });
            // 根据返回的状态码退出程序
            System.exit(status);
        } catch (Throwable e) {
            // 捕获所有异常，打印堆栈并显示使用帮助
            e.printStackTrace();
            CLI cli = CLIConfigurator.define(TelnetConsole.class);
            System.out.println(usage(cli));
            System.exit(STATUS_ERROR);
        }

    }

    /**
     * 提供给 arthas-boot 使用的主处理函数
     *
     * 该方法是供 arthas-boot 调用的入口点，不会调用 System.exit()。
     *
     * @param args 命令行参数
     * @return 状态码（0 表示成功，非 0 表示错误）
     * @throws IOException IO 异常
     * @throws InterruptedException 线程中断异常
     */
    public static int process(String[] args) throws IOException, InterruptedException {
        return process(args, null);
    }

    /**
     * Arthas 客户端主处理函数
     *
     * 注意：process() 函数提供给 arthas-boot 使用，内部不能调用 System.exit() 结束进程的方法
     *
     * 该函数完成以下任务：
     * 1. 解析命令行参数
     * 2. 配置终端参数（宽度、高度等）
     * 3. 连接到 Arthas 服务器
     * 4. 根据参数选择执行模式（交互模式或批处理模式）
     * 5. 处理 Ctrl+C 和 Ctrl+D 事件
     *
     * @param args 命令行参数数组
     * @param eotEventCallback Ctrl+D 信号的回调函数（表示传输结束，End of Transmission）
     * @return 状态码（0 表示成功，非 0 表示错误）
     * @throws IOException IO 异常
     */
    public static int process(String[] args, ActionListener eotEventCallback) throws IOException {
        // 支持 Cygwin/MinGW 环境下的 jline 彩色输出
        if (OSUtils.isCygwinOrMinGW()) {
            System.setProperty("jline.terminal", System.getProperty("jline.terminal", "jline.UnixTerminal"));
        }

        // 创建 TelnetConsole 实例并解析命令行参数
        TelnetConsole telnetConsole = new TelnetConsole();
        CLI cli = CLIConfigurator.define(TelnetConsole.class);

        // 解析命令行参数
        CommandLine commandLine = cli.parse(Arrays.asList(args));

        // 将解析后的参数注入到 TelnetConsole 实例
        CLIConfigurator.inject(commandLine, telnetConsole);

        // 如果请求显示帮助信息，打印用法后返回成功
        if (telnetConsole.isHelp()) {
            System.out.println(usage(cli));
            return STATUS_OK;
        }

        // 准备要执行的命令列表
        List<String> cmds = new ArrayList<String>();
        // 优先使用 -c 参数指定的命令
        if (telnetConsole.getCommand() != null) {
            // 按分号分割多个命令
            for (String c : telnetConsole.getCommand().split(";")) {
                cmds.add(c.trim());
            }
        } else if (telnetConsole.getBatchFile() != null) {
            // 其次使用 -f 参数指定的批处理文件
            File file = new File(telnetConsole.getBatchFile());
            if (!file.exists()) {
                throw new IllegalArgumentException("batch file do not exist: " + telnetConsole.getBatchFile());
            } else {
                // 读取批处理文件中的所有命令
                cmds.addAll(readLines(file));
            }
        }

        // 创建控制台读取器，用于处理用户输入
        final ConsoleReader consoleReader = new ConsoleReader(System.in, System.out);
        consoleReader.setHandleUserInterrupt(true);
        Terminal terminal = consoleReader.getTerminal();

        // 配置终端以支持捕获 Ctrl+C 事件
        terminal.disableInterruptCharacter();
        if (terminal instanceof UnixTerminal) {
            // 禁用 Unix 终端的字面下一个字符功能
            ((UnixTerminal) terminal).disableLitteralNextCharacter();
        }

        try {
            // 初始化终端宽度和高度
            int width = TerminalSupport.DEFAULT_WIDTH;
            int height = TerminalSupport.DEFAULT_HEIGHT;

            if (!cmds.isEmpty()) {
                // 批处理模式：使用命令行参数指定的宽度高度，或使用默认值
                if (telnetConsole.getWidth() != null) {
                    width = telnetConsole.getWidth();
                }
                if (telnetConsole.getheight() != null) {
                    height = telnetConsole.getheight();
                }
            } else {
                // 交互模式：获取当前终端的实际大小
                if (telnetConsole.getWidth() != null) {
                    width = telnetConsole.getWidth();
                } else {
                    width = terminal.getWidth();
                    // Windows DOS 的特殊处理：宽度减 1（避免换行问题）
                    if (OSUtils.isWindows()) {
                        width--;
                    }
                }
                if (telnetConsole.getheight() != null) {
                    height = telnetConsole.getheight();
                } else {
                    height = terminal.getHeight();
                }
            }

            // 创建 Telnet 客户端
            final TelnetClient telnet = new TelnetClient();
            telnet.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);

            // 发送初始终端大小选项给服务器
            // 参数：宽度, 高度, 本地激活, 远程激活, 接受本地, 接受远程
            TelnetOptionHandler sizeOpt = new WindowSizeOptionHandler(width, height, true, true, false, false);
            try {
                telnet.addOptionHandler(sizeOpt);
            } catch (InvalidTelnetOptionException e) {
                // 忽略选项处理器添加失败
            }

            // 绑定 Ctrl+C 事件回调
            consoleReader.getKeys().bind(Character.toString((char) CTRL_C), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        // 清除当前行的输入
                        consoleReader.getCursorBuffer().clear();
                        // 向服务器发送 Ctrl+C 信号
                        telnet.getOutputStream().write(CTRL_C);
                        telnet.getOutputStream().flush();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }

            });

            // 绑定 Ctrl+D 事件回调（EOT - End of Transmission）
            consoleReader.getKeys().bind(Character.toString(KeyMap.CTRL_D), eotEventCallback);

            try {
                // 连接到 Arthas 服务器
                telnet.connect(telnetConsole.getTargetIp(), telnetConsole.getPort());
            } catch (IOException e) {
                System.out.println("Connect to telnet server error: " + telnetConsole.getTargetIp() + " "
                        + telnetConsole.getPort());
                throw e;
            }

            // 判断执行模式
            if (cmds.isEmpty()) {
                // 交互模式：直接转发输入输出流，实现实时交互
                IOUtil.readWrite(telnet.getInputStream(), telnet.getOutputStream(), consoleReader.getInput(),
                        consoleReader.getOutput());
            } else {
                // 批处理模式：执行所有命令后退出
                try {
                    return batchModeRun(telnet, cmds, telnetConsole.getExecutionTimeout());
                } catch (Throwable e) {
                    System.out.println("Execute commands error: " + e.getMessage());
                    e.printStackTrace();
                    return STATUS_EXEC_ERROR;
                } finally {
                    // 确保断开连接
                    try {
                        telnet.disconnect();
                    } catch (IOException e) {
                        // 忽略断开连接时的异常
                    }
                }
            }

            return STATUS_OK;
        } finally {
            // 恢复终端设置，修复 https://github.com/alibaba/arthas/issues/1412
            try {
                terminal.restore();
            } catch (Throwable e) {
                System.out.println("Restore terminal settings failure: "+e.getMessage());
                e.printStackTrace();
            }
        }

    }

    /**
     * 批处理模式运行
     *
     * 依次执行命令列表中的所有命令，等待每个命令执行完成后再执行下一个。
     * 通过检测 Arthas 提示符来判断命令是否执行完成。
     *
     * @param telnet Telnet 客户端实例
     * @param commands 要执行的命令列表
     * @param executionTimeout 执行超时时间（毫秒），-1 表示不限制
     * @return 状态码（0 表示成功，100 表示超时）
     * @throws IOException IO 异常
     * @throws InterruptedException 线程中断异常
     */
    private static int batchModeRun(TelnetClient telnet, List<String> commands, final int executionTimeout)
            throws IOException, InterruptedException {
        // 如果没有命令需要执行，直接返回成功
        if (commands.size() == 0) {
            return STATUS_OK;
        }

        // 记录开始时间，用于超时检测
        long startTime = System.currentTimeMillis();
        final InputStream inputStream = telnet.getInputStream();
        final OutputStream outputStream = telnet.getOutputStream();

        // 创建阻塞队列用于接收提示符信号（命令执行完成的标志）
        final BlockingQueue<String> receviedPromptQueue = new LinkedBlockingQueue<String>(1);

        // 创建打印结果的线程，从服务器读取输出并打印到控制台
        Thread printResultThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StringBuilder line = new StringBuilder();
                    // 使用 UTF-8 编码读取输入流
                    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    int b = -1;
                    while (true) {
                        b = in.read();
                        if (b == -1) {
                            // 输入流结束，退出循环
                            break;
                        }
                        line.appendCodePoint(b);

                        // 检查到有 [arthas@ 时，意味着命令执行完成，可以执行下一个命令了
                        int index = line.indexOf(PROMPT);
                        if (index > 0) {
                            // 清除已处理的内容
                            line.delete(0, index + PROMPT.length());
                            // 向队列发送信号，提示命令执行完成
                            receviedPromptQueue.put("");
                        }
                        // 将字符输出到控制台
                        System.out.print(Character.toChars(b));
                    }
                } catch (Exception e) {
                    // 忽略异常
                }
            }
        });
        // 启动打印线程
        printResultThread.start();

        // 向 Arthas 服务器发送命令
        for (String command : commands) {
            // 跳过空命令
            if (command.trim().isEmpty()) {
                continue;
            }
            // 轮询等待提示符，并检查是否超时
            while (receviedPromptQueue.poll(100, TimeUnit.MILLISECONDS) == null) {
                if (executionTimeout > 0) {
                    long now = System.currentTimeMillis();
                    // 检查是否超过执行超时时间
                    if (now - startTime > executionTimeout) {
                        return STATUS_EXEC_TIMEOUT;
                    }
                }
            }
            // 向服务器发送命令（使用 plaintext 格式避免 ANSI 转义序列）
            outputStream.write((command + " | plaintext\n").getBytes());
            outputStream.flush();
        }

        // 等待最后一个命令执行后的提示符，然后发送 quit 命令
        receviedPromptQueue.take();
        outputStream.write("quit\n".getBytes());
        outputStream.flush();
        System.out.println();

        return STATUS_OK;
    }

    /**
     * 生成使用帮助信息
     *
     * @param cli CLI 实例
     * @return 格式化后的使用帮助字符串
     */
    private static String usage(CLI cli) {
        StringBuilder usageStringBuilder = new StringBuilder();
        UsageMessageFormatter usageMessageFormatter = new UsageMessageFormatter();
        usageMessageFormatter.setOptionComparator(null);
        cli.usage(usageStringBuilder, usageMessageFormatter);
        return UsageRender.render(usageStringBuilder.toString());
    }

    /**
     * 获取目标 IP 地址
     * @return 目标 IP 地址
     */
    public String getTargetIp() {
        return targetIp;
    }

    /**
     * 获取目标端口
     * @return 目标端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 获取要执行的命令
     * @return 命令字符串
     */
    public String getCommand() {
        return command;
    }

    /**
     * 获取批处理文件路径
     * @return 批处理文件路径
     */
    public String getBatchFile() {
        return batchFile;
    }

    /**
     * 获取执行超时时间
     * @return 超时时间（毫秒）
     */
    public int getExecutionTimeout() {
        return executionTimeout;
    }

    /**
     * 获取终端宽度
     * @return 终端宽度（字符数）
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * 获取终端高度
     * @return 终端高度（行数）
     */
    public Integer getheight() {
        return height;
    }

    /**
     * 是否显示帮助信息
     * @return true 表示显示帮助，false 表示不显示
     */
    public boolean isHelp() {
        return help;
    }

}
