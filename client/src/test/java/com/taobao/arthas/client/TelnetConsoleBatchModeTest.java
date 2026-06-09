package com.taobao.arthas.client;

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
        private final String command;
        private final String quit;

        private ServerResult(String command, String quit) {
            this.command = command;
            this.quit = quit;
        }
    }
}
