package com.taobao.arthas.compiler;


import javax.tools.*;
import java.io.IOException;
import java.util.*;

public class DynamicJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private static final List<String> superLocationNames = Arrays.asList(
            /** JPMS StandardLocation.SYSTEM_MODULES **/
            "SYSTEM_MODULES");

    private final DynamicClassLoader classLoader;
    private final Set<JavaFileObjectSearchRoot> classpathRoots = new HashSet<>();
    private final List<MemoryByteCode> byteCodes = new ArrayList<MemoryByteCode>();

    public DynamicJavaFileManager(JavaFileManager fileManager, DynamicClassLoader classLoader) {
        super(fileManager);
        this.classLoader = classLoader;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                               JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        for (MemoryByteCode byteCode : byteCodes) {
            if (byteCode.getClassName().equals(className)) {
                return byteCode;
            }
        }
        MemoryByteCode innerClass = new MemoryByteCode(className);
        byteCodes.add(innerClass);
        classLoader.registerCompiledSource(innerClass);
        return innerClass;

    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return classLoader;
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
        if (location == StandardLocation.PLATFORM_CLASS_PATH || superLocationNames.contains(location.getName())) {
            return super.list(location, packageName, kinds, recurse);
        }
        if (location == StandardLocation.CLASS_PATH) {
            List<JavaFileObject> result = new ArrayList<>();
            for (JavaFileObjectSearchRoot classpathRoot : classpathRoots) {
                result.addAll(classpathRoot.search(packageName, kinds));
            }
            return new IterableJoin<>(super.list(location, packageName, kinds, recurse), result);
        }
        return super.list(location, packageName, kinds, recurse);
    }

    public void addClasspathRoots(Set<JavaFileObjectSearchRoot> classSearchRoots) {
        this.classpathRoots.addAll(classSearchRoots);
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
