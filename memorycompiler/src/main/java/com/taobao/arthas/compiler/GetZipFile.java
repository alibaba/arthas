package com.taobao.arthas.compiler;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class GetZipFile extends ZipFile {

    public GetZipFile(String name) throws IOException {
        super(name);
    }

    public GetZipFile(File file, int mode) throws IOException {
        super(file, mode);
    }

    public GetZipFile(File file) throws ZipException, IOException {
        super(file);
    }
    
    public Stream<ZipEntry> stream() {
        Enumeration<? extends ZipEntry> entries = super.entries();
        List<ZipEntry> entryList = new ArrayList<>();
        while (entries.hasMoreElements()) {
            entryList.add(entries.nextElement());
        }
        return entryList.stream();
    }

    public class ZipJavaFileObject implements JavaFileObject {
        private final String className;
        private final URI uri;
        private final ZipEntry file;

        public ZipJavaFileObject(String className, URI uri, ZipEntry file) {
            this.uri = uri;
            this.className = className;
            this.file = file;
        }

        public URI toUri() {
            return uri;
        }

        public InputStream openInputStream() throws IOException {
            return GetZipFile.this.getInputStream(file);
        }

        public OutputStream openOutputStream() {
            throw new UnsupportedOperationException();
        }

        public String getName() {
            return this.className;
        }

        public Reader openReader(boolean ignoreEncodingErrors) {
            throw new UnsupportedOperationException();
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            throw new UnsupportedOperationException();
        }

        public Writer openWriter() throws IOException {
            throw new UnsupportedOperationException();
        }

        public long getLastModified() {
            return 0;
        }

        public boolean delete() {
            throw new UnsupportedOperationException();
        }

        public Kind getKind() {
            return Kind.CLASS;
        }

        public boolean isNameCompatible(String simpleName, Kind kind) {
            return Kind.CLASS.equals(getKind())
                    && this.className.endsWith(simpleName);
        }

        public NestingKind getNestingKind() {
            throw new UnsupportedOperationException();
        }

        public Modifier getAccessLevel() {
            throw new UnsupportedOperationException();
        }

        public String getClassName() {
            return this.className;
        }


        public String toString() {
            return this.getClass().getName() + "[" + this.toUri() + "]";
        }
    }
}
