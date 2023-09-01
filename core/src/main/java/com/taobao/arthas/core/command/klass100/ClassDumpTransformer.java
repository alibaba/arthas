package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author beiwei30 on 25/11/2016.
 */
class ClassDumpTransformer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(ClassDumpTransformer.class);

    private Set<Class<?>> classesToEnhance;
    private Map<Class<?>, File> dumpResult;
    private File arthasLogHome;

    private File directory;

    public ClassDumpTransformer(Set<Class<?>> classesToEnhance) {
        this(classesToEnhance, null);
    }

    public ClassDumpTransformer(Set<Class<?>> classesToEnhance, File directory) {
        this.classesToEnhance = classesToEnhance;
        this.dumpResult = new HashMap<Class<?>, File>();
        this.arthasLogHome = new File(LogUtil.loggingDir());
        this.directory = directory;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        if (classesToEnhance.contains(classBeingRedefined)) {
            dumpClassIfNecessary(classBeingRedefined, classfileBuffer);
        }
        return null;
    }

    public Map<Class<?>, File> getDumpResult() {
        return dumpResult;
    }

    public File dumpDir() {
        String classDumpDir = "classdump";
        final File dumpDir;
        if (directory != null) {
            dumpDir = directory;
        } else {
            dumpDir = new File(arthasLogHome, classDumpDir);
        }
        return dumpDir;
    }

    private void dumpClassIfNecessary(Class<?> clazz, byte[] data) {
        String className = clazz.getName();
        ClassLoader classLoader = clazz.getClassLoader();

        // 创建类所在的包路径
        File dumpDir = dumpDir();
        if (!dumpDir.mkdirs() && !dumpDir.exists()) {
            logger.warn("create dump directory:{} failed.", dumpDir.getAbsolutePath());
            return;
        }

        String fileName;
        if (classLoader != null) {
            fileName = classLoader.getClass().getName() + "-" + Integer.toHexString(classLoader.hashCode()) +
                    File.separator + className.replace(".", File.separator) + ".class";
        } else {
            fileName = className.replace(".", File.separator) + ".class";
        }

        File dumpClassFile = new File(dumpDir, fileName);

        // 将类字节码写入文件
        try {
            FileUtils.writeByteArrayToFile(dumpClassFile, data);
            dumpResult.put(clazz, dumpClassFile);
        } catch (IOException e) {
            logger.warn("dump class:{} to file {} failed.", className, dumpClassFile, e);
        }
    }
}
