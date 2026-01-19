package com.taobao.arthas.mcp.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.spec.HttpHeaders;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ArthasMcpToolsIT {

    private static final ObjectMapper OBJECT_MAPPER = JsonParser.getObjectMapper();

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void should_list_tools_and_call_tool_via_mcp() throws Exception {
        Assumptions.assumeFalse(isWindows(), "集成测试依赖 bash/as.sh，Windows 环境跳过");

        Path arthasHome = resolveArthasBinDir();
        assertThat(arthasHome).isDirectory();
        assertThat(arthasHome.resolve("as.sh")).exists();
        assertThat(arthasHome.resolve("arthas-core.jar")).exists();
        assertThat(arthasHome.resolve("arthas-agent.jar")).exists();

        int telnetPort = 0;
        int httpPort = findFreePort();

        Process targetJvm = null;
        Path tempHome = null;
        try {
            tempHome = Files.createTempDirectory("arthas-mcp-it-home");

            Path targetLog = tempHome.resolve("target-jvm.log");
            targetJvm = startTargetJvm(targetLog);
            long targetPid = ProcessPid.pidOf(targetJvm);

            Path attachLog = tempHome.resolve("attach.log");
            runAttach(arthasHome, tempHome, attachLog, targetPid, telnetPort, httpPort);

            waitForPortOpen("127.0.0.1", httpPort, Duration.ofSeconds(30));

            StreamableMcpHttpClient client = new StreamableMcpHttpClient("127.0.0.1", httpPort, "/mcp");
            String sessionId = retry(Duration.ofSeconds(30), client::initialize);
            client.sendInitializedNotification(sessionId);

            McpSchema.ListToolsResult toolsResult = client.listTools(sessionId);
            assertThat(toolsResult.getTools()).isNotNull();
            assertThat(toolsResult.getTools().size()).isGreaterThanOrEqualTo(10);

            Set<String> toolNames = new HashSet<>();
            for (McpSchema.Tool tool : toolsResult.getTools()) {
                toolNames.add(tool.getName());
            }
            assertThat(toolNames).contains("jvm", "jad", "thread");

            McpSchema.CallToolResult callToolResult = client.callTool(sessionId, "jvm", Collections.emptyMap());
            assertThat(callToolResult).isNotNull();
            assertThat(callToolResult.getIsError()).isNotEqualTo(Boolean.TRUE);
            assertThat(callToolResult.getContent()).isNotNull().isNotEmpty();
        } finally {
            if (targetJvm != null) {
                targetJvm.destroy();
                if (!targetJvm.waitFor(5, TimeUnit.SECONDS)) {
                    targetJvm.destroyForcibly();
                }
            }
            if (tempHome != null) {
                deleteDirectoryQuietly(tempHome);
            }
        }
    }

    private static String retry(Duration timeout, IoSupplier<String> supplier) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        Exception last = null;
        while (System.nanoTime() < deadline) {
            try {
                return supplier.get();
            } catch (Exception e) {
                last = e;
                Thread.sleep(200);
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("重试超时");
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws Exception;
    }

    private static void runAttach(Path arthasHome, Path tempHome, Path attachLog, long targetPid,
                                  int telnetPort, int httpPort) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add(arthasHome.resolve("as.sh").toString());
        command.add("--attach-only");
        command.add("--arthas-home");
        command.add(arthasHome.toString());
        command.add("--target-ip");
        command.add("127.0.0.1");
        command.add("--telnet-port");
        command.add(String.valueOf(telnetPort));
        command.add("--http-port");
        command.add(String.valueOf(httpPort));
        command.add(String.valueOf(targetPid));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.redirectOutput(attachLog.toFile());
        Map<String, String> env = pb.environment();
        env.put("JAVA_HOME", System.getProperty("java.home"));
        env.put("HOME", tempHome.toAbsolutePath().toString());

        Process attach = pb.start();
        if (!attach.waitFor(90, TimeUnit.SECONDS)) {
            attach.destroyForcibly();
            Assertions.fail("as.sh attach 超时: " + attachLog);
        }
        if (attach.exitValue() != 0) {
            Assertions.fail("as.sh attach 失败(exit=" + attach.exitValue() + "): " + attachLog);
        }
    }

    private static Process startTargetJvm(Path targetLog) throws IOException {
        String javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = Paths.get(System.getProperty("basedir"), "target", "test-classes").toString();
        ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", classpath, TargetJvmApp.class.getName());
        pb.redirectErrorStream(true);
        pb.redirectOutput(targetLog.toFile());
        return pb.start();
    }

    private static Path resolveArthasBinDir() {
        String basedir = System.getProperty("basedir");
        assertThat(basedir).as("Maven surefire/failsafe should set system property 'basedir'").isNotBlank();
        return Paths.get(basedir).resolve("../packaging/target/arthas-bin").normalize();
    }

    private static int findFreePort() throws IOException {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static void waitForPortOpen(String host, int port, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 500);
                return;
            } catch (IOException ignored) {
                Thread.sleep(200);
            }
        }
        throw new IllegalStateException("等待端口监听超时: " + host + ":" + port);
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(Locale.ROOT).contains("win");
    }

    private static void deleteDirectoryQuietly(Path dir) {
        try {
            if (!Files.exists(dir)) {
                return;
            }
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {
        }
    }

    private static final class ProcessPid {
        private ProcessPid() {
        }

        static long pidOf(Process process) {
            // Java 9+
            try {
                return (Long) Process.class.getMethod("pid").invoke(process);
            } catch (Exception ignored) {
                // Java 8 fallback
            }
            try {
                java.lang.reflect.Field pidField = process.getClass().getDeclaredField("pid");
                pidField.setAccessible(true);
                Object value = pidField.get(process);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                throw new IllegalStateException("Unsupported pid field type: " + value);
            } catch (Exception e) {
                throw new IllegalStateException("无法获取目标 JVM pid", e);
            }
        }
    }

    private static final class StreamableMcpHttpClient {
        private final String baseUrl;
        private final String mcpEndpoint;

        StreamableMcpHttpClient(String host, int port, String mcpEndpoint) {
            this.baseUrl = "http://" + host + ":" + port;
            this.mcpEndpoint = mcpEndpoint;
        }

        String initialize() throws Exception {
            McpSchema.InitializeRequest init = new McpSchema.InitializeRequest(
                    McpSchema.LATEST_PROTOCOL_VERSION,
                    new McpSchema.ClientCapabilities(null, null, null, null),
                    new McpSchema.Implementation("arthas-mcp-it", "1.0.0")
            );
            McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest(
                    McpSchema.JSONRPC_VERSION,
                    McpSchema.METHOD_INITIALIZE,
                    1,
                    init
            );

            HttpURLConnection conn = openPostConnection(null);
            writeJson(conn, request);

            int code = conn.getResponseCode();
            String body = readBody(conn);
            if (code != 200) {
                throw new IllegalStateException("initialize 失败: http=" + code + ", body=" + body);
            }

            String sessionId = conn.getHeaderField(HttpHeaders.MCP_SESSION_ID);
            if (sessionId == null || sessionId.trim().isEmpty()) {
                throw new IllegalStateException("initialize 未返回 mcp-session-id header, body=" + body);
            }

            McpSchema.JSONRPCMessage msg = McpSchema.deserializeJsonRpcMessage(OBJECT_MAPPER, body);
            if (!(msg instanceof McpSchema.JSONRPCResponse)) {
                throw new IllegalStateException("initialize 响应不是 JSONRPCResponse: " + body);
            }
            McpSchema.JSONRPCResponse resp = (McpSchema.JSONRPCResponse) msg;
            if (resp.getError() != null) {
                throw new IllegalStateException("initialize 返回 error: " + OBJECT_MAPPER.writeValueAsString(resp.getError()));
            }
            return sessionId;
        }

        void sendInitializedNotification(String sessionId) throws Exception {
            McpSchema.JSONRPCNotification notification = new McpSchema.JSONRPCNotification(
                    McpSchema.JSONRPC_VERSION,
                    McpSchema.METHOD_NOTIFICATION_INITIALIZED,
                    Collections.emptyMap()
            );
            HttpURLConnection conn = openPostConnection(sessionId);
            writeJson(conn, notification);
            int code = conn.getResponseCode();
            if (code != 202 && code != 200) {
                throw new IllegalStateException("notifications/initialized 失败: http=" + code + ", body=" + readBody(conn));
            }
        }

        McpSchema.ListToolsResult listTools(String sessionId) throws Exception {
            McpSchema.PaginatedRequest params = new McpSchema.PaginatedRequest(null);
            McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest(
                    McpSchema.JSONRPC_VERSION,
                    McpSchema.METHOD_TOOLS_LIST,
                    2,
                    params
            );
            McpSchema.JSONRPCResponse response = postRequestExpectSseResponse(sessionId, request, Duration.ofSeconds(30));
            if (response.getError() != null) {
                throw new IllegalStateException("tools/list 返回 error: " + OBJECT_MAPPER.writeValueAsString(response.getError()));
            }
            return OBJECT_MAPPER.convertValue(response.getResult(), McpSchema.ListToolsResult.class);
        }

        McpSchema.CallToolResult callTool(String sessionId, String toolName, Map<String, Object> arguments) throws Exception {
            McpSchema.CallToolRequest params = new McpSchema.CallToolRequest(toolName, arguments, null);
            McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest(
                    McpSchema.JSONRPC_VERSION,
                    McpSchema.METHOD_TOOLS_CALL,
                    3,
                    params
            );
            McpSchema.JSONRPCResponse response = postRequestExpectSseResponse(sessionId, request, Duration.ofSeconds(60));
            if (response.getError() != null) {
                throw new IllegalStateException("tools/call 返回 error: " + OBJECT_MAPPER.writeValueAsString(response.getError()));
            }
            return OBJECT_MAPPER.convertValue(response.getResult(), McpSchema.CallToolResult.class);
        }

        private HttpURLConnection openPostConnection(String sessionId) throws IOException {
            URL url = new URL(baseUrl + mcpEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(30_000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "text/event-stream, application/json");
            if (sessionId != null) {
                conn.setRequestProperty(HttpHeaders.MCP_SESSION_ID, sessionId);
            }
            return conn;
        }

        private static void writeJson(HttpURLConnection conn, Object body) throws IOException {
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(body);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }
        }

        private static String readBody(HttpURLConnection conn) throws IOException {
            InputStream is = null;
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                is = conn.getErrorStream();
            }
            if (is == null) {
                return "";
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        }

        private McpSchema.JSONRPCResponse postRequestExpectSseResponse(String sessionId, McpSchema.JSONRPCRequest request, Duration timeout)
                throws Exception {
            HttpURLConnection conn = openPostConnection(sessionId);
            conn.setReadTimeout((int) timeout.toMillis());
            writeJson(conn, request);

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IllegalStateException(request.getMethod() + " 失败: http=" + code + ", body=" + readBody(conn));
            }

            try (InputStream is = conn.getInputStream()) {
                return readJsonRpcResponseFromSse(is, request.getId());
            }
        }

        private static McpSchema.JSONRPCResponse readJsonRpcResponseFromSse(InputStream inputStream, Object expectedId) throws Exception {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            String data = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    data = line.substring("data:".length()).trim();
                    continue;
                }
                if (line.isEmpty() && data != null) {
                    McpSchema.JSONRPCMessage msg = McpSchema.deserializeJsonRpcMessage(OBJECT_MAPPER, data);
                    data = null;
                    if (msg instanceof McpSchema.JSONRPCResponse) {
                        McpSchema.JSONRPCResponse resp = (McpSchema.JSONRPCResponse) msg;
                        if (Objects.equals(resp.getId(), expectedId)) {
                            return resp;
                        }
                    }
                }
            }
            throw new IllegalStateException("未从 SSE 流中读取到期望的 JSONRPCResponse, id=" + expectedId);
        }
    }
}
