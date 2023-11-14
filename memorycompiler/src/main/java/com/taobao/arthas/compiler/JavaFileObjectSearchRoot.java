package com.taobao.arthas.compiler;

import javax.tools.JavaFileObject;
import java.util.List;
import java.util.Set;

public interface JavaFileObjectSearchRoot {


    public List<JavaFileObject> search(String packageName, Set<JavaFileObject.Kind> kinds);
}
