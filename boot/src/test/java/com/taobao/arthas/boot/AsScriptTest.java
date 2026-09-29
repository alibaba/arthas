package com.taobao.arthas.boot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AsScriptTest {
    private static final String[] JARS = { "arthas-core.jar", "arthas-agent.jar", "arthas-spy.jar", "arthas-client.jar" };

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path commands;
    private Path library;
    private Path launcher;
    private Path javaCalls;
    private Path curlCalls;
    private Path download;
    private Path javaHome;
    private Path platformJavaHome;
    private Path macJavaHome;
    private Path discoveryCalls;
    private Map<String, String> environment;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("Bash launcher tests require a Unix shell", File.separatorChar == '/');
        commands = temporaryFolder.newFolder("commands").toPath();
        library = temporaryFolder.newFolder("library").toPath();
        javaCalls = temporaryFolder.newFile("java-calls").toPath();
        curlCalls = temporaryFolder.newFile("curl-calls").toPath();
        download = temporaryFolder.newFile("download.zip").toPath();
        discoveryCalls = temporaryFolder.newFile("discovery-calls").toPath();
        platformJavaHome = temporaryFolder.getRoot().toPath().resolve("platform-jdk");
        macJavaHome = temporaryFolder.getRoot().toPath().resolve("mac-java-home");
        createArchive(true);

        // 只暴露启动器所需命令，避免宿主机的 telnet、lsof 和 Java 进程影响结果。
        for (String command : Arrays.asList("dirname", "basename", "uname", "grep", "awk", "unzip", "ls", "sed",
                        "tr", "sort", "tail", "mkdir", "rm", "mv", "cp", "date", "id", "cut", "mktemp")) {
            Process lookup = new ProcessBuilder("/bin/bash", "-c", "command -v \"$1\"", "lookup", command).start();
            byte[] output = readOutput(lookup);
            assertEquals("Missing test dependency: " + command, 0, lookup.exitValue());
            Files.createSymbolicLink(commands.resolve(command), Paths.get(new String(output, StandardCharsets.UTF_8).trim()));
        }
        writeExecutable(commands.resolve("telnet"), "exit 0\n");
        writeExecutable(commands.resolve("ps"),
                        "case \"$*\" in\n"
                        + "  *user=*) id -u -n;;\n"
                        + "  aux)\n"
                        + "    printf 'ps\\n' >> \"$AS_TEST_DISCOVERY_CALLS\"\n"
                        + "    if [[ -n $AS_TEST_PROCESS_JAVA_HOME ]]; then\n"
                        + "      printf 'user 1234 0 0 0 0 ? Sl 00:00 0:00 %s/bin/java example.Main\\n' \"$AS_TEST_PROCESS_JAVA_HOME\"\n"
                        + "    fi;;\n"
                        + "  *) echo 1234;;\n"
                        + "esac\n");
        writeExecutable(commands.resolve("curl"),
                        "printf '%s\\n' \"$*\" >> \"$AS_TEST_CURL_CALLS\"\n"
                        + "output=\n"
                        + "while [[ $# -gt 0 ]]; do\n"
                        + "  if [[ $1 == -o ]]; then shift; output=$1; fi\n"
                        + "  shift\n"
                        + "done\n"
                        + "if [[ -z $output ]]; then\n"
                        + "  [[ $AS_TEST_VERSION_FAILURE == true ]] && exit 22\n"
                        + "  printf '%s' \"$AS_TEST_VERSION\"\n"
                        + "else\n"
                        + "  [[ $AS_TEST_DOWNLOAD_FAILURE == true ]] && exit 28\n"
                        + "  cp \"$AS_TEST_DOWNLOAD\" \"$output\"\n"
                        + "fi\n");

        javaHome = createJavaHome(temporaryFolder.getRoot().toPath().resolve("jdk"), "17.0.2", false);

        Path project = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (project != null && !Files.isRegularFile(project.resolve("bin/as.sh"))) {
            project = project.getParent();
        }
        assertTrue("Cannot locate bin/as.sh", project != null);
        String script = new String(Files.readAllBytes(project.resolve("bin/as.sh")), StandardCharsets.UTF_8);
        // 隔离固定平台路径，保留完整 Java 初始化和启动流程。
        script = script.replace("/opt/taobao/java", platformJavaHome.toString())
                        .replace("/usr/libexec/java_home", macJavaHome.toString());
        int mainCall = script.lastIndexOf("\nmain \"${@}\"");
        assertTrue("Cannot locate launcher entry point", mainCall >= 0);
        // 显式设置 umask，避免宿主设置影响权限断言；旧版本的 /tmp 仍隔离到测试目录。
        script = script.substring(0, mainCall) + "\numask \"$AS_TEST_UMASK\"\nTMP_DIR=\"$AS_TEST_TMP_DIR\""
                        + script.substring(mainCall);
        launcher = temporaryFolder.newFile("as.sh").toPath();
        Files.write(launcher, script.getBytes(StandardCharsets.UTF_8));

        environment = new HashMap<String, String>();
        environment.put("PATH", commands.toString());
        environment.put("JAVA_HOME", javaHome.toString());
        environment.put("ARTHAS_LIB_DIR", library.toString());
        environment.put("LC_ALL", "C");
        environment.put("AS_TEST_UMASK", "022");
        environment.put("AS_TEST_VERSION", "4.3.5");
        environment.put("AS_TEST_DOWNLOAD", download.toString());
        environment.put("AS_TEST_JAVA_CALLS", javaCalls.toString());
        environment.put("AS_TEST_CURL_CALLS", curlCalls.toString());
        environment.put("AS_TEST_DISCOVERY_CALLS", discoveryCalls.toString());
        environment.put("AS_TEST_TMP_DIR", temporaryFolder.newFolder("downloads").getAbsolutePath());
    }

    @Test
    public void invalidExplicitJavaHomeFallsBackToPlatform() throws Exception {
        Path fallback = createJavaHome(platformJavaHome, "11.0.27.26-AJDK", false);
        Path missing = temporaryFolder.getRoot().toPath().resolve("missing-jdk");
        Path empty = temporaryFolder.newFolder("empty-jdk").toPath();
        Path nonExecutable = createJavaHome(temporaryFolder.getRoot().toPath().resolve("non-executable"), "17", false);
        Files.setPosixFilePermissions(nonExecutable.resolve("bin/java"), PosixFilePermissions.fromString("rw-r--r--"));
        for (Path invalid : Arrays.asList(missing, empty, nonExecutable)) {
            environment.put("JAVA_HOME", invalid.toString());
            String output = runHelp(0);
            assertJavaHome(output, fallback);
            assertTrue(output, output.contains("bin/java is missing or not executable"));
        }
        assertEquals("", read(discoveryCalls));
    }

    @Test
    public void validExplicitJavaHomeAvoidsFallbackDiscovery() throws Exception {
        Path fallback = createJavaHome(platformJavaHome, "11", false);
        useMacJava(fallback);
        Files.createSymbolicLink(commands.resolve("java"), fallback.resolve("bin/java"));
        environment.put("AS_TEST_PROCESS_JAVA_HOME", fallback.toString());
        String output = runHelp(0);
        assertJavaHome(output, javaHome);
        assertFalse(output, output.contains("[WARN]"));
        assertEquals("", read(discoveryCalls));
    }

    @Test
    public void platformJavaTakesPrecedenceOverMacAndPath() throws Exception {
        environment.put("JAVA_HOME", "");
        Path fallback = createJavaHome(platformJavaHome, "11", false);
        useMacJava(javaHome);
        Files.createSymbolicLink(commands.resolve("java"), javaHome.resolve("bin/java"));
        assertJavaHome(runHelp(0), fallback);
        assertEquals("", read(discoveryCalls));
    }

    @Test
    public void macJavaTakesPrecedenceOverPath() throws Exception {
        environment.put("JAVA_HOME", "");
        useMacJava(javaHome);
        Path pathJava = createJavaHome(temporaryFolder.getRoot().toPath().resolve("path-jdk"), "21", false);
        Files.createSymbolicLink(commands.resolve("java"), pathJava.resolve("bin/java"));
        assertJavaHome(runHelp(0), javaHome);
        assertEquals("mac\n", read(discoveryCalls));
    }

    @Test
    public void failedMacLookupFallsBackToPath() throws Exception {
        environment.put("JAVA_HOME", "");
        writeExecutable(macJavaHome, "exit 1\n");
        Files.createSymbolicLink(commands.resolve("java"), javaHome.resolve("bin/java"));
        assertJavaHome(runHelp(0), javaHome);
    }

    @Test
    public void pathWithSpacesAndSymlinkWorksWithoutOptionalPlatformPaths() throws Exception {
        environment.put("JAVA_HOME", "");
        Path path = temporaryFolder.newFolder("path with spaces").toPath();
        Files.createSymbolicLink(path.resolve("java"), javaHome.resolve("bin/java"));
        environment.put("PATH", path + File.pathSeparator + commands);
        String output = runHelp(0);
        assertJavaHome(output, javaHome);
        assertFalse(output, output.contains("[WARN]"));
        assertEquals("", read(discoveryCalls));
    }

    @Test
    public void processFallbackAcceptsModernJavaWithoutToolsJar() throws Exception {
        environment.put("JAVA_HOME", temporaryFolder.getRoot().toPath().resolve("missing-jdk").toString());
        Path processJava = createJavaHome(temporaryFolder.getRoot().toPath().resolve("process-jdk"), "11", false);
        environment.put("AS_TEST_PROCESS_JAVA_HOME", processJava.toString());
        assertJavaHome(runHelp(0), processJava);
        assertEquals("ps\n", read(discoveryCalls));
    }

    @Test
    public void failedVersionCommandIsRejectedEvenWithRecognizableOutput() throws Exception {
        Path fallback = createJavaHome(platformJavaHome, "11", false);
        writeJava(javaHome, "openjdk version \"17.0.2\"", 42);
        String output = runHelp(0);
        assertJavaHome(output, fallback);
        assertTrue(output, output.contains("java -version failed"));
    }

    @Test
    public void invalidVersionsFallBackInsteadOfAssumingJava8() throws Exception {
        Path fallback = createJavaHome(platformJavaHome, "11", false);
        for (String version : Arrays.asList("", "unrecognized vendor output", "openjdk version \"0\"",
                        "openjdk version \"invalid\"")) {
            writeJava(javaHome, version, 0);
            String output = runHelp(0);
            assertJavaHome(output, fallback);
            assertTrue(output, output.contains("unrecognized Java version"));
        }
    }

    @Test
    public void modernJavaVersionsDoNotNeedToolsJar() throws Exception {
        for (String version : Arrays.asList("9", "11.0.27.26-AJDK", "11-ea", "17.0.15", "21.0.7", "25")) {
            writeJava(javaHome, "openjdk version \"" + version + "\"\r\nOpenJDK Runtime Environment\r", 0);
            assertJavaHome(runHelp(0), javaHome);
        }
    }

    @Test
    public void java8AndNestedJreUseJdkToolsJarWithSpaces() throws Exception {
        Path jdk = createJavaHome(temporaryFolder.getRoot().toPath().resolve("jdk8 with spaces"), "1.8.0_402", true);
        Path jre = createJavaHome(jdk.resolve("jre"), "1.8.0_402", false);
        Path nestedJre = createJavaHome(jre.resolve("nested"), "1.8.0_402", false);
        Path arthas = cache("4.3.5");
        for (Path candidate : Arrays.asList(jdk, jre, nestedJre)) {
            Files.write(javaCalls, new byte[0]);
            environment.put("JAVA_HOME", candidate.toString());
            environment.put("BOOT_CLASSPATH", "-Xbootclasspath/a:/stale/tools.jar");
            assertJavaHome(run(0, "--arthas-home", arthas.toString(), "--attach-only", "1234"), jdk);
            String calls = read(javaCalls);
            assertTrue(calls, calls.contains("-Xbootclasspath/a:" + jdk.resolve("lib/tools.jar")));
            assertFalse(calls, calls.contains("/stale/tools.jar"));
        }
    }

    @Test
    public void java8WithoutUsableParentJdkFallsBack() throws Exception {
        Path fallback = createJavaHome(platformJavaHome, "11", false);
        Path parent = temporaryFolder.newFolder("jdk8-without-java").toPath();
        Files.createDirectories(parent.resolve("lib"));
        Files.createFile(parent.resolve("lib/tools.jar"));
        Path jre = createJavaHome(parent.resolve("jre"), "1.8.0_402", false);
        environment.put("JAVA_HOME", jre.toString());
        String output = runHelp(0);
        assertJavaHome(output, fallback);
        assertTrue(output, output.contains("requires a JDK with lib/tools.jar"));
    }

    @Test
    public void modernJavaClearsInheritedBootClasspathBeforeAttach() throws Exception {
        environment.put("BOOT_CLASSPATH", "-Xbootclasspath/a:/stale/tools.jar");
        Path arthas = cache("4.3.5");
        assertJavaHome(run(0, "--arthas-home", arthas.toString(), "--attach-only", "1234"), javaHome);
        assertFalse(read(javaCalls), read(javaCalls).contains("-Xbootclasspath"));
    }

    @Test
    public void inheritedBootClasspathCannotMakeInvalidJavaUsable() throws Exception {
        environment.put("JAVA_HOME", temporaryFolder.getRoot().toPath().resolve("missing-jdk").toString());
        environment.put("BOOT_CLASSPATH", "-Xbootclasspath/a:/stale/tools.jar");
        String output = runHelp(1);
        assertTrue(output, output.contains("Can not find a usable JAVA_HOME"));
        assertFalse(output, output.contains("tools.jar was not found, so arthas could not be launched!"));
    }

    @Test
    public void unknownVersionReportsSelectionFailureBeforeDownloadOrAttach() throws Exception {
        writeJava(javaHome, "unrecognized vendor output", 0);
        String output = runHelp(1);
        assertTrue(output, output.contains("unrecognized Java version"));
        assertTrue(output, output.contains("Can not find a usable JAVA_HOME"));
        assertFalse(output, output.contains("tools.jar was not found, so arthas could not be launched!"));
    }

    @Test
    public void choosesNewestNumericCachedVersion() throws Exception {
        cache("4.3.9");
        Path newest = cache("4.3.10");
        assertHome(run(0, "--attach-only", "1234"), newest);
    }

    @Test
    public void downloadsNewerNumericVersion() throws Exception {
        cache("4.3.9");
        environment.put("AS_TEST_VERSION", "4.3.10");
        Path newest = library.resolve("4.3.10/arthas");
        assertHome(run(0, "--attach-only", "1234"), newest);
        assertComplete(newest);
    }

    @Test
    public void downloadsRespectUmask() throws Exception {
        String[][] permissions = { { "022", "rwxr-xr-x" }, { "027", "rwxr-x---" } };
        for (int i = 0; i < permissions.length; i++) {
            String version = "4.3." + (5 + i);
            environment.put("AS_TEST_UMASK", permissions[i][0]);
            environment.put("AS_TEST_VERSION", version);
            Path home = library.resolve(version).resolve("arthas");
            assertHome(run(0, "--attach-only", "1234"), home);
            assertComplete(home);
            assertEquals(PosixFilePermissions.fromString(permissions[i][1]), Files.getPosixFilePermissions(home));
            assertEquals(PosixFilePermissions.fromString(permissions[i][1]),
                            Files.getPosixFilePermissions(home.getParent()));
        }
        assertNoTemporaryDownloads();
    }

    @Test
    public void comparesMajorAndMinorVersionsNumerically() throws Exception {
        cache("4.9.9");
        Path minor = cache("4.10.0");
        assertHome(run(0, "--attach-only", "1234"), minor);
        cache("9.10.0");
        Path major = cache("10.0.0");
        assertHome(run(0, "--attach-only", "1234"), major);
    }

    @Test
    public void repairsEmptyVersionDirectory() throws Exception {
        Files.createDirectory(library.resolve("4.3.5"));
        Path home = library.resolve("4.3.5/arthas");
        assertHome(run(0, "--attach-only", "1234"), home);
        assertComplete(home);
    }

    @Test
    public void repairsExplicitIncompleteVersion() throws Exception {
        Path home = Files.createDirectories(library.resolve("4.3.5/arthas"));
        Files.write(home.resolve("arthas-core.jar"), new byte[] { 1 });
        run(0, "--use-version", "4.3.5", "--attach-only", "1234");
        assertComplete(home);
    }

    @Test
    public void repairsEmptyJar() throws Exception {
        Path home = cache("4.3.5");
        Files.write(home.resolve("arthas-agent.jar"), new byte[0]);
        run(0, "--attach-only", "1234");
        assertComplete(home);
    }

    @Test
    public void fallsBackWhenArchiveIsIncomplete() throws Exception {
        Path previous = cache("4.3.4");
        createArchive(false);
        assertHome(run(0, "--attach-only", "1234"), previous);
        assertFalse(Files.exists(library.resolve("4.3.5")));
        assertComplete(previous);
        assertNoTemporaryDownloads();
    }

    @Test
    public void fallsBackWhenArchiveCannotBeUnzipped() throws Exception {
        Path previous = cache("4.3.4");
        Files.write(download, "not a zip archive".getBytes(StandardCharsets.UTF_8));
        assertHome(run(0, "--attach-only", "1234"), previous);
        assertFalse(Files.exists(library.resolve("4.3.5")));
        assertComplete(previous);
        assertNoTemporaryDownloads();
    }

    @Test
    public void failedDownloadDoesNotCreateVersionDirectory() throws Exception {
        Path previous = cache("4.3.4");
        environment.put("AS_TEST_DOWNLOAD_FAILURE", "true");
        assertHome(run(0, "--attach-only", "1234"), previous);
        // Java 启动器会枚举版本目录，失败下载不能留下一个虚假的最新版本。
        assertFalse(Files.exists(library.resolve("4.3.5")));
        assertComplete(previous);
        assertNoTemporaryDownloads();
    }

    @Test
    public void fallsBackWhenDownloadFailsAndIgnoresBrokenCache() throws Exception {
        Path previous = cache("4.3.4");
        Files.createDirectories(library.resolve("4.3.5/arthas"));
        environment.put("AS_TEST_DOWNLOAD_FAILURE", "true");
        assertHome(run(0, "--attach-only", "1234"), previous);
        assertTrue(Files.isDirectory(library.resolve("4.3.5/arthas")));
        assertComplete(previous);
        assertNoTemporaryDownloads();
    }

    @Test
    public void explicitVersionFailureStopsBeforeAttach() throws Exception {
        cache("4.3.4");
        environment.put("AS_TEST_DOWNLOAD_FAILURE", "true");
        String output = run(1, "--use-version", "4.3.5", "--attach-only", "1234");
        assertTrue(output, output.contains("Failed to install Arthas version 4.3.5"));
        assertEquals("", read(javaCalls));
        assertFalse(Files.exists(library.resolve("4.3.5")));
        assertNoTemporaryDownloads();
    }

    @Test
    public void failedVersionLookupUsesValidCache() throws Exception {
        Path previous = cache("4.3.4");
        environment.put("AS_TEST_VERSION_FAILURE", "true");
        assertHome(run(0, "--attach-only", "1234"), previous);
    }

    @Test
    public void invalidVersionResponseDoesNotTriggerDownload() throws Exception {
        Path previous = cache("4.3.4");
        environment.put("AS_TEST_VERSION", "<html>service unavailable</html>");
        assertHome(run(0, "--attach-only", "1234"), previous);
        assertEquals(read(curlCalls), 1, Files.readAllLines(curlCalls, StandardCharsets.UTF_8).size());
    }

    @Test
    public void reportsUnavailableRuntimeWhenLookupFailsWithoutCache() throws Exception {
        environment.put("AS_TEST_VERSION_FAILURE", "true");
        String output = run(1, "--attach-only", "1234");
        assertTrue(output, output.contains("No usable Arthas installation"));
        assertEquals("", read(javaCalls));
    }

    @Test
    public void attachOnlyDoesNotRequireTelnet() throws Exception {
        Files.delete(commands.resolve("telnet"));
        Path home = cache("4.3.5");
        assertHome(run(0, "--attach-only", "1234"), home);
        assertTrue(read(javaCalls).contains("arthas-core.jar"));
    }

    @Test
    public void batchCommandDoesNotRequireTelnet() throws Exception {
        Files.delete(commands.resolve("telnet"));
        cache("4.3.5");
        run(0, "-c", "version", "1234");
        assertTrue(read(javaCalls), read(javaCalls).contains("-c version"));
    }

    @Test
    public void batchFileDoesNotRequireTelnet() throws Exception {
        Files.delete(commands.resolve("telnet"));
        cache("4.3.5");
        Path batch = temporaryFolder.newFile("commands.as").toPath();
        Files.write(batch, "version\n".getBytes(StandardCharsets.UTF_8));
        run(0, "-f", batch.toString(), "1234");
        assertTrue(read(javaCalls), read(javaCalls).contains("-f " + batch));
    }

    @Test
    public void interactiveModeRequiresTelnetBeforeAttach() throws Exception {
        Files.delete(commands.resolve("telnet"));
        cache("4.3.5");
        String output = run(1, "1234");
        assertTrue(output, output.contains("telnet is not installed"));
        assertEquals("", read(javaCalls));
    }

    @Test
    public void requestsHaveHttpFailureHandlingAndTimeouts() throws Exception {
        run(0, "--attach-only", "1234");
        List<String> requests = Files.readAllLines(curlCalls, StandardCharsets.UTF_8);
        assertEquals(requests.toString(), 2, requests.size());
        for (String request : requests) {
            assertTrue(request, request.contains("--fail"));
            assertTrue(request, request.contains("--connect-timeout"));
            assertTrue(request, request.contains("--max-time"));
        }
        assertNoTemporaryDownloads();
    }

    private Path cache(String version) throws IOException {
        Path home = Files.createDirectories(library.resolve(version).resolve("arthas"));
        for (String jar : JARS) {
            Files.write(home.resolve(jar), new byte[] { 1 });
        }
        return home;
    }

    private void createArchive(boolean complete) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(download))) {
            for (String jar : complete ? JARS : new String[] { JARS[0] }) {
                zip.putNextEntry(new ZipEntry(jar));
                zip.write(1);
                zip.closeEntry();
            }
        }
    }

    private String run(int expectedExit, String... arguments) throws Exception {
        List<String> command = new ArrayList<String>(Arrays.asList("/bin/bash", launcher.toString()));
        command.addAll(Arrays.asList(arguments));
        Path output = temporaryFolder.newFile().toPath();
        ProcessBuilder builder = new ProcessBuilder(command).directory(temporaryFolder.getRoot());
        for (String variable : Arrays.asList("BOOT_CLASSPATH", "BASH_ENV", "ENV", "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS",
                        "JDK_JAVA_OPTIONS")) {
            builder.environment().remove(variable);
        }
        builder.environment().putAll(environment);
        builder.redirectErrorStream(true).redirectOutput(output.toFile());
        Process process = builder.start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Launcher timed out: " + read(output));
        }
        String text = read(output);
        assertEquals(text, expectedExit, process.exitValue());
        return text;
    }

    private String runHelp(int expectedExit) throws Exception {
        String output = run(expectedExit, "--help");
        assertEquals("Java selection must not invoke download commands", "", read(curlCalls));
        assertEquals("Java selection must not invoke attach or client commands", "", read(javaCalls));
        if (expectedExit == 0) {
            assertTrue(output, output.contains("Usage:"));
        }
        return output;
    }

    private void assertJavaHome(String output, Path expected) throws IOException {
        assertTrue(output, output.contains("[INFO] JAVA_HOME: " + expected.toRealPath() + "\n"));
    }

    private Path createJavaHome(Path home, String version, boolean tools) throws IOException {
        Files.createDirectories(home.resolve("bin"));
        writeJava(home, "openjdk version \"" + version + "\"", 0);
        writeExecutable(home.resolve("bin/jps"), "echo '1234 example.Main'\n");
        if (tools) {
            Files.createDirectories(home.resolve("lib"));
            Files.createFile(home.resolve("lib/tools.jar"));
        }
        return home.toRealPath();
    }

    private void writeJava(Path home, String versionOutput, int exitCode) throws IOException {
        writeExecutable(home.resolve("bin/java"),
                        "if [[ $1 == -version ]]; then\n"
                        + "  printf '%s\\n' '" + versionOutput.replace("'", "'\\''") + "' >&2\n"
                        + "  exit " + exitCode + "\n"
                        + "fi\n"
                        + "printf '%s\\n' \"$*\" >> \"$AS_TEST_JAVA_CALLS\"\n");
    }

    private void useMacJava(Path home) throws IOException {
        environment.put("AS_TEST_MAC_JAVA_HOME", home.toString());
        writeExecutable(macJavaHome,
                        "printf 'mac\\n' >> \"$AS_TEST_DISCOVERY_CALLS\"\n"
                        + "printf '%s\\n' \"$AS_TEST_MAC_JAVA_HOME\"\n");
    }

    private void assertHome(String output, Path home) {
        assertTrue(output, output.contains("Arthas home: " + home + "\n"));
    }

    private void assertComplete(Path home) throws IOException {
        for (String jar : JARS) {
            assertTrue(home.resolve(jar).toString(), Files.isRegularFile(home.resolve(jar)));
            assertTrue(Files.size(home.resolve(jar)) > 0);
        }
    }

    private void assertNoTemporaryDownloads() throws IOException {
        try (Stream<Path> paths = Files.walk(temporaryFolder.getRoot().toPath())) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith("temp_")
                            || path.getFileName().toString().startsWith(".arthas.")));
        }
    }

    private void writeExecutable(Path file, String body) throws IOException {
        Files.write(file, ("#!/bin/bash\n" + body).getBytes(StandardCharsets.UTF_8));
        assertTrue(file.toFile().setExecutable(true));
    }

    private String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private byte[] readOutput(Process process) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = process.getInputStream().read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        assertTrue(process.waitFor(5, TimeUnit.SECONDS));
        return output.toByteArray();
    }
}
