package com.taobao.arthas.client;

import com.taobao.middleware.cli.Argument;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CLIs;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.Option;
import com.taobao.middleware.cli.TypedOption;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.WindowSizeOptionHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ralf0131 2016-12-29 11:55.
 */
public class TelnetConsole{

    private static final String PROMPT = "$";
    private static final String DEFAULT_TELNET_PORT = "3658";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000; // 5000 ms
    private static final String DEFAULT_WINDOW_WIDTH = "120";
    private static final String DEFAULT_WINDOW_HEIGHT = "40";
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private TelnetClient telnet;
    private String address;
    private int port;
    private InputStream in;
    private PrintStream out;

    public TelnetConsole(String address, int port, int width, int height) {
        this.telnet = new TelnetClient();
        this.address = address;
        this.port = port;
        try {
            telnet.addOptionHandler(new WindowSizeOptionHandler(width, height, true, false, true, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
        telnet.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
    }

    public void connect() {
        try {
            // Connect to the specified server
            telnet.connect(address, port);
            // Get input and output stream references
            in = telnet.getInputStream();
            out = new PrintStream(telnet.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readUntil(String prompt) {
        try {
            StringBuilder sBuffer = new StringBuilder();
            byte[] b = new byte[DEFAULT_BUFFER_SIZE];
            while(true) {
                int size = in.read(b);
                if(-1 != size) {
                    sBuffer.append(new String(b,0,size));
                    String data = sBuffer.toString();
                    if(data.trim().endsWith(prompt)) {
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

    public String readUntilPrompt() {
        return readUntil(PROMPT);
    }

    public void write(String value) {
        try {
            out.println(value);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendCommand(String command) {
        try {
            write(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            telnet.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 批处理模式
     */
    public void batchModeRun(File batchFile) {
        if (batchFile == null || !batchFile.exists()) {
            return;
        }
        batchModeRun(readLines(batchFile));
    }

    private void batchModeRun(List<String> commands) {
        for (String command: commands) {
            // send command to server
            sendCommand(command + " | plaintext");
            // read result from server and output
            String response = readUntilPrompt();
            System.out.print(response);
        }
    }

    private List<String> readLines(File batchFile) {
        List<String> list = new ArrayList<String>();
        BufferedReader br = null;
        try {
            br =  new BufferedReader(new FileReader(batchFile));
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

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.err.println("Usage: TelnetConsole <target-ip> [-p PORT] [-c COMMAND] [-f BATCH_FILE] [-w WIDTH] [-h HEIGHT]");
                System.exit(1);
            }

            CommandLine commandLine = parseArguments(args);

            TelnetConsole console = new TelnetConsole(
                    (String)commandLine.getArgumentValue("target-ip"),
                    (Integer)commandLine.getOptionValue("p"),
                    (Integer)commandLine.getOptionValue("w"),
                    (Integer)commandLine.getOptionValue("h"));

            console.connect();
            String logo = console.readUntilPrompt();
            System.out.print(logo);

            String cmd = commandLine.getOptionValue("c");
            if (cmd != null) {
                List<String> cmds = new ArrayList<String>();
                for (String c: cmd.split(";")) {
                    cmds.add(c.trim());
                }
                console.batchModeRun(cmds);
            }

            String filePath = commandLine.getOptionValue("f");
            if (filePath != null) {
                File batchFile = new File(filePath);
                console.batchModeRun(batchFile);
            }

            console.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static CommandLine parseArguments(String[] args) {
        Argument addr = new Argument().setArgName("target-ip").setIndex(0).setRequired(true);
        Option port = new TypedOption<Integer>().setType(Integer.class).setShortName("p")
                .setDefaultValue(DEFAULT_TELNET_PORT);
        Option command = new TypedOption<String>().setType(String.class).setShortName("c");
        Option batchFileOption = new TypedOption<String>().setType(String.class).setShortName("f");
        Option width = new TypedOption<Integer>().setType(Integer.class).setShortName("w")
                .setDefaultValue(DEFAULT_WINDOW_WIDTH);
        Option height = new TypedOption<Integer>().setType(Integer.class).setShortName("h")
                .setDefaultValue(DEFAULT_WINDOW_HEIGHT);
        CLI cli = CLIs.create("TelnetConsole").addArgument(addr).addOption(port)
                .addOption(command).addOption(batchFileOption).addOption(width).addOption(height);
        return cli.parse(Arrays.asList(args));
    }

}
