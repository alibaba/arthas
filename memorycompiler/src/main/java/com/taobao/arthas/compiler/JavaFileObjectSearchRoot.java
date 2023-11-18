package com.taobao.arthas.compiler;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface JavaFileObjectSearchRoot {


    public List<JavaFileObject> search(String packageName, Set<JavaFileObject.Kind> kinds) throws IOException;

    default void close() {

    }
}
