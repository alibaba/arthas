package com.taobao.arthas.core.command.monitor200;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import one.profiler.AsyncProfiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ProfilerNativeTest {
    @TempDir
    Path output;

    @Test
    void shouldReleaseClassLoaderAfterLoading() throws Exception {
        runProbe("version");
    }

    @Test
    void shouldProfileAndReleaseClassLoader() throws Exception {
        runProbe("profile");
    }

    private void runProbe(String mode) throws Exception {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch");
        String library = null;
        if (os.contains("mac")) {
            library = "libasyncProfiler-mac.dylib";
        } else if (os.contains("linux")) {
            if (arch.equals("amd64") || arch.equals("x86_64")) {
                library = "libasyncProfiler-linux-x64.so";
            } else if (arch.equals("aarch64") || arch.equals("arm64")) {
                library = "libasyncProfiler-linux-arm64.so";
            }
        }
        assumeTrue(library != null, "No bundled async-profiler library for " + os + "/" + arch);

        File classes = new File(AsyncProfiler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        File root = classes.getParentFile().getParentFile().getParentFile();
        File nativeLibrary = new File(root, "async-profiler/" + library);
        assertTrue(nativeLibrary.isFile(), nativeLibrary.toString());
        File testClasses = new File(Probe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path log = output.resolve("probe.log");
        // 独立 JVM 隔离 native 状态；解释执行和 Serial GC 使类加载器回收检查不受 JIT 局部变量存活影响。
        Process process = new ProcessBuilder(new File(System.getProperty("java.home"), "bin/java").getPath(),
                "-Xint", "-Xmx128m", "-XX:+UseSerialGC", "-cp", testClasses.getPath(), Probe.class.getName(),
                classes.getPath(), nativeLibrary.getPath(), output.toString(), mode)
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Profiler probe timed out: " + log);
            assertEquals(0, process.exitValue(), new String(Files.readAllBytes(log), StandardCharsets.UTF_8));
        } finally {
            process.destroyForcibly();
        }
    }

    public static class Probe {
        public static void main(String[] args) throws Exception {
            WeakReference<ClassLoader> loader = profile(args);
            for (int i = 0; i < 30 && loader.get() != null; i++) {
                System.gc();
                System.runFinalization();
                Thread.sleep(100);
            }
            require(loader.get() == null, "Profiler class loader was retained");
        }

        private static WeakReference<ClassLoader> profile(String[] args) throws Exception {
            try (URLClassLoader loader = new URLClassLoader(new URL[] {new File(args[0]).toURI().toURL()}, null)) {
                Class<?> api = Class.forName("one.profiler.AsyncProfiler", true, loader);
                Object profiler = api.getMethod("getInstance", String.class).invoke(null, args[1]);
                require("4.6".equals(api.getMethod("getVersion").invoke(profiler)), "Unexpected profiler version");
                if ("profile".equals(args[3])) {
                    Method execute = api.getMethod("execute", String.class);
                    Thread.currentThread().setName("arthas-profiler-test");
                    execute.invoke(profiler, "start,event=wall,interval=1ms,ithread=arthas-profiler-test");
                    try {
                        workload();
                        require((Long) api.getMethod("getSamples").invoke(profiler) > 0, "No wall samples");
                        require(((String) execute.invoke(profiler, "metrics")).contains("mem_calltracestorage_kb "),
                                "Missing memory metrics");
                        Class<?> counter = loader.loadClass("one.profiler.Counter");
                        for (Object value : counter.getEnumConstants()) {
                            String collapsed = (String) api.getMethod("dumpCollapsed", counter).invoke(profiler, value);
                            require(collapsed.contains("workload"), "Missing workload in collapsed output: " + collapsed);
                            byte[] otlp = (byte[]) api.getMethod("dumpOtlp", counter).invoke(profiler, value);
                            require(otlp.length > 0, "Empty OTLP output");
                        }
                        File html = new File(args[2], "profile.html");
                        execute.invoke(profiler, "dump,flamegraph,file=" + html);
                        String flamegraph = new String(Files.readAllBytes(html.toPath()), StandardCharsets.UTF_8);
                        require(flamegraph.contains("workload") && flamegraph.contains("id='treeview'"),
                                "Missing samples or tree view in HTML");
                    } finally {
                        api.getMethod("stop").invoke(profiler);
                    }

                    File jfr = new File(args[2], "profile.jfr");
                    execute.invoke(profiler, "start,event=wall,interval=1ms,jfr,file=" + jfr);
                    try {
                        workload();
                    } finally {
                        api.getMethod("stop").invoke(profiler);
                    }
                    byte[] recording = Files.readAllBytes(jfr.toPath());
                    require(recording.length > 4 && recording[0] == 'F' && recording[1] == 'L'
                            && recording[2] == 'R' && recording[3] == 0, "Invalid JFR output");
                    require(((String) execute.invoke(profiler, "status")).contains("not active"),
                            "Profiler did not stop");
                }
                return new WeakReference<ClassLoader>(loader);
            }
        }

        private static void workload() {
            long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(300);
            while (System.nanoTime() < end) {
                Math.sqrt(System.nanoTime());
            }
        }

        private static void require(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }
    }
}
