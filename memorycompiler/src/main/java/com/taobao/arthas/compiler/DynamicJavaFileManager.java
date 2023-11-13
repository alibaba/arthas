package com.taobao.arthas.compiler;


import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

public class DynamicJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private static final List<String> superLocationNames = Arrays.asList(
            /** JPMS StandardLocation.SYSTEM_MODULES **/
            "SYSTEM_MODULES");
    private final DynamicClassLoader classLoader;
    private final Set<String> classpathRoots;
    private final List<MemoryByteCode> byteCodes = new ArrayList<MemoryByteCode>();

    public DynamicJavaFileManager(JavaFileManager fileManager, Set<String> classpathRoots, DynamicClassLoader classLoader) {
        super(fileManager);
        this.classpathRoots = classpathRoots;
        this.classLoader = classLoader;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
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
    public ClassLoader getClassLoader(JavaFileManager.Location location) {
        return classLoader;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof CustomJavaFileObject) {
            return ((CustomJavaFileObject) file).getClassName();
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
            for (String root : classpathRoots) {
                File packageFile = new File(root, packageName.replace('.', '/'));
                if (packageFile.exists() && packageFile.isDirectory()) {
                    File[] files = packageFile.listFiles(item ->
                            !item.isDirectory()
                                    && kinds.contains(getKind(item.getName())
                            ));
                    for (File classFile : files) {
                        result.add(new CustomJavaFileObject(classFile));
                    }
                }
            }
            return new IterableJoin<>(super.list(location, packageName, kinds, recurse), result);
        }
        return super.list(location, packageName, kinds, recurse);
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
