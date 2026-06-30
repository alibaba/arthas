package com.taobao.arthas.compiler;

/*-
 * #%L
 * compiler
 * %%
 * Copyright (C) 2017 - 2018 SkaLogs
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

public class PackageInternalsFinder {
    private final ClassLoader classLoader;
    private static final String CLASS_FILE_EXTENSION = ".class";

    private static final Map<String, JarFileIndex> INDEXS = new ConcurrentHashMap<>();

    public PackageInternalsFinder(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public List<JavaFileObject> find(String packageName) throws IOException {
        String javaPackageName = packageName.replaceAll("\\.", "/");

        List<JavaFileObject> result = new ArrayList<JavaFileObject>();

        Enumeration<URL> urlEnumeration = classLoader.getResources(javaPackageName);
        while (urlEnumeration.hasMoreElements()) { // one URL for each jar on the classpath that has the given package
            URL packageFolderURL = urlEnumeration.nextElement();
            result.addAll(listUnder(packageName, packageFolderURL));
        }

        return result;
    }

    private Collection<JavaFileObject> listUnder(String packageName, URL packageFolderURL) {
        File directory = new File(decode(packageFolderURL.getFile()));
        if (directory.isDirectory()) { // browse local .class files - useful for local execution
            return processDir(packageName, directory);
        } else { // browse a jar file
            return processJar(packageName, packageFolderURL);
        }
    }

    private List<JavaFileObject> processJar(String packageName, URL packageFolderURL) {
        try {
            String jarUri = packageFolderURL.toExternalForm().substring(0, packageFolderURL.toExternalForm().lastIndexOf("!/"));
            JarFileIndex jarFileIndex = INDEXS.get(jarUri);
            if (jarFileIndex == null) {
                jarFileIndex = new JarFileIndex(jarUri, URI.create(jarUri + "!/"));
                INDEXS.put(jarUri, jarFileIndex);
            }
            List<JavaFileObject> result = jarFileIndex.search(packageName);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            // ignore
        }
        // 保底
        return fuse(packageFolderURL);
    }

    private List<JavaFileObject> fuse(URL packageFolderURL) {
        List<JavaFileObject> result = new ArrayList<JavaFileObject>();
        try {
            String jarUri = packageFolderURL.toExternalForm().substring(0, packageFolderURL.toExternalForm().lastIndexOf("!/"));

            JarURLConnection jarConn = (JarURLConnection) packageFolderURL.openConnection();
            String rootEntryName = jarConn.getEntryName();

            if (rootEntryName != null) {
                //可能为 null（自己没有类文件时）
                int rootEnd = rootEntryName.length() + 1;

                Enumeration<JarEntry> entryEnum = jarConn.getJarFile().entries();
                while (entryEnum.hasMoreElements()) {
                    JarEntry jarEntry = entryEnum.nextElement();
                    String name = jarEntry.getName();
                    if (name.startsWith(rootEntryName) && name.indexOf('/', rootEnd) == -1 && name.endsWith(CLASS_FILE_EXTENSION)) {
                        URI uri = URI.create(jarUri + "!/" + name);
                        String binaryName = name.replaceAll("/", ".");
                        binaryName = binaryName.replaceAll(CLASS_FILE_EXTENSION + "$", "");

                        result.add(new CustomJavaFileObject(binaryName, uri));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Wasn't able to open " + packageFolderURL + " as a jar file", e);
        }
        return result;
    }

    private List<JavaFileObject> processDir(String packageName, File directory) {
        File[] files = directory.listFiles(item ->
                item.isFile() && getKind(item.getName()) == JavaFileObject.Kind.CLASS);
        if (files != null) {
            return Arrays.stream(files).map(item -> {
                String className = packageName + "." + item.getName()
                        .replaceAll(CLASS_FILE_EXTENSION + "$", "");
                return new CustomJavaFileObject(className, item.toURI());
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String decode(String filePath) {
        try {
            return URLDecoder.decode(filePath, "utf-8");
        } catch (Exception e) {
            // ignore, return original string
        }

        return filePath;
    }

    public static JavaFileObject.Kind getKind(String name) {
        if (name.endsWith(JavaFileObject.Kind.CLASS.extension))
            return JavaFileObject.Kind.CLASS;
        else if (name.endsWith(JavaFileObject.Kind.SOURCE.extension))
            return JavaFileObject.Kind.SOURCE;
        else if (name.endsWith(JavaFileObject.Kind.HTML.extension))
            return JavaFileObject.Kind.HTML;
        else
            return JavaFileObject.Kind.OTHER;
    }

    public static class JarFileIndex {
        private String jarUri;
        private URI uri;

        private Map<String, List<ClassUriWrapper>> packages = new HashMap<>();

        public JarFileIndex(String jarUri, URI uri) throws IOException {
            this.jarUri = jarUri;
            this.uri = uri;
            loadIndex();
        }

        private void loadIndex() throws IOException {
            JarURLConnection jarConn = (JarURLConnection) uri.toURL().openConnection();
            String rootEntryName = jarConn.getEntryName() == null ? "" : jarConn.getEntryName();
            Enumeration<JarEntry> entryEnum = jarConn.getJarFile().entries();
            while (entryEnum.hasMoreElements()) {
                JarEntry jarEntry = entryEnum.nextElement();
                String entryName = jarEntry.getName();
                if (entryName.startsWith(rootEntryName) && entryName.endsWith(CLASS_FILE_EXTENSION)) {
                    String className = entryName
                            .substring(0, entryName.length() - CLASS_FILE_EXTENSION.length())
                            .replace(rootEntryName, "")
                            .replace("/", ".");
                    if (className.startsWith(".")) className = className.substring(1);
                    if (className.equals("package-info")
                            || className.equals("module-info")
                            || className.lastIndexOf(".") == -1) {
                        continue;
                    }
                    String packageName = className.substring(0, className.lastIndexOf("."));
                    List<ClassUriWrapper> classes = packages.get(packageName);
                    if (classes == null) {
                        classes = new ArrayList<>();
                        packages.put(packageName, classes);
                    }
                    classes.add(new ClassUriWrapper(className, URI.create(jarUri + "!/" + entryName)));
                }
            }
        }

        public List<JavaFileObject> search(String packageName) {
            if (this.packages.isEmpty()) {
                return null;
            }
            if (this.packages.containsKey(packageName)) {
                return packages.get(packageName).stream().map(item -> {
                    return new CustomJavaFileObject(item.getClassName(), item.getUri());
                }).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }
}
