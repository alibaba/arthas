package com.taobao.arthas.core.shell.cli;

import com.taobao.arthas.core.shell.cli.impl.CliTokenImpl;
import com.taobao.arthas.core.shell.session.Session;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class CompletionUtilsTest {

    private static final String DUPLICATE_TARGET = "test.arthas.DuplicateTarget";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldMergeMethodCandidatesFromClassesLoadedByDifferentClassLoaders() throws Exception {
        Class<?> first = compileDuplicateTarget("public void alpha() {}\npublic void common() {}");
        Class<?> second = compileDuplicateTarget("public void beta() {}\npublic void common() {}");
        RecordingCompletion completion = completionFor(methodCompletionTokens(DUPLICATE_TARGET, ""), first, second);

        Assert.assertTrue(CompletionUtils.completeMethodName(completion));

        Assert.assertNotNull(completion.candidates);
        Assert.assertTrue(completion.candidates.contains("alpha"));
        Assert.assertTrue(completion.candidates.contains("beta"));
        Assert.assertTrue(completion.candidates.contains("common"));
        Assert.assertTrue(completion.candidates.contains("<init>"));
        Assert.assertEquals(1, Collections.frequency(completion.candidates, "common"));
        Assert.assertEquals(new HashSet<String>(completion.candidates).size(), completion.candidates.size());
    }

    @Test
    public void shouldCompletePartialMethodNameAcrossClassesLoadedByDifferentClassLoaders() throws Exception {
        Class<?> first = compileDuplicateTarget("public void alpha() {}\npublic void common() {}");
        Class<?> second = compileDuplicateTarget("public void beta() {}\npublic void common() {}");
        RecordingCompletion completion = completionFor(methodCompletionTokens(DUPLICATE_TARGET, "al"), first, second);

        Assert.assertTrue(CompletionUtils.completeMethodName(completion));

        Assert.assertEquals("pha", completion.value);
        Assert.assertTrue(completion.terminal);
        Assert.assertNull(completion.candidates);
    }

    @Test
    public void shouldCompleteConstructorPrefix() throws Exception {
        Class<?> first = compileDuplicateTarget("public void alpha() {}");
        Class<?> second = compileDuplicateTarget("public void beta() {}");
        RecordingCompletion completion = completionFor(methodCompletionTokens(DUPLICATE_TARGET, "<"), first, second);

        Assert.assertTrue(CompletionUtils.completeMethodName(completion));

        Assert.assertEquals("init>", completion.value);
        Assert.assertTrue(completion.terminal);
        Assert.assertNull(completion.candidates);
    }

    @Test
    public void shouldCompleteEmptyListWhenNoClassMatches() {
        RecordingCompletion completion = completionFor(methodCompletionTokens(DUPLICATE_TARGET, ""));

        Assert.assertTrue(CompletionUtils.completeMethodName(completion));

        Assert.assertEquals(Collections.<String>emptyList(), completion.candidates);
        Assert.assertNull(completion.value);
    }

    @Test
    public void shouldCompleteEmptyListWhenClassPatternMatchesDifferentClassNames() throws Exception {
        Class<?> first = compileTargetWithMissingSignature("test.arthas.FirstTarget");
        Class<?> second = compileTargetWithMissingSignature("test.arthas.SecondTarget");
        assertMethodsUnresolvable(first);
        assertMethodsUnresolvable(second);
        RecordingCompletion completion = completionFor(methodCompletionTokens("test.arthas.*Target", ""), first, second);

        Assert.assertTrue(CompletionUtils.completeMethodName(completion));

        Assert.assertEquals(Collections.<String>emptyList(), completion.candidates);
        Assert.assertNull(completion.value);
    }

    @Test
    public void shouldSkipClassWithUnresolvableMethodSignature() throws Exception {
        Class<?> healthy = compileDuplicateTarget("public void alpha() {}");
        Class<?> broken = compileTargetWithMissingSignature(DUPLICATE_TARGET);
        assertMethodsUnresolvable(broken);
        RecordingCompletion completion = completionFor(methodCompletionTokens(DUPLICATE_TARGET, ""), healthy, broken);

        Assert.assertTrue(CompletionUtils.completeMethodName(completion));

        Assert.assertTrue(completion.candidates.contains("alpha"));
        Assert.assertTrue(completion.candidates.contains("<init>"));
        Assert.assertFalse(completion.candidates.contains("broken"));
    }

    private static void assertMethodsUnresolvable(Class<?> target) {
        try {
            target.getDeclaredMethods();
            Assert.fail("Expected an unresolved method signature for " + target.getName());
        } catch (LinkageError expected) {
            // expected
        }
    }

    private Class<?> compileDuplicateTarget(String methods) throws Exception {
        return compileTarget(DUPLICATE_TARGET, methods);
    }

    private Class<?> compileTarget(String className, String methods) throws Exception {
        return compileTarget(className, methods, null);
    }

    private Class<?> compileTargetWithMissingSignature(String className) throws Exception {
        return compileTarget(className, "public MissingType broken(MissingType value) { return value; }", "MissingType");
    }

    private Class<?> compileTarget(String className, String methods, String missingType) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assert.assertNotNull("JDK compiler is required to compile test classes", compiler);

        File sourceRoot = temporaryFolder.newFolder();
        int packageEnd = className.lastIndexOf('.');
        String packageName = className.substring(0, packageEnd);
        String simpleName = className.substring(packageEnd + 1);
        File packageDir = new File(sourceRoot, packageName.replace('.', File.separatorChar));
        Assert.assertTrue(packageDir.mkdirs());
        File sourceFile = new File(packageDir, simpleName + ".java");
        String source = "package " + packageName + ";\npublic class " + simpleName + " {\n" + methods + "\n}\n";
        if (missingType != null) {
            source += "class " + missingType + " {}\n";
        }
        Files.write(sourceFile.toPath(), source.getBytes(StandardCharsets.UTF_8));

        File outputRoot = temporaryFolder.newFolder();
        int exitCode = compiler.run(null, null, null, "-d", outputRoot.getAbsolutePath(), sourceFile.getAbsolutePath());
        Assert.assertEquals(0, exitCode);

        URLClassLoader classLoader = new URLClassLoader(new URL[] { outputRoot.toURI().toURL() }, null);
        try {
            Class<?> target = Class.forName(className, true, classLoader);
            if (missingType != null) {
                File missingClass = new File(packageDir(outputRoot, packageName), missingType + ".class");
                Assert.assertTrue(missingClass.delete());
            }
            return target;
        } finally {
            classLoader.close();
        }
    }

    private static File packageDir(File root, String packageName) {
        return new File(root, packageName.replace('.', File.separatorChar));
    }

    private static RecordingCompletion completionFor(List<CliToken> tokens, Class<?>... classes) {
        Session session = Mockito.mock(Session.class);
        Mockito.when(session.getInstrumentation()).thenReturn(instrumentationFor(classes));
        return new RecordingCompletion(session, tokens);
    }

    private static List<CliToken> methodCompletionTokens(String className, String methodPrefix) {
        if (methodPrefix.length() == 0) {
            return Arrays.<CliToken>asList(
                    new CliTokenImpl(true, "watch"),
                    new CliTokenImpl(false, " "),
                    new CliTokenImpl(true, className),
                    new CliTokenImpl(false, " "));
        }
        return Arrays.<CliToken>asList(
                new CliTokenImpl(true, "watch"),
                new CliTokenImpl(false, " "),
                new CliTokenImpl(true, className),
                new CliTokenImpl(false, " "),
                new CliTokenImpl(true, methodPrefix));
    }

    private static Instrumentation instrumentationFor(final Class<?>... classes) {
        return (Instrumentation) Proxy.newProxyInstance(
                CompletionUtilsTest.class.getClassLoader(),
                new Class<?>[] { Instrumentation.class },
                (proxy, method, args) -> {
                    if ("getAllLoadedClasses".equals(method.getName())) {
                        return classes;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == Class[].class) {
                        return new Class<?>[0];
                    }
                    return null;
                });
    }

    private static class RecordingCompletion implements Completion {

        private final Session session;
        private final List<CliToken> tokens;
        private List<String> candidates;
        private String value;
        private boolean terminal;

        private RecordingCompletion(Session session, List<CliToken> tokens) {
            this.session = session;
            this.tokens = tokens;
        }

        @Override
        public Session session() {
            return session;
        }

        @Override
        public String rawLine() {
            return "";
        }

        @Override
        public List<CliToken> lineTokens() {
            return tokens;
        }

        @Override
        public void complete(List<String> candidates) {
            this.candidates = candidates;
        }

        @Override
        public void complete(String value, boolean terminal) {
            this.value = value;
            this.terminal = terminal;
        }
    }
}
