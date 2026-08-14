package com.taobao.arthas.client;

import org.apache.commons.net.telnet.TelnetCommand;
import org.apache.commons.net.telnet.TelnetOption;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TelnetConsoleBatchModeTest {

    @Test
    void quietBatchModeShouldSendCommandWhenPromptStartsTheStream() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Future<ServerResult> serverResult = executorService.submit(() -> runPromptFirstServer(serverSocket));

            int status = TelnetConsole.process(new String[] {
                    "--quiet",
                    "-c",
                    "version",
                    "-t",
                    "1000",
                    "127.0.0.1",
                    String.valueOf(serverSocket.getLocalPort())
            });

            ServerResult result = serverResult.get(2, TimeUnit.SECONDS);
            assertThat(status).isEqualTo(TelnetConsole.STATUS_OK);
            assertThat(result.command).contains("version | plaintext");
            assertThat(result.quit).isEqualTo("quit");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void batchModeShouldKeepBackgroundMarkerAfterPlaintextPipeline() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Future<ServerResult> serverResult = executorService.submit(() -> runPromptFirstServer(serverSocket));

            int status = TelnetConsole.process(new String[] {
                    "--quiet",
                    "-c",
                    "version &",
                    "-t",
                    "1000",
                    "127.0.0.1",
                    String.valueOf(serverSocket.getLocalPort())
            });

            ServerResult result = serverResult.get(2, TimeUnit.SECONDS);
            assertThat(status).isEqualTo(TelnetConsole.STATUS_OK);
            assertThat(result.command).contains("version | plaintext &");
            assertThat(result.quit).isEqualTo("quit");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void batchModeShouldNegotiateBinaryAndSendChineseCommandAsUtf8() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Future<BinaryServerResult> serverResult = executorService.submit(() -> runBinaryServer(serverSocket));

            int status = TelnetConsole.process(new String[] {
                    "--quiet",
                    "-c",
                    "echo 中文",
                    "-t",
                    "1000",
                    "127.0.0.1",
                    String.valueOf(serverSocket.getLocalPort())
            });

            BinaryServerResult result = serverResult.get(2, TimeUnit.SECONDS);
            assertThat(status).isEqualTo(TelnetConsole.STATUS_OK);
            assertThat(result.clientWillBinary).isTrue();
            assertThat(result.clientDoBinary).isTrue();
            assertThat(result.command).isEqualTo("echo 中文 | plaintext");
            assertThat(result.quit).isEqualTo("quit");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void batchModeShouldIgnorePromptRepaintWhileEnteringChineseCommand() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Future<PromptRepaintServerResult> serverResult =
                    executorService.submit(() -> runPromptRepaintServer(serverSocket));

            int status = TelnetConsole.process(new String[] {
                    "--quiet",
                    "-c",
                    "echo 中文",
                    "-t",
                    "2000",
                    "127.0.0.1",
                    String.valueOf(serverSocket.getLocalPort())
            });

            PromptRepaintServerResult result = serverResult.get(3, TimeUnit.SECONDS);
            assertThat(status).isEqualTo(TelnetConsole.STATUS_OK);
            assertThat(result.command).isEqualTo("echo 中文 | plaintext");
            assertThat(result.quitSentBeforeCommandCompleted).isFalse();
            assertThat(result.quit).isEqualTo("quit");
        } finally {
            executorService.shutdownNow();
        }
    }

    private static ServerResult runPromptFirstServer(ServerSocket serverSocket) throws IOException {
        try (Socket socket = serverSocket.accept()) {
            socket.setSoTimeout(2000);
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            write(outputStream, "[arthas@123]$ ");
            String command = readLine(inputStream);

            write(outputStream, "ok\n[arthas@123]$ ");
            String quit = readLine(inputStream);

            return new ServerResult(command, quit);
        }
    }

    private static BinaryServerResult runBinaryServer(ServerSocket serverSocket) throws IOException {
        try (Socket socket = serverSocket.accept()) {
            socket.setSoTimeout(2000);
            OutputStream outputStream = socket.getOutputStream();
            TelnetApplicationReader reader = new TelnetApplicationReader(socket.getInputStream());

            outputStream.write(new byte[] {
                    (byte) TelnetCommand.IAC, (byte) TelnetCommand.DO, (byte) TelnetOption.BINARY,
                    (byte) TelnetCommand.IAC, (byte) TelnetCommand.WILL, (byte) TelnetOption.BINARY
            });
            write(outputStream, "[arthas@123]$ ");
            String command = reader.readLine();

            write(outputStream, "ok\n[arthas@123]$ ");
            String quit = reader.readLine();

            return new BinaryServerResult(
                    reader.clientWillBinary, reader.clientDoBinary, command, quit);
        }
    }

    private static PromptRepaintServerResult runPromptRepaintServer(ServerSocket serverSocket) throws IOException {
        try (Socket socket = serverSocket.accept()) {
            socket.setSoTimeout(2000);
            OutputStream outputStream = socket.getOutputStream();
            TelnetApplicationReader reader = new TelnetApplicationReader(socket.getInputStream());

            outputStream.write(new byte[] {
                    (byte) TelnetCommand.IAC, (byte) TelnetCommand.DO, (byte) TelnetOption.BINARY,
                    (byte) TelnetCommand.IAC, (byte) TelnetCommand.WILL, (byte) TelnetOption.BINARY
            });
            write(outputStream, "[arthas@123]$ ");
            String command = reader.readLine();

            write(outputStream, "\r\033[K[arthas@123]$ echo 中");
            socket.setSoTimeout(250);
            String quit = null;
            boolean quitSentBeforeCommandCompleted = false;
            try {
                quit = reader.readLine();
                quitSentBeforeCommandCompleted = "quit".equals(quit);
            } catch (java.net.SocketTimeoutException ignored) {
                // 重绘中的 prompt 不应唤醒批处理命令边界。
            }

            write(outputStream, "文\r\nok\r\n[arthas@123]$ ");
            if (quit == null) {
                socket.setSoTimeout(2000);
                quit = reader.readLine();
            }

            return new PromptRepaintServerResult(
                    command, quit, quitSentBeforeCommandCompleted);
        }
    }

    private static void write(OutputStream outputStream, String value) throws IOException {
        outputStream.write(value.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private static String readLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int value;
        while ((value = inputStream.read()) != -1) {
            if (value == '\n') {
                break;
            }
            buffer.write(value);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8).replace("\r", "");
    }

    private static class ServerResult {
        final String command;
        final String quit;

        private ServerResult(String command, String quit) {
            this.command = command;
            this.quit = quit;
        }
    }

    private static class BinaryServerResult extends ServerResult {
        private final boolean clientWillBinary;
        private final boolean clientDoBinary;

        private BinaryServerResult(
                boolean clientWillBinary,
                boolean clientDoBinary,
                String command,
                String quit) {
            super(command, quit);
            this.clientWillBinary = clientWillBinary;
            this.clientDoBinary = clientDoBinary;
        }
    }

    private static class PromptRepaintServerResult extends ServerResult {
        private final boolean quitSentBeforeCommandCompleted;

        private PromptRepaintServerResult(
                String command,
                String quit,
                boolean quitSentBeforeCommandCompleted) {
            super(command, quit);
            this.quitSentBeforeCommandCompleted = quitSentBeforeCommandCompleted;
        }
    }

    private static class TelnetApplicationReader {
        private final InputStream inputStream;
        private boolean clientWillBinary;
        private boolean clientDoBinary;

        private TelnetApplicationReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        private String readLine() throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int value;
            while ((value = inputStream.read()) != -1) {
                if (value == TelnetCommand.IAC) {
                    readTelnetCommand(buffer);
                } else if (value == '\n') {
                    break;
                } else if (value != '\r' && value != 0) {
                    buffer.write(value);
                }
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }

        private void readTelnetCommand(ByteArrayOutputStream buffer) throws IOException {
            int command = inputStream.read();
            if (command == TelnetCommand.IAC) {
                buffer.write(TelnetCommand.IAC);
            } else if (command == TelnetCommand.DO
                    || command == TelnetCommand.DONT
                    || command == TelnetCommand.WILL
                    || command == TelnetCommand.WONT) {
                int option = inputStream.read();
                if (option == TelnetOption.BINARY) {
                    clientWillBinary |= command == TelnetCommand.WILL;
                    clientDoBinary |= command == TelnetCommand.DO;
                }
            } else if (command == TelnetCommand.SB) {
                skipSubnegotiation();
            }
        }

        private void skipSubnegotiation() throws IOException {
            int previous = -1;
            int value;
            while ((value = inputStream.read()) != -1) {
                if (previous == TelnetCommand.IAC && value == TelnetCommand.SE) {
                    return;
                }
                previous = value;
            }
        }
    }
}
