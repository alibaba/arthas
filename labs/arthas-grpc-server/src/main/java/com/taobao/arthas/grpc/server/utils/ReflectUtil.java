package com.taobao.arthas.grpc.server.utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射工具类
 *
 * 该类提供了一系列基于反射机制的实用工具方法，主要用于在运行时动态地查找和加载类。
 * 核心功能是根据包名扫描并加载该包下所有的类文件。
 *
 * 主要应用场景：
 * - 在运行时动态发现和加载特定包下的所有类
 * - 支持插件化架构的类扫描和加载
 * - 为Arthas gRPC服务器提供类发现能力
 *
 * @author FengYe
 * @date 2024/9/6 02:20
 * @description 反射工具类
 */
public class ReflectUtil {

    /**
     * 根据包名查找并加载该包下的所有类
     *
     * 该方法通过扫描指定包对应的文件系统目录，查找所有的.class文件，
     * 并使用反射机制将其加载到JVM中，返回所有加载成功的Class对象列表。
     *
     * 实现步骤：
     * 1. 将包名中的点号替换为路径分隔符，转换为文件系统路径
     * 2. 通过当前线程的上下文类加载器获取包对应的URL资源
     * 3. 将URL转换为文件系统目录
     * 4. 遍历目录下的所有文件
     * 5. 筛选出以.class结尾的文件
     * 6. 将文件名转换为完整的类名（包名 + 类名）
     * 7. 使用Class.forName()加载类
     *
     * 注意事项：
     * - 只能加载文件系统中存在的类文件，无法加载jar包中的类
     * - 如果类加载失败（如缺少依赖），会捕获异常但不中断处理流程
     * - 返回的列表只包含成功加载的类
     *
     * @param packageName 要扫描的包名，格式如 "com.example.package"
     * @return 包下所有成功加载的Class对象列表，如果没有找到任何类或发生异常，返回空列表
     */
    public static List<Class<?>> findClasses(String packageName) {
        // 创建用于存储加载的Class对象的列表
        List<Class<?>> classes = new ArrayList<>();

        // 将包名转换为文件系统路径格式（将包名中的点替换为斜杠）
        String path = packageName.replace('.', '/');

        try {
            // 获取当前线程的上下文类加载器
            // 使用getContextClassLoader()而不是类自己的类加载器，以便更好地适应不同的类加载环境
            // 通过类加载器获取包路径对应的URL资源
            URL resource = Thread.currentThread().getContextClassLoader().getResource(path);

            // 检查资源是否存在
            if (resource != null) {
                // 将URL转换为URI，再转换为File对象
                File directory = new File(resource.toURI());

                // 检查目录是否存在
                if (directory.exists()) {
                    // 遍历目录下的所有文件
                    for (File file : directory.listFiles()) {
                        // 筛选条件：必须是文件且文件名以.class结尾
                        if (file.isFile() && file.getName().endsWith(".class")) {
                            // 构建完整的类名：包名 + "." + 去掉.class后缀的文件名
                            String className = packageName + '.' + file.getName().replace(".class", "");

                            // 使用反射加载类
                            // Class.forName()会触发类的静态初始化
                            classes.add(Class.forName(className));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 捕获所有异常，包括：
            // - URISyntaxException：URI转换失败
            // - ClassNotFoundException：类加载失败
            // - NoClassDefFoundError：类定义未找到
            // - SecurityException：安全管理器阻止操作
            // 异常被静默处理，不影响其他类的加载
        }

        // 返回加载成功的类列表
        return classes;
    }
}
