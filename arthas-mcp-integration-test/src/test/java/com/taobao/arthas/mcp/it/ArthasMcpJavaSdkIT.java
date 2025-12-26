package com.taobao.arthas.mcp.it;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ArthasMcpJavaSdkIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void should_list_tools_and_call_tool_via_java_mcp_sdk() throws Exception {
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
        McpSyncClient client = null;
        try {
            tempHome = Files.createTempDirectory("arthas-mcp-java-sdk-it-home");

            Path targetLog = tempHome.resolve("target-jvm.log");
            targetJvm = startTargetJvm(targetLog);
            long targetPid = ProcessPid.pidOf(targetJvm);

            Path attachLog = tempHome.resolve("attach.log");
            runAttach(arthasHome, tempHome, attachLog, targetPid, telnetPort, httpPort);

            waitForPortOpen("127.0.0.1", httpPort, Duration.ofSeconds(30));

            McpClientTransport transport = HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + httpPort).build();
            client = McpClient.sync(transport)
                    .clientInfo(new McpSchema.Implementation("arthas-mcp-java-sdk-it", "1.0.0"))
                    .requestTimeout(Duration.ofSeconds(60))
                    .initializationTimeout(Duration.ofSeconds(10))
                    .build();

            McpSchema.InitializeResult initResult = client.initialize();
            assertThat(initResult).isNotNull();

            McpSchema.ListToolsResult toolsResult = client.listTools();
            assertThat(toolsResult).isNotNull();
            assertThat(toolsResult.tools()).isNotNull();
            assertThat(toolsResult.tools().size()).isGreaterThanOrEqualTo(10);

            Set<String> toolNames = new HashSet<>();
            for (McpSchema.Tool tool : toolsResult.tools()) {
                toolNames.add(tool.name());
            }
            assertThat(toolNames).contains("jvm", "jad", "thread");

            McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest("jvm", Collections.emptyMap());
            McpSchema.CallToolResult callToolResult = client.callTool(callToolRequest);
            assertThat(callToolResult).isNotNull();
            assertThat(callToolResult.isError()).isNotEqualTo(Boolean.TRUE);
            assertThat(callToolResult.content()).isNotNull().isNotEmpty();
            boolean hasNonBlankText = false;
            for (McpSchema.Content content : callToolResult.content()) {
                if (content instanceof McpSchema.TextContent) {
                    McpSchema.TextContent text = (McpSchema.TextContent) content;
                    if (text.text() != null && !text.text().trim().isEmpty()) {
                        hasNonBlankText = true;
                        break;
                    }
                }
            }
            assertThat(hasNonBlankText).isTrue();
        } finally {
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
            if (tempHome != null) {
                deleteDirectoryQuietly(tempHome);
            }
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
