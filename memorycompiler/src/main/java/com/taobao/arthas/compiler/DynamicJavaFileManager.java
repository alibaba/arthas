package com.taobao.arthas.compiler;


import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class DynamicJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private final Set<JavaFileObjectSearchRoot> classpathRoots = new HashSet<>();
    private final List<URL> processorPath = new ArrayList<>();
    private final List<MemoryByteCode> byteCodes = new ArrayList<>();

    public DynamicJavaFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                               JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        for (MemoryByteCode byteCode : this.byteCodes) {
            if (byteCode.getClassName().equals(className)) {
                return byteCode;
            }
        }
        MemoryByteCode innerClass = new MemoryByteCode(className);
        this.byteCodes.add(innerClass);
        return innerClass;

    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        if (location == StandardLocation.ANNOTATION_PROCESSOR_PATH) {
            return new AnnotationProcessorClassloader(processorPath.toArray(new URL[0]), DynamicJavaFileManager.class.getClassLoader());
        }
         return ClassLoader.getSystemClassLoader();
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof CustomJavaFileObject) {
            return ((CustomJavaFileObject) file).getName();
        } else {
            /**
             * if it's not CustomJavaFileObject, then it's coming from standard file manager
             * - let it handle the file
             */
            return super.inferBinaryName(location, file);
        }
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds,
                                         boolean recurse) throws IOException {
        if (location == StandardLocation.PLATFORM_CLASS_PATH || location.getName().startsWith("SYSTEM_MODULES")) {
            return super.list(location, packageName, kinds, recurse);
        }
        if (location == StandardLocation.CLASS_PATH) {
            List<JavaFileObject> result = new ArrayList<>();
            for (JavaFileObjectSearchRoot classpathRoot : this.classpathRoots) {
                result.addAll(classpathRoot.search(packageName, kinds));
            }
            return new IterableJoin<>(super.list(location, packageName, kinds, recurse), result);
        }
        return super.list(location, packageName, kinds, recurse);
    }

    public void addClasspathRoots(Set<JavaFileObjectSearchRoot> classSearchRoots) {
        this.classpathRoots.addAll(classSearchRoots);
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.classpathRoots.forEach(JavaFileObjectSearchRoot::close);
    }

    public Map<String, byte[]> getArtifacts() {
        return this.byteCodes.stream()
                .collect(Collectors.toMap(MemoryByteCode::getName, MemoryByteCode::getByteCode));
    }

    public void addProcessorPath(List<String> processorsClassPath) throws IOException {
        for (String processorJarPath : processorsClassPath) {
            File processorJar = new File(processorJarPath);
            if (processorJar.exists()) {
                Map<String, PackageNameSearchRoot> loaded = PackageNameSearchRoot.load(new PathJarFile(processorJar));
                this.classpathRoots.addAll(loaded.values());
                this.processorPath.add(processorJar.toURI().toURL());
            }
        }
    }

    static class IterableJoin<T> implements Iterable<T> {
        private final Iterable<T> first, next;

        public IterableJoin(Iterable<T> first, Iterable<T> next) {
            this.first = first;
            this.next = next;
        }

        @Override
        public Iterator<T> iterator() {
            return new IteratorJoin<T>(first.iterator(), next.iterator());
        }
    }

    static class IteratorJoin<T> implements Iterator<T> {
        private final Iterator<T> first, next;

        public IteratorJoin(Iterator<T> first, Iterator<T> next) {
            this.first = first;
            this.next = next;
        }

        @Override
        public boolean hasNext() {
            return first.hasNext() || next.hasNext();
        }

        @Override
        public T next() {
            if (first.hasNext()) {
                return first.next();
            }
            return next.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }

    public static JavaFileObject.Kind getKind(String name) {
        if (name.endsWith(JavaFileObject.Kind.CLASS.extension))
            return JavaFileObject.Kind.CLASS;
        else if (name.endsWith(JavaFileObject.Kind.SOURCE.extension))
            return JavaFileObject.Kind.SOURCE;
        else if (name.endsWith(JavaFileObject.Kind.HTML.extension))
            return JavaFileObject.Kind.HTML;
        else
            return JavaFileObject.Kind.OTHER;
    }
}
