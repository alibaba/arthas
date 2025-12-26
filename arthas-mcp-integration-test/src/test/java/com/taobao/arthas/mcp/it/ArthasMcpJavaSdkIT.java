package com.taobao.arthas.mcp.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.util.JsonParser;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class ArthasMcpJavaSdkIT {

    private static final ObjectMapper OBJECT_MAPPER = JsonParser.getObjectMapper();

    private static final String TARGET_CLASS_PATTERN = TargetJvmApp.class.getName();

    private static final String TARGET_METHOD_PATTERN = "hotMethod";

    private static final List<String> EXPECTED_TOOL_NAMES = Arrays.asList(
            "classloader",
            "dashboard",
            "dump",
            "getstatic",
            "heapdump",
            "jad",
            "jvm",
            "mc",
            "mbean",
            "memory",
            "monitor",
            "ognl",
            "options",
            "perfcounter",
            "redefine",
            "retransform",
            "sc",
            "sm",
            "stack",
            "stop",
            "sysenv",
            "sysprop",
            "thread",
            "trace",
            "tt",
            "vmoption",
            "vmtool",
            "watch"
    );

    private Environment env;

    @BeforeAll
    void setUp() throws Exception {
        Assumptions.assumeFalse(isWindows(), "集成测试依赖 bash/as.sh，Windows 环境跳过");
        this.env = Environment.start("arthas-mcp-java-sdk-it", "arthas-mcp-java-sdk-it-home");
    }

    @AfterAll
    void tearDown() {
        if (this.env != null) {
            this.env.close();
            this.env = null;
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void should_list_all_mcp_tools_via_java_mcp_sdk() {
        Set<String> expected = new HashSet<String>(EXPECTED_TOOL_NAMES);
        Set<String> actual = new HashSet<String>(this.env.toolNames);

        Set<String> missing = new HashSet<String>(expected);
        missing.removeAll(actual);

        Set<String> extra = new HashSet<String>(actual);
        extra.removeAll(expected);

        assertThat(missing).as("tools/list 缺少工具: %s", missing).isEmpty();
        assertThat(extra).as("tools/list 存在未覆盖的工具: %s", extra).isEmpty();
    }

    static Stream<String> toolNamesExceptStop() {
        List<String> toolNames = new ArrayList<String>(EXPECTED_TOOL_NAMES);
        toolNames.remove("stop");
        return toolNames.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("toolNamesExceptStop")
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void should_call_each_mcp_tool_via_java_mcp_sdk(String toolName) throws Exception {
        Map<String, Object> args = createArgumentsForTool(toolName, this.env);
        McpSchema.CallToolResult result = this.env.client.callTool(new McpSchema.CallToolRequest(toolName, args));

        String body = assertCallToolSuccess(toolName, result);
        assertToolSideEffects(toolName, this.env, body);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void should_call_stop_tool_via_java_mcp_sdk() throws Exception {
        Assumptions.assumeFalse(isWindows(), "集成测试依赖 bash/as.sh，Windows 环境跳过");

        Environment stopEnv = null;
        try {
            stopEnv = Environment.start("arthas-mcp-java-sdk-stop-it", "arthas-mcp-java-sdk-stop-it-home");
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("delayMs", 200);

            McpSchema.CallToolResult result = stopEnv.client.callTool(new McpSchema.CallToolRequest("stop", args));
            assertCallToolSuccess("stop", result);

            waitForPortClosed("127.0.0.1", stopEnv.httpPort, Duration.ofSeconds(15));
        } finally {
            if (stopEnv != null) {
                stopEnv.close();
            }
        }
    }

    private static void assertToolSideEffects(String toolName, Environment env, String body) throws Exception {
        if ("heapdump".equals(toolName)) {
            Path heapdumpFile = env.tempHome.resolve("heapdump.hprof");
            assertThat(heapdumpFile).exists();
            assertThat(Files.size(heapdumpFile)).isGreaterThan(0L);
            try {
                Files.deleteIfExists(heapdumpFile);
            } catch (Exception ignored) {
            }
            return;
        }

        if ("dump".equals(toolName)) {
            Path dumpOutputDir = env.tempHome.resolve("dump-output");
            assertThat(dumpOutputDir).isDirectory();
            assertThat(countFilesWithSuffix(dumpOutputDir, ".class")).isGreaterThan(0);
            return;
        }

        if ("mc".equals(toolName)) {
            Path mcOutputDir = env.tempHome.resolve("mc-output");
            assertThat(mcOutputDir).isDirectory();
            assertThat(countFilesWithSuffix(mcOutputDir, ".class")).isGreaterThan(0);
            return;
        }

        if (isStreamableTool(toolName)) {
            JsonNode node = OBJECT_MAPPER.readTree(body);
            if (node != null && node.isObject()) {
                JsonNode resultCount = node.get("resultCount");
                if (resultCount != null && resultCount.canConvertToInt()) {
                    int count = resultCount.asInt();
                    assertThat(count).as("tool=%s resultCount, body=%s", toolName, body).isGreaterThan(0);
                } else {
                    Assertions.fail("streamable tool 未返回 resultCount: tool=" + toolName + ", body=" + body);
                }
            }
        }
    }

    private static boolean isStreamableTool(String toolName) {
        return "dashboard".equals(toolName)
                || "monitor".equals(toolName)
                || "watch".equals(toolName)
                || "trace".equals(toolName)
                || "stack".equals(toolName)
                || "tt".equals(toolName);
    }

    private static int countFilesWithSuffix(Path dir, String suffix) throws IOException {
        if (dir == null || !Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return (int) stream
                    .filter(p -> Files.isRegularFile(p)
                            && p.getFileName() != null
                            && p.getFileName().toString().endsWith(suffix))
                    .count();
        }
    }

    private static Map<String, Object> createArgumentsForTool(String toolName, Environment env) throws IOException {
        Map<String, Object> args = new HashMap<String, Object>();

        if ("jvm".equals(toolName)
                || "thread".equals(toolName)
                || "memory".equals(toolName)
                || "options".equals(toolName)
                || "vmoption".equals(toolName)
                || "classloader".equals(toolName)
                || "perfcounter".equals(toolName)) {
            return args;
        }

        if ("jad".equals(toolName)) {
            args.put("classPattern", TARGET_CLASS_PATTERN);
            return args;
        }

        if ("sc".equals(toolName)) {
            args.put("classPattern", TARGET_CLASS_PATTERN);
            return args;
        }

        if ("sm".equals(toolName)) {
            args.put("classPattern", TARGET_CLASS_PATTERN);
            args.put("methodPattern", TARGET_METHOD_PATTERN);
            return args;
        }

        if ("dump".equals(toolName)) {
            args.put("classPattern", TARGET_CLASS_PATTERN);
            Path outDir = env.tempHome.resolve("dump-output");
            Files.createDirectories(outDir);
            args.put("outputDir", outDir.toString());
            args.put("limit", 1);
            return args;
        }

        if ("mc".equals(toolName)) {
            Path sourceFile = env.ensureMcSourceFile();
            Path outDir = env.tempHome.resolve("mc-output");
            Files.createDirectories(outDir);
            args.put("javaFilePaths", sourceFile.toString());
            args.put("outputDir", outDir.toString());
            return args;
        }

        if ("retransform".equals(toolName) || "redefine".equals(toolName)) {
            args.put("classFilePaths", env.targetClassFile.toString());
            return args;
        }

        if ("getstatic".equals(toolName)) {
            args.put("className", "java.lang.Integer");
            args.put("fieldName", "MAX_VALUE");
            return args;
        }

        if ("ognl".equals(toolName)) {
            args.put("expression", "@java.lang.System@getProperty(\"java.version\")");
            args.put("expandLevel", 1);
            return args;
        }

        if ("mbean".equals(toolName)) {
            args.put("namePattern", "java.lang:type=Runtime");
            args.put("attributePattern", "Uptime");
            return args;
        }

        if ("sysenv".equals(toolName)) {
            args.put("envName", "PATH");
            return args;
        }

        if ("sysprop".equals(toolName)) {
            args.put("propertyName", "java.version");
            return args;
        }

        if ("vmtool".equals(toolName)) {
            args.put("action", "getInstances");
            args.put("className", TARGET_CLASS_PATTERN);
            args.put("limit", 1);
            args.put("expandLevel", 1);
            args.put("express", "instances.length");
            return args;
        }

        if ("heapdump".equals(toolName)) {
            Path heapdumpFile = env.tempHome.resolve("heapdump.hprof");
            args.put("filePath", heapdumpFile.toString());
            return args;
        }

        if ("dashboard".equals(toolName)) {
            args.put("intervalMs", 200);
            args.put("numberOfExecutions", 1);
            return args;
        }

        if ("watch".equals(toolName)) {
            args.put("classPattern", TARGET_CLASS_PATTERN);
            args.put("methodPattern", TARGET_METHOD_PATTERN);
            args.put("numberOfExecutions", 1);
            args.put("timeout", 10);
            return args;
        }

        if ("trace".equals(toolName)) {
            args.put("classPattern", TARGET_CLASS_PATTERN);
            args.put("methodPattern", TARGET_METHOD_PATTERN);
            args.put("numberOfExecutions", 1);
            args.put("timeout", 10);
            return args;
        }

        if ("monitor".equals(toolName)) {
            args.put("classPattern", TARGET_CLASS_PATTERN);
            args.put("methodPattern", TARGET_METHOD_PATTERN);
            args.put("intervalMs", 1000);
            args.put("numberOfExecutions", 1);
            args.put("timeout", 15);
            return args;
        }

        if ("stack".equals(toolName)) {
            args.put("classPattern", TARGET_CLASS_PATTERN);
            args.put("methodPattern", TARGET_METHOD_PATTERN);
            args.put("numberOfExecutions", 1);
            // CI 环境下 stack 增强+触发可能更慢，适当放大超时时间以减少偶发失败
            args.put("timeout", 30);
            return args;
        }

        if ("tt".equals(toolName)) {
            args.put("action", "record");
            args.put("classPattern", TARGET_CLASS_PATTERN);
            args.put("methodPattern", TARGET_METHOD_PATTERN);
            args.put("numberOfExecutions", 1);
            args.put("timeout", 10);
            return args;
        }

        throw new IllegalArgumentException("未为 tool 配置参数: " + toolName);
    }

    private static String assertCallToolSuccess(String toolName, McpSchema.CallToolResult result) throws Exception {
        assertThat(result).as("tool=%s", toolName).isNotNull();
        assertThat(result.isError()).as("tool=%s, content=%s", toolName, result.content()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.content()).as("tool=%s", toolName).isNotNull().isNotEmpty();

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

    private static String extractTextContent(McpSchema.CallToolResult result) {
        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : result.content()) {
            if (content instanceof McpSchema.TextContent) {
                String text = ((McpSchema.TextContent) content).text();
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

    private static final class Environment implements AutoCloseable {
        private final Path arthasHome;
        private final Path tempHome;
        private final int httpPort;
        private final Process targetJvm;
        private final McpSyncClient client;
        private final Set<String> toolNames;
        private final Path targetClassFile;
        private Path mcSourceFile;

        private Environment(Path arthasHome, Path tempHome, int httpPort, Process targetJvm,
                            McpSyncClient client, Set<String> toolNames, Path targetClassFile) {
            this.arthasHome = arthasHome;
            this.tempHome = tempHome;
            this.httpPort = httpPort;
            this.targetJvm = targetJvm;
            this.client = client;
            this.toolNames = toolNames;
            this.targetClassFile = targetClassFile;
        }

        static Environment start(String clientName, String tempDirPrefix) throws Exception {
            Path arthasHome = resolveArthasBinDir();
            assertThat(arthasHome).isDirectory();
            assertThat(arthasHome.resolve("as.sh")).exists();
            assertThat(arthasHome.resolve("arthas-core.jar")).exists();
            assertThat(arthasHome.resolve("arthas-agent.jar")).exists();

            int telnetPort = 0;
            int httpPort = findFreePort();

            Path tempHome = Files.createTempDirectory(tempDirPrefix);
            Process targetJvm = null;
            McpSyncClient client = null;
            try {
                Path targetLog = tempHome.resolve("target-jvm.log");
                targetJvm = startTargetJvm(tempHome, targetLog);
                long targetPid = ProcessPid.pidOf(targetJvm);

                Path attachLog = tempHome.resolve("attach.log");
                runAttach(arthasHome, tempHome, attachLog, targetPid, telnetPort, httpPort);

                waitForPortOpen("127.0.0.1", httpPort, Duration.ofSeconds(30));

                McpClientTransport transport = HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + httpPort).build();
                client = McpClient.sync(transport)
                        .clientInfo(new McpSchema.Implementation(clientName, "1.0.0"))
                        .requestTimeout(Duration.ofSeconds(120))
                        .initializationTimeout(Duration.ofSeconds(10))
                        .build();

                McpSchema.InitializeResult initResult = client.initialize();
                assertThat(initResult).isNotNull();

                McpSchema.ListToolsResult toolsResult = client.listTools();
                assertThat(toolsResult).isNotNull();
                assertThat(toolsResult.tools()).isNotNull();

                Set<String> toolNames = new HashSet<String>();
                for (McpSchema.Tool tool : toolsResult.tools()) {
                    toolNames.add(tool.name());
                }

                Path targetClassFile = resolveTargetJvmAppClassFile();
                assertThat(targetClassFile).exists();

                return new Environment(arthasHome, tempHome, httpPort, targetJvm, client, toolNames, targetClassFile);
            } catch (Exception e) {
                if (client != null) {
                    try {
                        client.closeGracefully();
                    } catch (Exception ignored) {
                    }
                }
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

        Path ensureMcSourceFile() throws IOException {
            if (this.mcSourceFile != null) {
                return this.mcSourceFile;
            }
            Path source = this.tempHome.resolve("McpMcTestClass.java");
            String code = ""
                    + "public class McpMcTestClass {\n"
                    + "    public static int add(int a, int b) {\n"
                    + "        return a + b;\n"
                    + "    }\n"
                    + "}\n";
            Files.write(source, code.getBytes(StandardCharsets.UTF_8));
            this.mcSourceFile = source;
            return source;
        }

        @Override
        public void close() {
            if (this.client != null) {
                try {
                    this.client.closeGracefully();
                } catch (Exception ignored) {
                }
            }
            if (this.targetJvm != null) {
                this.targetJvm.destroy();
                try {
                    if (!this.targetJvm.waitFor(5, TimeUnit.SECONDS)) {
                        this.targetJvm.destroyForcibly();
                    }
                } catch (Exception ignored) {
                }
            }
            deleteDirectoryQuietly(this.tempHome);
        }
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

    private static Path resolveArthasBinDir() {
        String basedir = System.getProperty("basedir");
        assertThat(basedir).as("Maven surefire/failsafe should set system property 'basedir'").isNotBlank();
        return Paths.get(basedir).resolve("../packaging/target/arthas-bin").normalize();
    }

    private static Path resolveTargetJvmAppClassFile() {
        String basedir = System.getProperty("basedir");
        assertThat(basedir).as("Maven surefire/failsafe should set system property 'basedir'").isNotBlank();
        return Paths.get(basedir, "target", "test-classes", "com", "taobao", "arthas", "mcp", "it", "TargetJvmApp.class").normalize();
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

    private static void waitForPortClosed(String host, int port, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 500);
                Thread.sleep(200);
            } catch (IOException ignored) {
                return;
            }
        }
        throw new IllegalStateException("等待端口关闭超时: " + host + ":" + port);
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
            // Java 9+ 获取 pid
            try {
                return (Long) Process.class.getMethod("pid").invoke(process);
            } catch (Exception ignored) {
                // Java 8 兼容处理
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
}
