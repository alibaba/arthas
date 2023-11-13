package com.taobao.arthas.compiler;

import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.FileUtils;
import com.taobao.arthas.common.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.tools.*;

public class DynamicCompiler {
    private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
    private static final StandardJavaFileManager STANDARD_FILE_MANAGER = COMPILER == null
            ? null : COMPILER.getStandardFileManager(null, null, null);
    private static final Set<String> CLASSPATH_ROOTS = new HashSet<>();

    private final List<String> options = new ArrayList<String>();
    private final DynamicClassLoader dynamicClassLoader;
    private final Collection<JavaFileObject> compilationUnits = new ArrayList<JavaFileObject>();
    private final List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<Diagnostic<? extends JavaFileObject>>();
    private final List<Diagnostic<? extends JavaFileObject>> warnings = new ArrayList<Diagnostic<? extends JavaFileObject>>();

    static {
        if (STANDARD_FILE_MANAGER != null) {
            Set<File> jarSearchPathSet = new HashSet<>();
            Set<String> classpathRootSet = new HashSet<>();

            try {
                String userDir = System.getProperty("user.dir");
                String[] jars = System.getProperty( "java.class.path").split(File.pathSeparator);
                for (String jarPath : jars) {
                    try (JarFile jarFile = new JarFile(getJarAbsolutePath(jarPath, userDir))) {
                        Manifest manifest = jarFile.getManifest();
                        if (manifest == null) {
                            continue;
                        }
                        String classpath = manifest.getMainAttributes().getValue(new Attributes.Name("Class-Path"));
                        if (Objects.nonNull(classpath)) {
                            inner: for (StringTokenizer st = new StringTokenizer(classpath); st.hasMoreTokens(); ) {
                                String ele = st.nextToken();
                                if (ele.startsWith("file:/")) ele = ele.substring(6);
                                if (ele.endsWith(".jar") || ele.endsWith(".zip")) {
                                    File absolutePath = getJarAbsolutePath(ele, userDir);
                                    if (absolutePath != null) jarSearchPathSet.add(absolutePath);
                                    continue inner;
                                }
                                classpathRootSet.add(ele);
                            }
                        }
                        // springboot的jar
                        String bootClasses = manifest.getMainAttributes().getValue(new Attributes.Name("Spring-Boot-Classes"));
                        String bootLibs = manifest.getMainAttributes().getValue(new Attributes.Name("Spring-Boot-Lib"));
                        if (Objects.nonNull(bootClasses) && Objects.nonNull(bootLibs)) {
                            handleBootJar(jarFile, bootClasses, bootLibs, jarSearchPathSet, classpathRootSet);
                        }
                    }
                }
                CLASSPATH_ROOTS.addAll(classpathRootSet);
                STANDARD_FILE_MANAGER.setLocation(StandardLocation.CLASS_PATH, jarSearchPathSet);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public DynamicCompiler(ClassLoader classLoader) {
        if (COMPILER == null) {
            throw new IllegalStateException(
                    "Can not load JavaCompiler from javax.tools.ToolProvider#getSystemJavaCompiler(),"
                            + " please confirm the application running in JDK not JRE.");
        }
        options.add("-Xlint:unchecked");
        dynamicClassLoader = new DynamicClassLoader(classLoader);
    }

    public void addSource(String className, String source) {
        addSource(new StringSource(className, source));
    }

    public void addSource(JavaFileObject javaFileObject) {
        compilationUnits.add(javaFileObject);
    }

    public Map<String, byte[]> buildByteCodes() {
        errors.clear();
        warnings.clear();
        JavaFileManager fileManager = new DynamicJavaFileManager(STANDARD_FILE_MANAGER, CLASSPATH_ROOTS, dynamicClassLoader);

        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler.CompilationTask task = COMPILER.getTask(null, fileManager, collector, options, null,
                compilationUnits);
        try {
            if (!compilationUnits.isEmpty()) {
                boolean result = task.call();
                if (!result || collector.getDiagnostics().size() > 0) {
                    for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
                        switch (diagnostic.getKind()) {
                            case NOTE:
                            case MANDATORY_WARNING:
                            case WARNING:
                                warnings.add(diagnostic);
                                break;
                            case OTHER:
                            case ERROR:
                            default:
                                errors.add(diagnostic);
                                break;
                        }
                    }
                    if (!errors.isEmpty()) {
                        throw new DynamicCompilerException("Compilation Error", errors);
                    }
                }
            }
            return dynamicClassLoader.getByteCodes();
        } catch (ClassFormatError e) {
            throw new DynamicCompilerException(e, errors);
        } finally {
            compilationUnits.clear();
        }
    }

    private List<String> diagnosticToString(List<Diagnostic<? extends JavaFileObject>> diagnostics) {

        List<String> diagnosticMessages = new ArrayList<String>();

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            diagnosticMessages.add(
                    "line: " + diagnostic.getLineNumber() + ", message: " + diagnostic.getMessage(Locale.US));
        }
        return diagnosticMessages;
    }

    public List<String> getErrors() {
        return diagnosticToString(errors);
    }

    public List<String> getWarnings() {
        return diagnosticToString(warnings);
    }

    public ClassLoader getClassLoader() {
        return dynamicClassLoader;
    }

    private static void handleBootJar(JarFile jarFile, String classPrefix, String libPrefix, Set<File> jarSearchPathSet, Set<String> classpathRootSet) throws IOException {
        String tmpClasspath = FileUtils.getTempProcessDir();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if ((entryName.startsWith(classPrefix) || entryName.startsWith(libPrefix))
                    && (entryName.endsWith(".class") || entryName.endsWith(".jar"))) {
                File tmpFile = new File(tmpClasspath, entryName.replace(classPrefix, ""));
                FileUtils.writeByteArrayToFile(tmpFile, IOUtils.getBytes(jarFile.getInputStream(entry)));
                if (entryName.endsWith(".jar")) {
                    jarSearchPathSet.add(tmpFile);
                }
            }
        }
        classpathRootSet.add(tmpClasspath);
    }

    private static File getJarAbsolutePath(String jarFile, String userDir) {
        File file = new File(jarFile);
        if (file.exists()) {
            return file;
        }
        file = new File(userDir, jarFile);
        if (file.exists()) {
            return file;
        }
        return null;
    }
}
