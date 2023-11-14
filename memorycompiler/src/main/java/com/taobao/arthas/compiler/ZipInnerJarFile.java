package com.taobao.arthas.compiler;

import java.io.File;

public class ZipInnerJarFile extends File {

    private String innerJarPath;

    public ZipInnerJarFile(String path, String innerJarPath) {
        super(path);
        this.innerJarPath = innerJarPath;
    }

    @Override
    public String getPath() {
        return super.getPath() + "!/" + this.innerJarPath;
    }

//    @Override
//    public boolean equals(Object obj) {
//        if ((obj != null) && (obj instanceof ZipInnerJarFile)) {
//            ZipInnerJarFile to = (ZipInnerJarFile) obj;
//            return Objects.equals(to.getPath(), this.getPath())
//                    && Objects.equals(to.innerJarPath, this.innerJarPath);
//        }
//        return false;
//    }
}
