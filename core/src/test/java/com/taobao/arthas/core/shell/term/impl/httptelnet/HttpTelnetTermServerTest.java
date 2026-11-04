package com.taobao.arthas.core.shell.term.impl.httptelnet;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.term.TermServer;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.telnet.TelnetConnection;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HttpTelnetTermServerTest {

    private static final int BINARY_OPTION = 0;
    private static final int NAWS_OPTION = 31;

    @Test
    void shouldRequestBinaryInBothDirections() throws Exception {
        int port = findFreePort();
        EventExecutorGroup workerGroup = new DefaultEventExecutorGroup(1);
        HttpTelnetTermServer server = new HttpTelnetTermServer(
                "127.0.0.1", port, 3000, workerGroup, new HttpSessionManager());
        server.termHandler(term -> {
        });
        AtomicReference<Future<TermServer>> listenResult = new AtomicReference<Future<TermServer>>();

        try {
            server.listen(listenResult::set);
            assertThat(listenResult.get()).isNotNull();
            assertThat(listenResult.get().succeeded()).isTrue();

            try (Socket socket = new Socket("127.0.0.1", port)) {
                socket.setSoTimeout(1000);
                OutputStream output = socket.getOutputStream();
                output.write(new byte[] {
                        TelnetConnection.BYTE_IAC, TelnetConnection.BYTE_WILL, (byte) NAWS_OPTION
                });
                output.flush();

                BinaryNegotiation negotiation = readBinaryNegotiation(socket.getInputStream(), output);

                assertThat(negotiation.serverRequestsBinaryInput).isTrue();
                assertThat(negotiation.serverOffersBinaryOutput).isTrue();
            }
        } finally {
            server.close(result -> {
            });
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }
    }

    private static BinaryNegotiation readBinaryNegotiation(InputStream input, OutputStream output)
            throws Exception {
        BinaryNegotiation result = new BinaryNegotiation();
        try {
            while (!result.complete()) {
                int value = input.read();
                if (value == -1) {
                    break;
                }
                if (value != (TelnetConnection.BYTE_IAC & 0xff)) {
                    continue;
                }
                int command = input.read();
                int option = input.read();
                if (option != BINARY_OPTION) {
                    continue;
                }
                if (command == (TelnetConnection.BYTE_DO & 0xff)) {
                    result.serverRequestsBinaryInput = true;
                    output.write(new byte[] {
                            TelnetConnection.BYTE_IAC,
                            TelnetConnection.BYTE_WILL,
                            (byte) BINARY_OPTION
                    });
                    output.flush();
                } else if (command == (TelnetConnection.BYTE_WILL & 0xff)) {
                    result.serverOffersBinaryOutput = true;
                    output.write(new byte[] {
                            TelnetConnection.BYTE_IAC,
                            TelnetConnection.BYTE_DO,
                            (byte) BINARY_OPTION
                    });
                    output.flush();
                }
            }
        } catch (SocketTimeoutException ignored) {
            // 由断言报告缺失的协商方向。
        }
        return result;
    }

    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static class BinaryNegotiation {
        private boolean serverRequestsBinaryInput;
        private boolean serverOffersBinaryOutput;

        private boolean complete() {
            return serverRequestsBinaryInput && serverOffersBinaryOutput;
        }
    }
}
