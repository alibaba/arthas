package com.taobao.arthas.compiler;

import com.taobao.arthas.common.FileUtils;
import com.taobao.arthas.common.IOUtils;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class InnerJarClassSearchRoot implements JavaFileObjectSearchRoot {

    private final URI uri;

    private InnerJarIndex index;

    public InnerJarClassSearchRoot(URI uri) throws IOException {
        this.uri = uri;
        this.index = new InnerJarIndex();
    }

    @Override
    public List<JavaFileObject> search(String packageName, Set<JavaFileObject.Kind> kinds) throws IOException {
        if (kinds.contains(JavaFileObject.Kind.CLASS)) {
            return this.index.find(packageName);
        }
        return Collections.emptyList();
    }

    @Override
    public void close() {
        this.index.close();
    }

    public class InnerJarIndex {

        private boolean loaded;

        private Set<String> packages = new HashSet<>();

        private Map<String, PackageNameSearchRoot> packageNameSearchRoot = new HashMap<>();

        private File tmpFile;

        public InnerJarIndex() throws IOException {
            JarInputStream jarInputStream = new JarInputStream(uri.toURL().openStream());
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    String className = entryName
                            .substring(0, entryName.length() - ".class".length())
                            .replace("/", ".");
                    if (className.equals("package-info")
                            || className.equals("module-info")
                            || className.lastIndexOf(".") == -1) {
                        continue;
                    }
                    String packageName = className.substring(0, className.lastIndexOf("."));
                    packages.add(packageName);
                }
            }
        }

        public List<JavaFileObject> find(String packageName) throws IOException {
            if (this.packages.contains(packageName)) {
                if (!this.loaded) {
                    loadJar();
                }
                return this.packageNameSearchRoot.get(packageName).search(packageName, EnumSet.of(JavaFileObject.Kind.CLASS));
            }
            return Collections.emptyList();
        }

        private void loadJar() throws IOException {
            this.tmpFile = new File(FileUtils.getTempProcessDirectory(), UUID.randomUUID().toString() + ".jar");
            if (!this.tmpFile.exists()) {
                this.tmpFile.getParentFile().mkdirs();
            }
            FileUtils.writeByteArrayToFile(this.tmpFile, IOUtils.getBytes(uri.toURL().openStream()));
            try (PathJarFile jarFile = new PathJarFile(this.tmpFile)) {
                this.packageNameSearchRoot = PackageNameSearchRoot.load(jarFile);
            }
            this.loaded = true;
        }

        public void close() {
            // delete tmp file
            if (this.tmpFile != null) {
                tmpFile.delete();
            }
        }
    }
}
