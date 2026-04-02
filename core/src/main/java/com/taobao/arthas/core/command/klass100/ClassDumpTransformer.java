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
 * 类转储转换器
 *
 * 该类实现了Java Instrumentation API的ClassFileTransformer接口，
 * 用于在类加载或重定义时将类的字节码转储到文件系统中。
 * 主要用于Arthas的类转储功能，可以将JVM中已加载的类保存到磁盘文件。
 *
 * @author beiwei30 on 25/11/2016.
 */
class ClassDumpTransformer implements ClassFileTransformer {

    /**
     * 日志记录器，用于记录转储过程中的信息和错误
     */
    private static final Logger logger = LoggerFactory.getLogger(ClassDumpTransformer.class);

    /**
     * 需要增强（转储）的类集合
     * 当这些类被加载或重定义时，会触发转储操作
     */
    private Set<Class<?>> classesToEnhance;

    /**
     * 转储结果映射表
     * 记录每个类对应的转储文件，键为Class对象，值为转储的File对象
     */
    private Map<Class<?>, File> dumpResult;

    /**
     * Arthas日志目录
     * 默认的类转储目录的父目录
     */
    private File arthasLogHome;

    /**
     * 用户指定的转储目录
     * 如果不为null，则类文件将转储到此目录；否则转储到arthasLogHome下的classdump目录
     */
    private File directory;

    /**
     * 构造函数：使用默认目录创建类转储转换器
     *
     * @param classesToEnhance 需要转储的类集合
     */
    public ClassDumpTransformer(Set<Class<?>> classesToEnhance) {
        // 调用带目录参数的构造函数，目录参数传null表示使用默认目录
        this(classesToEnhance, null);
    }

    /**
     * 构造函数：使用指定目录创建类转储转换器
     *
     * @param classesToEnhance 需要转储的类集合
     * @param directory 转储目录，如果为null则使用默认目录
     */
    public ClassDumpTransformer(Set<Class<?>> classesToEnhance, File directory) {
        this.classesToEnhance = classesToEnhance;
        // 初始化转储结果映射表
        this.dumpResult = new HashMap<Class<?>, File>();
        // 获取Arthas日志目录作为默认转储目录
        this.arthasLogHome = new File(LogUtil.loggingDir());
        // 保存用户指定的转储目录
        this.directory = directory;
    }

    /**
     * 转换类文件
     *
     * 该方法在Java Instrumentation框架中，当类被加载或重定义时被调用。
     * 如果当前类在需要转储的类集合中，则将其字节码写入文件。
     *
     * @param loader 定义类的类加载器，如果是引导类加载器则为null
     * @param className 类的全限定名，使用'/'分隔而非'.'
     * @param classBeingRedefined 被重定义的类，如果是首次加载则为null
     * @param protectionDomain 类的保护域
     * @param classfileBuffer 类的字节码数据
     * @return 返回null表示不修改类的字节码
     * @throws IllegalClassFormatException 如果类文件格式错误
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        // 检查当前类是否在需要转储的类集合中
        if (classesToEnhance.contains(classBeingRedefined)) {
            // 如果是，则执行类转储操作
            dumpClassIfNecessary(classBeingRedefined, classfileBuffer);
        }
        // 返回null表示不修改类的字节码
        return null;
    }

    /**
     * 获取转储结果
     *
     * @return 转储结果映射表，包含每个类对应的转储文件
     */
    public Map<Class<?>, File> getDumpResult() {
        return dumpResult;
    }

    /**
     * 获取类转储目录
     *
     * 如果用户指定了目录则返回用户指定的目录，
     * 否则返回Arthas日志目录下的classdump子目录。
     *
     * @return 类转储目录
     */
    public File dumpDir() {
        // 默认的类转储子目录名称
        String classDumpDir = "classdump";
        final File dumpDir;
        // 如果用户指定了目录，则使用用户指定的目录
        if (directory != null) {
            dumpDir = directory;
        } else {
            // 否则使用Arthas日志目录下的classdump子目录
            dumpDir = new File(arthasLogHome, classDumpDir);
        }
        return dumpDir;
    }

    /**
     * 如果需要则转储类文件
     *
     * 该方法将类的字节码写入到文件系统中，文件路径根据类加载器和类名生成。
     * 对于有类加载器的类，文件路径包含类加载器信息以避免冲突。
     *
     * @param clazz 要转储的类对象
     * @param data 类的字节码数据
     */
    private void dumpClassIfNecessary(Class<?> clazz, byte[] data) {
        // 获取类的全限定名
        String className = clazz.getName();
        // 获取加载该类的类加载器
        ClassLoader classLoader = clazz.getClassLoader();

        // 创建类所在的包路径
        File dumpDir = dumpDir();
        // 尝试创建目录（包括所有必需的父目录）
        if (!dumpDir.mkdirs() && !dumpDir.exists()) {
            // 如果目录创建失败且目录不存在，记录警告并返回
            logger.warn("create dump directory:{} failed.", dumpDir.getAbsolutePath());
            return;
        }

        // 构建转储文件的文件名
        String fileName;
        if (classLoader != null) {
            // 对于有类加载器的类，文件名包含类加载器信息
            // 格式：类加载器类名-类加载器哈希码/类全限定名.class
            fileName = classLoader.getClass().getName() + "-" + Integer.toHexString(classLoader.hashCode()) +
                    File.separator + className.replace(".", File.separator) + ".class";
        } else {
            // 对于Bootstrap类加载器加载的类，直接使用类名
            fileName = className.replace(".", File.separator) + ".class";
        }

        // 创建转储文件对象
        File dumpClassFile = new File(dumpDir, fileName);

        // 将类字节码写入文件
        try {
            // 使用工具类将字节数组写入文件
            FileUtils.writeByteArrayToFile(dumpClassFile, data);
            // 将转储结果保存到映射表中
            dumpResult.put(clazz, dumpClassFile);
        } catch (IOException e) {
            // 如果写入失败，记录警告信息
            logger.warn("dump class:{} to file {} failed.", className, dumpClassFile, e);
        }
    }
}
