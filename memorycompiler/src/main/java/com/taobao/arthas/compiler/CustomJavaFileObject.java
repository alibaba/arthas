package com.taobao.arthas.compiler;

import com.taobao.arthas.common.FileUtils;

import javax.tools.SimpleJavaFileObject;
import java.io.*;

public class CustomJavaFileObject extends SimpleJavaFileObject {

    private File file;

    public CustomJavaFileObject(File file) {
        super(file.toURI(), DynamicJavaFileManager.getKind(file.getName()));
        this.file = file;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return new String(FileUtils.readFileToByteArray(file));
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return new FileOutputStream(super.uri.getPath());
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(super.uri.getPath());
    }

    public String getClassName() {
        String fileName = this.file.getName();
        int index = fileName.lastIndexOf("/");
        if (index != -1) {
            fileName = fileName.substring(index + 1);
        }
        index = fileName.lastIndexOf(".");
        if (index != -1) {
            return fileName.substring(0, index);
        }
        return fileName;
    }
}
