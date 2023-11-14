package com.taobao.arthas.compiler;

import javax.tools.*;
import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

public class DynamicCompiler {
    private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
    private static final StandardJavaFileManager STANDARD_FILE_MANAGER = COMPILER == null
            ? null : COMPILER.getStandardFileManager(null, null, null);
    private static final Set<JavaFileObjectSearchRoot> CLASS_SEARCH_ROOTS = new HashSet<>();

    private final List<String> options = new ArrayList<>();

    private final DynamicClassLoader dynamicClassLoader;

    private final Collection<JavaFileObject> compilationUnits = new ArrayList<JavaFileObject>();

    static {
        if (STANDARD_FILE_MANAGER != null) {
            try {
                enhanceCompiler();

                Set<File> jarSearchPathSet = new HashSet<>();
                Set<JavaFileObjectSearchRoot> classpathRootSet = new HashSet<>();
                String userDir = System.getProperty("user.dir");
                String[] jars = System.getProperty("java.class.path").split(File.pathSeparator);
                for (String jarPath : jars) {
                    try (PathJarFile jarFile = new PathJarFile(getJarAbsolutePath(jarPath, userDir))) {
                        Manifest manifest = jarFile.getManifest();
                        if (manifest == null) {
                            continue;
                        }
                        String classpath = manifest.getMainAttributes().getValue(new Attributes.Name("Class-Path"));
                        if (Objects.nonNull(classpath)) {
                            inner:
                            for (StringTokenizer st = new StringTokenizer(classpath); st.hasMoreTokens(); ) {
                                String ele = st.nextToken();
                                if (ele.startsWith("file:/")) ele = ele.substring(6);
                                if (ele.endsWith(".jar") || ele.endsWith(".zip")) {
                                    File absolutePath = getJarAbsolutePath(ele, userDir);
                                    if (absolutePath != null) jarSearchPathSet.add(absolutePath);
                                    continue inner;
                                }
                                classpathRootSet.add(new ClasspathObjectSearchRoot(ele));
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
                CLASS_SEARCH_ROOTS.addAll(classpathRootSet);
                STANDARD_FILE_MANAGER.setLocation(StandardLocation.CLASS_PATH, jarSearchPathSet);
            } catch (Exception e) {
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
        compilationUnits.add(new StringSource(className, source));
    }

    public Map<String, byte[]> buildByteCodes() {
        if (compilationUnits.isEmpty()) {
            return null;
        }
        List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
        List<Diagnostic<? extends JavaFileObject>> warnings = new ArrayList<>();

        DynamicJavaFileManager fileManager = new DynamicJavaFileManager(STANDARD_FILE_MANAGER, this.dynamicClassLoader);
        fileManager.addClasspathRoots(CLASS_SEARCH_ROOTS);

        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
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
            return dynamicClassLoader.getByteCodes();
        } catch (ClassFormatError e) {
            throw new DynamicCompilerException(e, errors);
        } finally {
            compilationUnits.clear();
        }
    }

    public static void enhanceCompiler() {

    }

    private static void handleBootJar(PathJarFile jarFile, String classPrefix, String libPrefix, Set<File> jarSearchPathSet, Set<JavaFileObjectSearchRoot> classpathRootSet) {
        Map<String, PackageInnerClassObjectSearchRoot> packages = new HashMap<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entryName.startsWith(libPrefix) && entryName.endsWith(".jar")) {
                jarSearchPathSet.add(new ZipInnerJarFile(jarFile.getPath(), entryName));
            } else if (entryName.startsWith(classPrefix) && entryName.endsWith(".class")) {
                String className = entryName
                        .substring(0, entryName.length() - ".class".length())
                        .replace(classPrefix, "")
                        .replace("/", ".");
                String packageName = className.substring(0, className.lastIndexOf("."));
                PackageInnerClassObjectSearchRoot packageRoot = packages.get(packageName);
                if (packageRoot == null) {
                    packageRoot = new PackageInnerClassObjectSearchRoot(packageName);
                    packages.put(packageName, packageRoot);
                }
                URI fileUri = jarFile.getFileUri(entryName);
                packageRoot.addClassFile(className, fileUri);
            }
        }
        classpathRootSet.addAll(packages.values());
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
