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
 * @author ralf0131 2016-12-29 11:55.
 * @author hengyunabc 2018-11-01
 */
@Name("arthas-client")
@Summary("Arthas Telnet Client")
@Description("EXAMPLES:\n" + "  java -jar arthas-client.jar 127.0.0.1 3658\n"
        + "  java -jar arthas-client.jar -c 'dashboard -n 1' \n"
        + "  java -jar arthas-client.jar -f batch.as 127.0.0.1\n")
public class TelnetConsole {
    private static final String PROMPT = "[arthas@"; // [arthas@49603]$
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000; // 5000 ms

    private static final byte CTRL_C = 0x03;

    // ------- Status codes ------- //
    /**
     * Process success
     */
    public static final int STATUS_OK = 0;
    /**
     * Generic error
     */
    public static final int STATUS_ERROR = 1;
    /**
     * Execute commands timeout
     */
    public static final int STATUS_EXEC_TIMEOUT = 100;
    /**
     * Execute commands error
     */
    public static final int STATUS_EXEC_ERROR = 101;


    private boolean help = false;

    private String targetIp = "127.0.0.1";
    private int port = 3658;

    private String command;
    private String batchFile;
    private int executionTimeout = -1;

    private Integer width = null;
    private Integer height = null;

    @Argument(argName = "target-ip", index = 0, required = false)
    @Description("Target ip")
    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    @Argument(argName = "port", index = 1, required = false)
    @Description("The remote server port")
    public void setPort(int port) {
        this.port = port;
    }

    @Option(longName = "help", flag = true)
    @Description("Print usage")
    public void setHelp(boolean help) {
        this.help = help;
    }

    @Option(shortName = "c", longName = "command")
    @Description("Command to execute, multiple commands separated by ;")
    public void setCommand(String command) {
        this.command = command;
    }

    @Option(shortName = "f", longName = "batch-file")
    @Description("The batch file to execute")
    public void setBatchFile(String batchFile) {
        this.batchFile = batchFile;
    }

    @Option(shortName = "t", longName = "execution-timeout")
    @Description("The timeout (ms) of execute commands or batch file ")
    public void setExecutionTimeout(int executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    @Option(shortName = "w", longName = "width")
    @Description("The terminal width")
    public void setWidth(int width) {
        this.width = width;
    }

    @Option(shortName = "h", longName = "height")
    @Description("The terminal height")
    public void setheight(int height) {
        this.height = height;
    }

    public TelnetConsole() {
    }

    private static List<String> readLines(File batchFile) {
        List<String> list = new ArrayList<String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(batchFile));
            String line = br.readLine();
            while (line != null) {
                list.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return list;
    }

    public static void main(String[] args) throws Exception {

        try {
            int status = process(args, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(STATUS_OK);
                }
            });
            System.exit(status);
        } catch (Throwable e) {
            e.printStackTrace();
            CLI cli = CLIConfigurator.define(TelnetConsole.class);
            System.out.println(usage(cli));
            System.exit(STATUS_ERROR);
        }

    }

    /**
     * 提供给arthas-boot使用的主处理函数
     *
     * @param args
     * @return status code
     * @throws IOException
     * @throws InterruptedException
     */
    public static int process(String[] args) throws IOException, InterruptedException {
        return process(args, null);
    }

    /**
     * arthas client 主函数
     * 注意： process()函数提供给arthas-boot使用，内部不能调用System.exit()结束进程的方法
     *
     * @param telnetConsole
     * @param cli
     * @param args
     * @param eotEventCallback Ctrl+D signals an End of Transmission (EOT) event
     * @return status code
     * @throws IOException
     * @throws InterruptedException
     */
    public static int process(String[] args, ActionListener eotEventCallback) throws IOException, InterruptedException {
        // support mingw/cygw jline color
        if (OSUtils.isCygwinOrMinGW()) {
            System.setProperty("jline.terminal", System.getProperty("jline.terminal", "jline.UnixTerminal"));
        }

        TelnetConsole telnetConsole = new TelnetConsole();
        CLI cli = CLIConfigurator.define(TelnetConsole.class);

        CommandLine commandLine = cli.parse(Arrays.asList(args));

        CLIConfigurator.inject(commandLine, telnetConsole);

        if (telnetConsole.isHelp()) {
            System.out.println(usage(cli));
            return STATUS_ERROR;
        }

        // Try to read cmds
        List<String> cmds = new ArrayList<String>();
        if (telnetConsole.getCommand() != null) {
            for (String c : telnetConsole.getCommand().split(";")) {
                cmds.add(c.trim());
            }
        } else if (telnetConsole.getBatchFile() != null) {
            File file = new File(telnetConsole.getBatchFile());
            if (!file.exists()) {
                throw new IllegalArgumentException("batch file do not exist: " + telnetConsole.getBatchFile());
            } else {
                cmds.addAll(readLines(file));
            }
        }

        final ConsoleReader consoleReader = new ConsoleReader(System.in, System.out);
        consoleReader.setHandleUserInterrupt(true);
        Terminal terminal = consoleReader.getTerminal();

        // support catch ctrl+c event
        terminal.disableInterruptCharacter();
        if (terminal instanceof UnixTerminal) {
            ((UnixTerminal) terminal).disableLitteralNextCharacter();
        }

        try {
            int width = TerminalSupport.DEFAULT_WIDTH;
            int height = TerminalSupport.DEFAULT_HEIGHT;

            if (!cmds.isEmpty()) {
                // batch mode
                if (telnetConsole.getWidth() != null) {
                    width = telnetConsole.getWidth();
                }
                if (telnetConsole.getheight() != null) {
                    height = telnetConsole.getheight();
                }
            } else {
                // normal telnet client, get current terminal size
                if (telnetConsole.getWidth() != null) {
                    width = telnetConsole.getWidth();
                } else {
                    width = terminal.getWidth();
                    // hack for windows dos
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

            final TelnetClient telnet = new TelnetClient();
            telnet.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);

            // send init terminal size
            TelnetOptionHandler sizeOpt = new WindowSizeOptionHandler(width, height, true, true, false, false);
            try {
                telnet.addOptionHandler(sizeOpt);
            } catch (InvalidTelnetOptionException e) {
                // ignore
            }

            // ctrl + c event callback
            consoleReader.getKeys().bind(new Character((char) CTRL_C).toString(), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        consoleReader.getCursorBuffer().clear(); // clear current line
                        telnet.getOutputStream().write(CTRL_C);
                        telnet.getOutputStream().flush();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }

            });

            // ctrl + d event call back
            consoleReader.getKeys().bind(new Character(KeyMap.CTRL_D).toString(), eotEventCallback);

            try {
                telnet.connect(telnetConsole.getTargetIp(), telnetConsole.getPort());
            } catch (IOException e) {
                System.out.println("Connect to telnet server error: " + telnetConsole.getTargetIp() + " "
                        + telnetConsole.getPort());
                throw e;
            }

            if (cmds.isEmpty()) {
                IOUtil.readWrite(telnet.getInputStream(), telnet.getOutputStream(), consoleReader.getInput(),
                        consoleReader.getOutput());
            } else {
                try {
                    return batchModeRun(telnet, cmds, telnetConsole.getExecutionTimeout());
                } catch (Throwable e) {
                    System.out.println("Execute commands error: " + e.getMessage());
                    e.printStackTrace();
                    return STATUS_EXEC_ERROR;
                } finally {
                    try {
                        telnet.disconnect();
                    } catch (IOException e) {
                        //ignore ex
                    }
                }
            }

            return STATUS_OK;
        } finally {
            //reset terminal setting, fix https://github.com/alibaba/arthas/issues/1412
            try {
                terminal.restore();
            } catch (Throwable e) {
                System.out.println("Restore terminal settings failure: "+e.getMessage());
                e.printStackTrace();
            }
        }

    }

    private static int batchModeRun(TelnetClient telnet, List<String> commands, final int executionTimeout)
            throws IOException, InterruptedException {
        if (commands.size() == 0) {
            return STATUS_OK;
        }

        long startTime = System.currentTimeMillis();
        final InputStream inputStream = telnet.getInputStream();
        final OutputStream outputStream = telnet.getOutputStream();

        final BlockingQueue<String> receviedPromptQueue = new LinkedBlockingQueue<String>(1);
        Thread printResultThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StringBuilder line = new StringBuilder();
                    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    int b = -1;
                    while (true) {
                        b = in.read();
                        if (b == -1) {
                            break;
                        }
                        line.appendCodePoint(b);

                        // 检查到有 [arthas@ 时，意味着可以执行下一个命令了
                        int index = line.indexOf(PROMPT);
                        if (index > 0) {
                            line.delete(0, index + PROMPT.length());
                            receviedPromptQueue.put("");
                        }
                        System.out.print(Character.toChars(b));
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        });
        printResultThread.start();

        // send commands to arthas server
        for (String command : commands) {
            if (command.trim().isEmpty()) {
                continue;
            }
            // try poll prompt and check timeout
            while (receviedPromptQueue.poll(100, TimeUnit.MILLISECONDS) == null) {
                if (executionTimeout > 0) {
                    long now = System.currentTimeMillis();
                    if (now - startTime > executionTimeout) {
                        return STATUS_EXEC_TIMEOUT;
                    }
                }
            }
            // send command to server
            outputStream.write((command + " | plaintext\n").getBytes());
            outputStream.flush();
        }

        // 读到最后一个命令执行后的 prompt ，可以直接发 quit命令了。
        receviedPromptQueue.take();
        outputStream.write("quit\n".getBytes());
        outputStream.flush();
        System.out.println();

        return STATUS_OK;
    }

    private static String usage(CLI cli) {
        StringBuilder usageStringBuilder = new StringBuilder();
        UsageMessageFormatter usageMessageFormatter = new UsageMessageFormatter();
        usageMessageFormatter.setOptionComparator(null);
        cli.usage(usageStringBuilder, usageMessageFormatter);
        return UsageRender.render(usageStringBuilder.toString());
    }

    public String getTargetIp() {
        return targetIp;
    }

    public int getPort() {
        return port;
    }

    public String getCommand() {
        return command;
    }

    public String getBatchFile() {
        return batchFile;
    }

    public int getExecutionTimeout() {
        return executionTimeout;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getheight() {
        return height;
    }

    public boolean isHelp() {
        return help;
    }

}
