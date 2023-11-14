package com.taobao.arthas.compiler.javac;

import com.sun.tools.javac.file.FSInfo;
import com.taobao.arthas.compiler.ZipInnerJarFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ArthasFsInfo extends FSInfo {

    @Override
    public List<File> getJarClassPath(File jarfile) throws IOException {
        if (jarfile instanceof ZipInnerJarFile) {
            return getJarClassPath0((ZipInnerJarFile) jarfile);
        }
        return super.getJarClassPath(jarfile);
    }

    public List<File> getJarClassPath0(ZipInnerJarFile jarFile) {
        return null;
    }
}
