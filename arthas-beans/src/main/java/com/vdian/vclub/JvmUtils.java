package com.vdian.vclub;

import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.OSUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;
import org.zeroturnaround.zip.ZipUtil;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.LinkedList;

/**
 * 如果是web环境，需要调用init方法。
 *
 * @author 1936978077@qq.com
 * @date 2021/02/12
 */
public class JvmUtils {

    private static final PathMatchingResourcePatternResolver LOADER = new PathMatchingResourcePatternResolver();

    private static String getSoLibName() throws Error {
        String so = null;
        if (OSUtils.isLinux()) {
            so = "jni-lib-linux-x64.so";
        } else if (OSUtils.isMac()) {
            so = "jni-lib-macos-x64.so";
        } else if (OSUtils.isWindows()) {
            //todo
        }
        if (StringUtils.isEmpty(so)) {
            throw new Error("MemoryAnalyzer is not supported in your operating system!");
        }
        return so;
    }

    static {
        try {
            String so = getSoLibName();
            Resource[] resources = LOADER.getResources("classpath*:/cpp/" + so);
            if (null == resources || resources.length == 0) {
                throw new IllegalStateException("jni-lib not found !");
            }
            for (Resource resource : resources) {
                try {
                    System.load(resource.getURL().getPath());
                } catch (Throwable ignored) {
                }
            }
            AnsiLog.warn("checkResult->" + check() + ", jni-lib available !");
        } catch (Throwable t) {
            AnsiLog.error("load jni-lib failed:" + t.getMessage(), t);
        }
    }

    /**
     * 检测jni-lib是否正常，如果正常，应该输出OK
     */
    public static native String check();

    /**
     * 获取某个class在jvm中当前所有存活实例
     */
    public static synchronized native <T> LinkedList<T> getInstances(Class<T> klass);

    /**
     * 统计某个class在jvm中当前所有存活实例的总占用内存，单位：Byte
     */
    public static synchronized native long sumInstanceSize(Class<?> klass);

    /**
     * 获取某个实例的占用内存，单位：Byte
     */
    public static native long getInstanceSize(Object instance);

    /**
     * 统计某个class在jvm中当前所有存活实例的总个数
     */
    public static synchronized native long countInstances(Class<?> klass);

    /**
     * 获取所有已加载的类
     */
    public static native LinkedList<Class<?>> getAllLoadedClasses();

    /**
     * 包括小类型(如int)
     */
    @SuppressWarnings("all")
    public static LinkedList<Class> getAllClasses() {
        return getInstances(Class.class);
    }

    public static String init(ServletContext servletContext) {
        try {
            String dirPath = servletContext.getRealPath("/WEB-INF/lib");
            String jarPath = dirPath + "/" + getJarName(dirPath);
            //解压到lib文件夹下
            ZipUtil.unpack(new File(jarPath), new File(dirPath));
            String so = getSoLibName();
            AnsiLog.warn("try to load " + dirPath + "/cpp/" + so);
            System.load(dirPath + "/cpp/" + so);
            return check();
        } catch (Throwable t) {
            t.printStackTrace();
            AnsiLog.error("load jni-lib failed with exception:{}", t.getMessage(), t);
        }
        return null;
    }

    private static String getJarName(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists()) {
            for (String name : dir.list()) {
                if (name.startsWith("arthas-beans-")) {
                    return name;
                }
            }
        }
        return null;
    }
}
