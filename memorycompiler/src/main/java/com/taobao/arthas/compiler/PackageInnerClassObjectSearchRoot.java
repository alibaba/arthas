package com.taobao.arthas.compiler;


import javax.tools.JavaFileObject;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PackageInnerClassObjectSearchRoot implements JavaFileObjectSearchRoot {

    private String packageName;

    private Set<ClassUrlWrapper> classes;

    public PackageInnerClassObjectSearchRoot(String packageName) {
        this.packageName = packageName;
        this.classes = new HashSet<>();
    }

    public void addClassFile(String className, URI uri) {
        this.classes.add(new ClassUrlWrapper(className, uri));
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
