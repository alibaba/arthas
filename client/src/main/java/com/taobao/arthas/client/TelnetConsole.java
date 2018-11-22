package com.taobao.arthas.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private static final String PROMPT = "$";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000; // 5000 ms
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private static final byte CTRL_C = 0x03;

    private boolean help = false;

    private String targetIp = "127.0.0.1";
    private int port = 3658;

    private String command;
    private String batchFile;

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

    private static String readUntil(InputStream in, String prompt) {
        try {
            StringBuilder sBuffer = new StringBuilder();
            byte[] b = new byte[DEFAULT_BUFFER_SIZE];
            while (true) {
                int size = in.read(b);
                if (-1 != size) {
                    sBuffer.append(new String(b, 0, size));
                    String data = sBuffer.toString();
                    if (data.trim().endsWith(prompt)) {
                        break;
                    }
                }
            }
            return sBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

    public static void main(String[] args) throws IOException {
        // support mingw/cygw jline color
        if (OSUtils.isCygwinOrMinGW()) {
            System.setProperty("jline.terminal", System.getProperty("jline.terminal", "jline.UnixTerminal"));
        }

        TelnetConsole telnetConsole = new TelnetConsole();

        CLI cli = CLIConfigurator.define(TelnetConsole.class);

        try {
            CommandLine commandLine = cli.parse(Arrays.asList(args));

            CLIConfigurator.inject(commandLine, telnetConsole);

            if (telnetConsole.isHelp()) {
                System.out.println(usage(cli));
                System.exit(0);
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

            if (terminal instanceof TerminalSupport) {
                ((TerminalSupport) terminal).disableInterruptCharacter();
            }

            // support catch ctrl+c event
            terminal.disableInterruptCharacter();
            if (terminal instanceof UnixTerminal) {
                ((UnixTerminal) terminal).disableLitteralNextCharacter();
            }

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
            consoleReader.getKeys().bind(new Character(KeyMap.CTRL_D).toString(), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });

            try {
                telnet.connect(telnetConsole.getTargetIp(), telnetConsole.getPort());
            } catch (IOException e) {
                System.out.println("Connect to telnet server error: " + telnetConsole.getTargetIp() + " "
                                + telnetConsole.getPort());
                throw e;
            }

            if (cmds.isEmpty()) {
                IOUtil.readWrite(telnet.getInputStream(), telnet.getOutputStream(), System.in,
                                consoleReader.getOutput());
            } else {
                batchModeRun(telnet, cmds);
                telnet.disconnect();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println(usage(cli));
            System.exit(1);
        }

    }

    private static void batchModeRun(TelnetClient telnet, List<String> commands) throws IOException {
        InputStream inputStream = telnet.getInputStream();
        OutputStream outputStream = telnet.getOutputStream();

        for (String command : commands) {
            if (command.trim().isEmpty()) {
                continue;
            }
            // send command to server
            outputStream.write((command + " | plaintext\n").getBytes());
            outputStream.flush();
            // read result from server and output
            String response = readUntil(inputStream, PROMPT);
            System.out.print(response);
            System.out.flush();
        }
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
