package com.taobao.arthas.mcp.it.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.it.TargetJvmApp;
import com.taobao.arthas.mcp.server.protocol.spec.HttpHeaders;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.util.JsonParser;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

final class ArthasMcpTaskTestSupport {

    static final ObjectMapper OBJECT_MAPPER = JsonParser.getObjectMapper();

    static final String TARGET_CLASS_PATTERN = TargetJvmApp.class.getName();

    static final String TARGET_METHOD_PATTERN = "hotMethod";

    private ArthasMcpTaskTestSupport() {
    }

    static void assumeSupportedPlatform() {
        Assumptions.assumeFalse(isWindows(), "集成测试依赖 bash/as.sh，Windows 环境跳过");
    }

    static Map<String, Object> createWatchArguments(int numberOfExecutions, int timeoutSeconds) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classPattern", TARGET_CLASS_PATTERN);
        args.put("methodPattern", TARGET_METHOD_PATTERN);
        args.put("numberOfExecutions", numberOfExecutions);
        args.put("timeout", timeoutSeconds);
        return args;
    }

    static Map<String, Object> createWatchArguments(String methodPattern, int numberOfExecutions, int timeoutSeconds) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classPattern", TARGET_CLASS_PATTERN);
        args.put("methodPattern", methodPattern);
        args.put("numberOfExecutions", numberOfExecutions);
        args.put("timeout", timeoutSeconds);
        return args;
    }

    static Map<String, Object> createTraceArguments(String methodPattern, int numberOfExecutions, int timeoutSeconds) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classPattern", TARGET_CLASS_PATTERN);
        args.put("methodPattern", methodPattern);
        args.put("numberOfExecutions", numberOfExecutions);
        args.put("timeout", timeoutSeconds);
        return args;
    }

    static Map<String, Object> createStackArguments(String methodPattern, int numberOfExecutions, int timeoutSeconds) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classPattern", TARGET_CLASS_PATTERN);
        args.put("methodPattern", methodPattern);
        args.put("numberOfExecutions", numberOfExecutions);
        args.put("timeout", timeoutSeconds);
        return args;
    }

    static void assertTaskSupportMode(McpSchema.ListToolsResult toolsResult, String toolName,
                                      McpSchema.TaskSupportMode expectedMode) {
        McpSchema.Tool tool = toolsResult.getTools().stream()
                .filter(item -> toolName.equals(item.getName()))
                .findFirst()
                .orElse(null);
        assertThat(tool).as("tool=%s should exist", toolName).isNotNull();
        assertThat(tool.getExecution()).as("tool=%s execution", toolName).isNotNull();
        assertThat(tool.getExecution().getTaskSupport()).as("tool=%s taskSupport", toolName).isEqualTo(expectedMode);
    }

    static McpSchema.GetTaskResult waitForTaskStatus(StreamableMcpHttpClient client, String sessionId, String taskId,
                                                     Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        Exception last = null;
        while (System.nanoTime() < deadline) {
            try {
                McpSchema.GetTaskResult task = client.getTask(sessionId, taskId);
                if (task != null && task.getStatus() != null && task.getStatus().isTerminal()) {
                    return task;
                }
                last = null;
            } catch (Exception e) {
                last = e;
            }
            Thread.sleep(200);
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("等待任务进入终态超时: taskId=" + taskId);
    }

    static void waitForTaskExpiration(StreamableMcpHttpClient client, String sessionId, String taskId, Duration timeout)
            throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                client.getTask(sessionId, taskId);
            } catch (IllegalStateException e) {
                if (e.getMessage() != null && e.getMessage().contains("Task not found")) {
                    return;
                }
                throw e;
            }
            Thread.sleep(1_000L);
        }
        throw new IllegalStateException("等待任务过期超时: taskId=" + taskId);
    }

    static String assertCallToolSuccess(String toolName, McpSchema.CallToolResult result) throws Exception {
        assertThat(result).as("tool=%s", toolName).isNotNull();
        assertThat(result.getIsError()).as("tool=%s, content=%s", toolName, result.getContent())
                .isNotEqualTo(Boolean.TRUE);
        assertThat(result.getContent()).as("tool=%s", toolName).isNotNull().isNotEmpty();

        String text = extractTextContent(result);
        assertThat(text).as("tool=%s", toolName).isNotBlank();

        JsonNode node = OBJECT_MAPPER.readTree(text);
        if (node != null && node.isObject()) {
            JsonNode error = node.get("error");
            if (error != null && error.isBoolean() && error.booleanValue()) {
                Assertions.fail("tool 执行返回 error=true: tool=" + toolName + ", body=" + text);
            }
            JsonNode status = node.get("status");
            if (status != null && status.isTextual() && "error".equalsIgnoreCase(status.asText())) {
                Assertions.fail("tool 执行返回 status=error: tool=" + toolName + ", body=" + text);
            }
        }

        return text;
    }

    static void assertStreamableResultCountGreaterThanZero(String toolName, String body) throws Exception {
        JsonNode node = OBJECT_MAPPER.readTree(body);
        if (node != null && node.isObject()) {
            JsonNode resultCount = node.get("resultCount");
            if (resultCount != null && resultCount.canConvertToInt()) {
                assertThat(resultCount.asInt()).as("tool=%s resultCount, body=%s", toolName, body).isGreaterThan(0);
                return;
            }
        }
        Assertions.fail("streamable tool 未返回 resultCount: tool=" + toolName + ", body=" + body);
    }

    static String extractTextContent(McpSchema.CallToolResult result) {
        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : result.getContent()) {
            if (content instanceof McpSchema.TextContent) {
                String text = ((McpSchema.TextContent) content).getText();
                if (text != null) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(text);
                }
            }
        }
        return sb.toString();
    }

    static String createTaskAndGetId(StreamableMcpHttpClient client, String sessionId, int numberOfExecutions,
                                     int timeoutSeconds) throws Exception {
        return createTaskAndGetId(client, sessionId, "watch",
                createWatchArguments(numberOfExecutions, timeoutSeconds), 60_000L);
    }

    static String createTaskAndGetId(StreamableMcpHttpClient client, String sessionId, String toolName,
                                     Map<String, Object> arguments, Long ttl) throws Exception {
        McpSchema.CreateTaskResult createTaskResult = client.createTask(sessionId, toolName, arguments, ttl);
        assertThat(createTaskResult).isNotNull();
        assertThat(createTaskResult.getTask()).isNotNull();
        assertThat(createTaskResult.getTask().getTaskId()).isNotBlank();
        return createTaskResult.getTask().getTaskId();
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(Locale.ROOT).contains("win");
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

    private static void waitForMcpEndpointReady(String host, int port, String endpoint, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                URL url = new URL("http://" + host + ":" + port + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(2_000);
                conn.setReadTimeout(5_000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json, text/event-stream");
                // Minimal valid JSON-RPC initialize request
                byte[] body = ("{\"jsonrpc\":\"2.0\",\"id\":0,\"method\":\"initialize\","
                        + "\"params\":{\"protocolVersion\":\"2025-11-25\","
                        + "\"capabilities\":{},"
                        + "\"clientInfo\":{\"name\":\"probe\",\"version\":\"0\"}}}").getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(body.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body);
                }
                int code = conn.getResponseCode();
                if (code == 200) {
                    // drain response to avoid connection leak
                    try (InputStream is = conn.getInputStream()) {
                        byte[] buf = new byte[4096];
                        while (is.read(buf) != -1) { /* drain */ }
                    } catch (IOException ignored) {
                    }
                    return;
                }
            } catch (IOException ignored) {
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("等待 MCP endpoint 就绪超时: http://" + host + ":" + port + endpoint);
    }

    private static void waitForPortClosed(String host, int port, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 300);
                // port still open, keep waiting
            } catch (IOException e) {
                // connection refused or reset — port is closed
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        // timed out waiting — proceed anyway
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

    private static Path resolveArthasBinDir() {
        String basedir = System.getProperty("basedir");
        assertThat(basedir).as("Maven surefire/failsafe should set system property 'basedir'").isNotBlank();
        return Paths.get(basedir).resolve("../packaging/target/arthas-bin").normalize();
    }

    private static Process startTargetJvm(Path workDir, Path targetLog) throws IOException {
        String javaBin = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = Paths.get(System.getProperty("basedir"), "target", "test-classes").toString();
        ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", classpath, TargetJvmApp.class.getName());
        if (workDir != null) {
            pb.directory(workDir.toFile());
        }
        pb.redirectErrorStream(true);
        pb.redirectOutput(targetLog.toFile());
        return pb.start();
    }

    private static void runAttach(Path arthasHome, Path tempHome, Path attachLog, long targetPid,
                                  int telnetPort, int httpPort) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                arthasHome.resolve("as.sh").toString(),
                "--attach-only",
                "--arthas-home", arthasHome.toString(),
                "--target-ip", "127.0.0.1",
                "--telnet-port", String.valueOf(telnetPort),
                "--http-port", String.valueOf(httpPort),
                String.valueOf(targetPid)
        );
        pb.redirectErrorStream(true);
        pb.redirectOutput(attachLog.toFile());
        pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
        pb.environment().put("HOME", tempHome.toAbsolutePath().toString());

        Process attach = pb.start();
        if (!attach.waitFor(90, TimeUnit.SECONDS)) {
            attach.destroyForcibly();
            Assertions.fail("as.sh attach 超时: " + attachLog);
        }
        if (attach.exitValue() != 0) {
            Assertions.fail("as.sh attach 失败(exit=" + attach.exitValue() + "): " + attachLog);
        }
    }

    static final class Environment implements AutoCloseable {
        final Path tempHome;
        final int httpPort;
        final Process targetJvm;

        private Environment(Path tempHome, int httpPort, Process targetJvm) {
            this.tempHome = tempHome;
            this.httpPort = httpPort;
            this.targetJvm = targetJvm;
        }

        static Environment start(String tempDirPrefix) throws Exception {
            Path arthasHome = resolveArthasBinDir();
            assertThat(arthasHome).isDirectory();
            assertThat(arthasHome.resolve("as.sh")).exists();
            assertThat(arthasHome.resolve("arthas-core.jar")).exists();
            assertThat(arthasHome.resolve("arthas-agent.jar")).exists();

            int telnetPort = 0;
            int httpPort = findFreePort();
            Path tempHome = Files.createTempDirectory(tempDirPrefix);
            Process targetJvm = null;
            try {
                Path targetLog = tempHome.resolve("target-jvm.log");
                targetJvm = startTargetJvm(tempHome, targetLog);
                long targetPid = ProcessPid.pidOf(targetJvm);

                Path attachLog = tempHome.resolve("attach.log");
                runAttach(arthasHome, tempHome, attachLog, targetPid, telnetPort, httpPort);

                waitForPortOpen("127.0.0.1", httpPort, Duration.ofSeconds(30));
                waitForMcpEndpointReady("127.0.0.1", httpPort, "/mcp", Duration.ofSeconds(30));
                // 等待服务端内部状态完全稳定（CI 环境资源紧张时需要额外时间）
                Thread.sleep(3_000L);
                return new Environment(tempHome, httpPort, targetJvm);
            } catch (Exception e) {
                if (targetJvm != null) {
                    targetJvm.destroy();
                    if (!targetJvm.waitFor(5, TimeUnit.SECONDS)) {
                        targetJvm.destroyForcibly();
                    }
                }
                deleteDirectoryQuietly(tempHome);
                throw e;
            }
        }

        @Override
        public void close() {
            if (this.targetJvm != null) {
                this.targetJvm.destroy();
                try {
                    if (!this.targetJvm.waitFor(10, TimeUnit.SECONDS)) {
                        this.targetJvm.destroyForcibly();
                        this.targetJvm.waitFor(5, TimeUnit.SECONDS);
                    }
                } catch (Exception ignored) {
                }
            }
            // 增加端口关闭等待时间 (原来 15 秒)
            waitForPortClosed("127.0.0.1", this.httpPort, Duration.ofSeconds(30));
            // 增加延迟以确保 OS 端口完全释放 (原来 2 秒)
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            deleteDirectoryQuietly(this.tempHome);
        }
    }

    private static final class ProcessPid {
        private ProcessPid() {
        }

        static long pidOf(Process process) {
            try {
                return (Long) Process.class.getMethod("pid").invoke(process);
            } catch (Exception ignored) {
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

    static final class InitializedSession {
        final String sessionId;
        final McpSchema.InitializeResult initializeResult;

        private InitializedSession(String sessionId, McpSchema.InitializeResult initializeResult) {
            this.sessionId = sessionId;
            this.initializeResult = initializeResult;
        }
    }

    static final class StreamableMcpHttpClient {
        private final String baseUrl;
        private final String mcpEndpoint;
        private final AtomicInteger nextRequestId = new AtomicInteger(1);

        StreamableMcpHttpClient(String host, int port, String mcpEndpoint) {
            this.baseUrl = "http://" + host + ":" + port;
            this.mcpEndpoint = mcpEndpoint;
        }

        InitializedSession initializeSession(String clientName) throws Exception {
            int maxAttempts = 5;
            IOException lastIoException = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return doInitializeSession(clientName);
                } catch (IOException e) {
                    lastIoException = e;
                    if (attempt < maxAttempts) {
                        Thread.sleep(1_000L * attempt);
                    }
                }
            }
            throw new IllegalStateException("initialize 重试 " + maxAttempts + " 次后仍然失败", lastIoException);
        }

        private InitializedSession doInitializeSession(String clientName) throws Exception {
            McpSchema.InitializeRequest init = new McpSchema.InitializeRequest(
                    McpSchema.LATEST_PROTOCOL_VERSION,
                    new McpSchema.ClientCapabilities(null, null, null, null),
                    new McpSchema.Implementation(clientName, "1.0.0")
            );
            McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest(
                    McpSchema.JSONRPC_VERSION,
                    McpSchema.METHOD_INITIALIZE,
                    nextRequestId.getAndIncrement(),
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

            McpSchema.InitializeResult initializeResult = OBJECT_MAPPER.convertValue(resp.getResult(), McpSchema.InitializeResult.class);
            sendInitializedNotification(sessionId);
            return new InitializedSession(sessionId, initializeResult);
        }

        McpSchema.ListToolsResult listTools(String sessionId) throws Exception {
            McpSchema.JSONRPCResponse response = postRequestExpectSseResponse(
                    sessionId,
                    McpSchema.METHOD_TOOLS_LIST,
                    new McpSchema.PaginatedRequest(null),
                    Duration.ofSeconds(30)
            );
            ensureNoError(response, McpSchema.METHOD_TOOLS_LIST);
            return OBJECT_MAPPER.convertValue(response.getResult(), McpSchema.ListToolsResult.class);
        }

        McpSchema.CreateTaskResult createTask(String sessionId, String toolName, Map<String, Object> arguments, Long ttl)
                throws Exception {
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(
                    toolName,
                    arguments,
                    null,
                    new McpSchema.TaskMetadata(ttl)
            );
            McpSchema.JSONRPCResponse response = postRequestExpectSseResponse(
                    sessionId,
                    McpSchema.METHOD_TOOLS_CALL,
                    request,
                    Duration.ofSeconds(30)
            );
            ensureNoError(response, "tools/call(task)");
            return OBJECT_MAPPER.convertValue(response.getResult(), McpSchema.CreateTaskResult.class);
        }

        McpSchema.GetTaskResult getTask(String sessionId, String taskId) throws Exception {
            McpSchema.JSONRPCResponse response = postRequestExpectSseResponse(
                    sessionId,
                    McpSchema.METHOD_TASKS_GET,
                    new McpSchema.GetTaskRequest(taskId, null),
                    Duration.ofSeconds(30)
            );
            ensureNoError(response, McpSchema.METHOD_TASKS_GET);
            return OBJECT_MAPPER.convertValue(response.getResult(), McpSchema.GetTaskResult.class);
        }

        McpSchema.CallToolResult getTaskResult(String sessionId, String taskId, Duration timeout) throws Exception {
            McpSchema.JSONRPCResponse response = postRequestExpectSseResponse(
                    sessionId,
                    McpSchema.METHOD_TASKS_RESULT,
                    new McpSchema.GetTaskPayloadRequest(taskId, null),
                    timeout
            );
            ensureNoError(response, McpSchema.METHOD_TASKS_RESULT);
            return OBJECT_MAPPER.convertValue(response.getResult(), McpSchema.CallToolResult.class);
        }

        McpSchema.ListTasksResult listTasks(String sessionId) throws Exception {
            McpSchema.JSONRPCResponse response = postRequestExpectSseResponse(
                    sessionId,
                    McpSchema.METHOD_TASKS_LIST,
                    new McpSchema.PaginatedRequest(null),
                    Duration.ofSeconds(30)
            );
            ensureNoError(response, McpSchema.METHOD_TASKS_LIST);
            return OBJECT_MAPPER.convertValue(response.getResult(), McpSchema.ListTasksResult.class);
        }

        McpSchema.CancelTaskResult cancelTask(String sessionId, String taskId) throws Exception {
            McpSchema.JSONRPCResponse response = postRequestExpectSseResponse(
                    sessionId,
                    McpSchema.METHOD_TASKS_CANCEL,
                    new McpSchema.CancelTaskRequest(taskId, null),
                    Duration.ofSeconds(30)
            );
            ensureNoError(response, McpSchema.METHOD_TASKS_CANCEL);
            return OBJECT_MAPPER.convertValue(response.getResult(), McpSchema.CancelTaskResult.class);
        }

        private void sendInitializedNotification(String sessionId) throws Exception {
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

        private McpSchema.JSONRPCResponse postRequestExpectSseResponse(String sessionId, String method, Object params,
                                                                       Duration timeout) throws Exception {
            int maxAttempts = 5;
            Exception lastException = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return doPostExpectSseResponse(sessionId, method, params, timeout);
                } catch (IOException e) {
                    lastException = e;
                    if (attempt < maxAttempts) {
                        Thread.sleep(1_000L * attempt);
                    }
                }
            }
            throw new IllegalStateException(method + " 重试 " + maxAttempts + " 次后仍然失败", lastException);
        }

        private McpSchema.JSONRPCResponse doPostExpectSseResponse(String sessionId, String method, Object params,
                                                                   Duration timeout) throws Exception {
            McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest(
                    McpSchema.JSONRPC_VERSION,
                    method,
                    nextRequestId.getAndIncrement(),
                    params
            );

            HttpURLConnection conn = openPostConnection(sessionId);
            conn.setReadTimeout((int) timeout.toMillis());
            writeJson(conn, request);

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new IllegalStateException(method + " 失败: http=" + code + ", body=" + readBody(conn));
            }

            try (InputStream is = conn.getInputStream()) {
                return readJsonRpcResponseFromSse(is, request.getId());
            }
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

        private void ensureNoError(McpSchema.JSONRPCResponse response, String method) throws IOException {
            if (response.getError() != null) {
                throw new IllegalStateException(method + " 返回 error: " + OBJECT_MAPPER.writeValueAsString(response.getError()));
            }
        }

        private static void writeJson(HttpURLConnection conn, Object body) throws IOException {
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(body);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }
        }

        private static String readBody(HttpURLConnection conn) throws IOException {
            InputStream is;
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

        private static McpSchema.JSONRPCResponse readJsonRpcResponseFromSse(InputStream inputStream, Object expectedId)
                throws Exception {
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
                        if (Objects.equals(String.valueOf(resp.getId()), String.valueOf(expectedId))) {
                            return resp;
                        }
                    }
                }
            }
            // 更详细的错误信息
            throw new IllegalStateException("未从 SSE 流中读取到期望的 JSONRPCResponse, id=" + expectedId +
                    " (连接已关闭)");
        }
    }
}
