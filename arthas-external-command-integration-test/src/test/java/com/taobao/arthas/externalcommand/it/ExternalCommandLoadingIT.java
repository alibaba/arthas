package com.taobao.arthas.externalcommand.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ExternalCommandLoadingIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void should_load_demo_command_from_default_commands_directory() throws Exception {
        Assumptions.assumeFalse(isWindows(), "集成测试依赖 bash/as.sh，Windows 环境跳过");

        Path packagedArthasHome = resolvePackagedArthasHome();
        Path demoCommandJar = resolveDemoCommandJar();

        assertThat(packagedArthasHome).isDirectory();
        assertThat(packagedArthasHome.resolve("as.sh")).exists();
        assertThat(packagedArthasHome.resolve("arthas-client.jar")).exists();
        assertThat(demoCommandJar).exists();

        Path tempHome = Files.createTempDirectory("arthas-external-command-it-home");
        Path tempArthasHome = Files.createTempDirectory("arthas-external-command-it-arthas-home");
        Process targetJvm = null;
        try {
            copyDirectory(packagedArthasHome, tempArthasHome);
            Path commandsDir = tempArthasHome.resolve("commands");
            Files.createDirectories(commandsDir);
            Files.copy(demoCommandJar, commandsDir.resolve(demoCommandJar.getFileName()), StandardCopyOption.REPLACE_EXISTING);

            Path targetLog = tempHome.resolve("target-jvm.log");
            targetJvm = startTargetJvm(targetLog);
            long targetPid = ProcessPid.pidOf(targetJvm);

            int telnetPort = findFreePort();
            Path attachLog = tempHome.resolve("attach.log");
            runAttach(tempArthasHome, tempHome, attachLog, targetPid, telnetPort);

            waitForPortOpen("127.0.0.1", telnetPort, Duration.ofSeconds(30));

            String output = runClientCommand(tempArthasHome, tempHome, telnetPort,
                            "help demo-external; demo-external Codex");
            assertThat(output).contains("demo-external");
            assertThat(output).contains("demo external command loaded: Codex");
        } finally {
            if (targetJvm != null) {
                targetJvm.destroy();
                if (!targetJvm.waitFor(5, TimeUnit.SECONDS)) {
                    targetJvm.destroyForcibly();
                }
            }
            deleteDirectoryQuietly(tempHome);
            deleteDirectoryQuietly(tempArthasHome);
        }
    }

    private static void runAttach(Path arthasHome, Path tempHome, Path attachLog, long targetPid, int telnetPort)
                    throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                        "bash",
                        arthasHome.resolve("as.sh").toString(),
                        "--attach-only",
                        "--arthas-home",
                        arthasHome.toString(),
                        "--target-ip",
                        "127.0.0.1",
                        "--telnet-port",
                        String.valueOf(telnetPort),
                        "--http-port",
                        "-1",
                        String.valueOf(targetPid));
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(attachLog.toFile());

        Map<String, String> env = processBuilder.environment();
        env.put("JAVA_HOME", System.getProperty("java.home"));
        env.put("HOME", tempHome.toString());

        Process attachProcess = processBuilder.start();
        if (!attachProcess.waitFor(90, TimeUnit.SECONDS)) {
            attachProcess.destroyForcibly();
            throw new IllegalStateException("as.sh attach 超时: " + attachLog);
        }
        if (attachProcess.exitValue() != 0) {
            throw new IllegalStateException("as.sh attach 失败(exit=" + attachProcess.exitValue() + "): "
                            + attachLog + "\n" + readText(attachLog));
        }
    }

    private static String runClientCommand(Path arthasHome, Path tempHome, int telnetPort, String command)
                    throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                        resolveJavaBin("java"),
                        "-jar",
                        arthasHome.resolve("arthas-client.jar").toString(),
                        "127.0.0.1",
                        String.valueOf(telnetPort),
                        "-c",
                        command,
                        "--execution-timeout",
                        "30000");
        processBuilder.redirectErrorStream(true);

        Map<String, String> env = processBuilder.environment();
        env.put("JAVA_HOME", System.getProperty("java.home"));
        env.put("HOME", tempHome.toString());

        Process clientProcess = processBuilder.start();
        String output;
        try (InputStream inputStream = clientProcess.getInputStream()) {
            output = toString(inputStream);
        }

        if (!clientProcess.waitFor(60, TimeUnit.SECONDS)) {
            clientProcess.destroyForcibly();
            throw new IllegalStateException("arthas-client 执行超时");
        }
        if (clientProcess.exitValue() != 0) {
            throw new IllegalStateException("arthas-client 执行失败(exit=" + clientProcess.exitValue() + "):\n" + output);
        }
        return output;
    }

    private static Process startTargetJvm(Path targetLog) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                        resolveJavaBin("java"),
                        "-cp",
                        Paths.get(System.getProperty("basedir"), "target", "test-classes").toString(),
                        TargetJvmApp.class.getName());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(targetLog.toFile());
        return processBuilder.start();
    }

    private static Path resolvePackagedArthasHome() {
        return Paths.get(System.getProperty("basedir")).resolve("../packaging/target/arthas-bin").normalize();
    }

    private static Path resolveDemoCommandJar() throws IOException {
        Path targetDir = Paths.get(System.getProperty("basedir"))
                        .resolve("../arthas-demo-external-command/target")
                        .normalize();
        try (Stream<Path> stream = Files.list(targetDir)) {
            return stream
                            .filter(Files::isRegularFile)
                            .filter(path -> {
                                String fileName = path.getFileName().toString();
                                return fileName.startsWith("arthas-demo-external-command-")
                                                && fileName.endsWith(".jar")
                                                && !fileName.endsWith("-sources.jar")
                                                && !fileName.endsWith("-javadoc.jar");
                            })
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("未找到 demo command jar: " + targetDir));
        }
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

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(dir);
                Files.createDirectories(target.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Files.copy(file, target.resolve(relative), StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String readText(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static String toString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }

    private static String resolveJavaBin(String name) {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Path candidate = Paths.get(javaHome, "bin", name);
            if (Files.exists(candidate)) {
                return candidate.toString();
            }
        }
        throw new IllegalStateException("未找到 Java 命令: " + name);
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(Locale.ROOT).contains("win");
    }

    private static void deleteDirectoryQuietly(Path dir) {
        try {
            if (dir == null || !Files.exists(dir)) {
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
            try {
                java.lang.reflect.Method method = Process.class.getMethod("pid");
                Object value = method.invoke(process);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            } catch (Exception ignored) {
            }

            try {
                java.lang.reflect.Field pidField = process.getClass().getDeclaredField("pid");
                pidField.setAccessible(true);
                Object value = pidField.get(process);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            } catch (Exception e) {
                throw new IllegalStateException("无法获取目标 JVM pid", e);
            }
            throw new IllegalStateException("无法获取目标 JVM pid");
        }
    }
}
