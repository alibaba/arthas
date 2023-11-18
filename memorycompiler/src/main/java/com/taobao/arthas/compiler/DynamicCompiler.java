package com.taobao.arthas.compiler;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class DynamicCompiler {
    private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
    private static final StandardJavaFileManager STANDARD_FILE_MANAGER = COMPILER == null
            ? null : COMPILER.getStandardFileManager(null, null, null);
    private static final Set<JavaFileObjectSearchRoot> CLASS_SEARCH_ROOTS = new HashSet<>();

    private final List<String> options = new ArrayList<>();

    private final List<String> processors = new ArrayList<>();

    private final List<String> processorsClassPath = new ArrayList<>();

    private final Collection<JavaFileObject> compilationUnits = new ArrayList<JavaFileObject>();

    static {
        if (STANDARD_FILE_MANAGER != null) {
            try {
                Set<File> jarSearchPathSet = new HashSet<>();
                Set<JavaFileObjectSearchRoot> classpathRootSet = new HashSet<>();
                String userDir = System.getProperty("user.dir");
                String[] classSearchPaths = System.getProperty("java.class.path").split(File.pathSeparator);
                if (classSearchPaths.length == 1 && isJarModeStart()) {
                    String startJar = classSearchPaths[0];
                    try (PathJarFile jarFile = new PathJarFile(getJarAbsolutePath(startJar, userDir))) {
                        Manifest manifest = jarFile.getManifest();
                        if (manifest != null) {
                            String classpath = manifest.getMainAttributes().getValue(new Attributes.Name("Class-Path"));
                            if (Objects.nonNull(classpath)) {
                                inner:
                                for (StringTokenizer st = new StringTokenizer(classpath); st.hasMoreTokens(); ) {
                                    String ele = st.nextToken();
                                    if (ele.startsWith("file:/")) ele = ele.substring(6);
                                    if (isJarFile(ele)) {
                                        File absolutePath = getJarAbsolutePath(ele, userDir);
                                        if (absolutePath != null) jarSearchPathSet.add(absolutePath);
                                        continue inner;
                                    }
                                    classpathRootSet.add(new ClasspathSearchRoot(ele));
                                }
                            }

                            // springboot的jar
                            String bootClasses = manifest.getMainAttributes().getValue(new Attributes.Name("Spring-Boot-Classes"));
                            String bootLibs = manifest.getMainAttributes().getValue(new Attributes.Name("Spring-Boot-Lib"));
                            if (Objects.nonNull(bootClasses) && Objects.nonNull(bootLibs)) {
                                PackageNameSearchRoot.loadBootJar(jarFile, bootClasses, bootLibs, classpathRootSet);
                            }
                        }
                    }
                    CLASS_SEARCH_ROOTS.addAll(classpathRootSet);
                    STANDARD_FILE_MANAGER.setLocation(StandardLocation.CLASS_PATH, jarSearchPathSet);
                }
                STANDARD_FILE_MANAGER.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, Collections.emptyList());
            } catch (Exception e) {
                // ignore
                e.printStackTrace();
            }
        }
    }

    private static boolean isJarFile(String classpath) {
        return classpath.endsWith(".jar") || classpath.endsWith(".zip");
    }

    private static boolean isJarModeStart() {
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-cp") || arg.startsWith("-classpath")) {
                return false;
            }
        }
        return true;
    }

    public DynamicCompiler() {
        if (COMPILER == null) {
            throw new IllegalStateException(
                    "Can not load JavaCompiler from javax.tools.ToolProvider#getSystemJavaCompiler(),"
                            + " please confirm the application running in JDK not JRE.");
        }
        options.add("-Xlint:unchecked");
    }

    public void addSource(String className, String source) {
        compilationUnits.add(new StringSource(className, source));
    }

    public Map<String, byte[]> buildByteCodes() throws ClassNotFoundException, IOException {
        if (compilationUnits.isEmpty()) {
            return null;
        }
        List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
        List<Diagnostic<? extends JavaFileObject>> warnings = new ArrayList<>();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();

        if (this.processors.size() > 0) {
            this.options.add("-processor");
            this.options.add(createAnnotationProcessor());
        }

        DynamicJavaFileManager fileManager = new DynamicJavaFileManager(STANDARD_FILE_MANAGER);
        fileManager.addClasspathRoots(CLASS_SEARCH_ROOTS);
        fileManager.addProcessorPath(processorsClassPath);

        JavaCompiler.CompilationTask task = COMPILER.getTask(null, fileManager, collector, options, null, compilationUnits);
        try {
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
            return fileManager.getArtifacts();
        } catch (Throwable e) {
            throw new DynamicCompilerException(e, errors);
        } finally {
            compilationUnits.clear();
        }
    }

    private String createAnnotationProcessor() throws ClassNotFoundException {
        return String.join(",", this.processors);
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

    public void addOption(String k, String v) {
        this.options.add(k);
        this.options.add(v);
    }

    public void addProcessor(String processor) {
        this.processors.add(processor);
    }

    public void addProcessorPath(String processorPath) {
        this.processorsClassPath.add(processorPath);
    }
}
