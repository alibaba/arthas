package com.taobao.arthas.compiler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.jar.JarFile;

public class PathJarFile extends JarFile {

    private File file;

    public PathJarFile(File file) throws IOException {
        super(file);
        this.file = file;
    }


    public String getPath() {
        return file.getPath();
    }

    /**
     * 获取文件在jar包中的相对路径
     * @param relativePaths
     * @return
     */
    public URI getFileUri(String relativePaths) {
        return URI.create(String.format("jar:%s!/%s", file.toURI(), relativePaths));
    }
}
