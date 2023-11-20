package com.taobao.arthas.compiler;


import javax.tools.JavaFileObject;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

public class PackageNameSearchRoot implements JavaFileObjectSearchRoot {

    private String packageName;

    private Set<ClassUriWrapper> classes;

    public PackageNameSearchRoot(String packageName) {
        this.packageName = packageName;
        this.classes = new HashSet<>();
    }

    public static Map<String, PackageNameSearchRoot> loadJar(PathJarFile jarFile) throws IOException {
        Map<String, PackageNameSearchRoot> packages = new HashMap<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith(".class")) {
                String className = entryName
                        .substring(0, entryName.length() - ".class".length())
                        .replace("/", ".");
                if (className.equals("package-info")
                        || className.equals("module-info")
                        || className.lastIndexOf(".") == -1) {
                    continue;
                }
                String packageName = className.substring(0, className.lastIndexOf("."));
                PackageNameSearchRoot packageRoot = packages.get(packageName);
                if (packageRoot == null) {
                    packageRoot = new PackageNameSearchRoot(packageName);
                    packages.put(packageName, packageRoot);
                }
                URI fileUri = jarFile.getFileUri(entryName);
                packageRoot.addClassFile(className, fileUri);
            }
        }
        return packages;
    }

    public static Set<JavaFileObjectSearchRoot> loadBootJar(PathJarFile jarFile, String classPrefix, String libPrefix) throws IOException {
        Set<JavaFileObjectSearchRoot> classpathRootSet = new HashSet<>();
        Map<String, PackageNameSearchRoot> packages = new HashMap<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entryName.startsWith(libPrefix) && entryName.endsWith(".jar")) {
                classpathRootSet.add(new InnerJarClassSearchRoot(jarFile.getFileUri(entry.getName())));
            } else if (entryName.startsWith(classPrefix) && entryName.endsWith(".class")) {
                String className = entryName
                        .substring(0, entryName.length() - ".class".length())
                        .replace(classPrefix, "")
                        .replace("/", ".");
                if (className.equals("package-info")
                        || className.equals("module-info")
                        || className.lastIndexOf(".") == -1) {
                    continue;
                }
                String packageName = className.substring(0, className.lastIndexOf("."));
                PackageNameSearchRoot packageRoot = packages.get(packageName);
                if (packageRoot == null) {
                    packageRoot = new PackageNameSearchRoot(packageName);
                    packages.put(packageName, packageRoot);
                }
                URI fileUri = jarFile.getFileUri(entryName);
                packageRoot.addClassFile(className, fileUri);
            }
        }
        classpathRootSet.addAll(packages.values());
        return classpathRootSet;
    }

    public void addClassFile(String className, URI uri) {
        this.classes.add(new ClassUriWrapper(className, uri));
    }

    public void addClassFile(String className, Supplier<byte[]> supplier) {
        this.classes.add(new ClassUriWrapper(className, null));
    }

    @Override
    public List<JavaFileObject> search(String searchPackageName, Set<JavaFileObject.Kind> kinds) {
        if (kinds.contains(JavaFileObject.Kind.CLASS) && searchPackageName.equals(this.packageName)) {
            return classes.stream()
                    .map(item -> new CustomJavaFileObject(item.getUri(), item.getClassName(), JavaFileObject.Kind.CLASS))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
