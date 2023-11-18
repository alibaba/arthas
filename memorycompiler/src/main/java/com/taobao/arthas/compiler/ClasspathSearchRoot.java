package com.taobao.arthas.compiler;

import javax.tools.JavaFileObject;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class ClasspathSearchRoot implements JavaFileObjectSearchRoot {

    private String classpath;

    public ClasspathSearchRoot(String classpath) {
        this.classpath = classpath;
    }

    public List<JavaFileObject> search(String packageName, Set<JavaFileObject.Kind> kinds) {
        File packageFile = new File(this.classpath, packageName);
        if (packageFile.exists() && packageFile.isDirectory()) {
            File[] files = packageFile.listFiles(item ->
                    !item.isDirectory()
                            && kinds.contains(DynamicJavaFileManager.getKind(item.getName())
                    ));
            if (files != null) {
                return Arrays.stream(files).map(CustomJavaFileObject::new).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
