package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassDetailVO;
import com.taobao.arthas.core.command.model.ClassLoaderModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.ClassSetVO;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ResultUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * 类加载器命令
 *
 * 这是Arthas中用于查看和管理ClassLoader的命令实现。
 * 该命令提供了丰富的功能来查看和分析JVM中的类加载器信息。
 *
 * 主要功能包括：
 * 1. 显示类加载器的继承树结构（-t参数）
 * 2. 按类加载器实例统计加载的类数量（-l参数）
 * 3. 查看特定类加载器的URL信息（-c参数）
 * 4. 查找资源文件（-r参数）
 * 5. 显示类加载器加载的所有类（-a参数）
 * 6. 使用类加载器加载指定的类（--load参数）
 * 7. 显示URL统计信息（-u参数）
 * 8. 显示URL与类的关系（--url-classes参数）
 */
@Name("classloader")
@Summary("Show classloader info")
@Description(Constants.EXAMPLE +
        "  classloader\n" +
        "  classloader -t\n" +
        "  classloader -l\n" +
        "  classloader -c 327a647b\n" +
        "  classloader -c 327a647b -r META-INF/MANIFEST.MF\n" +
        "  classloader -a\n" +
        "  classloader -a -c 327a647b\n" +
        "  classloader -c 659e0bfd --load demo.MathGame\n" +
        "  classloader -u      # url statistics\n" +
        "  classloader -c 659e0bfd --url-classes\n" +
        "  classloader -c 659e0bfd --url-classes -d\n" +
        "  classloader -c 659e0bfd --url-classes --jar spring-core --class org.springframework\n" +
        Constants.WIKI + Constants.WIKI_HOME + "classloader")
public class ClassLoaderCommand extends AnnotatedCommand {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ClassLoaderCommand.class);

    /**
     * 默认的URL类显示限制数量
     * 在详细模式下，每个URL最多显示的类数量
     */
    private static final int DEFAULT_URL_CLASSES_LIMIT = 100;

    /**
     * 未知代码源的标识字符串
     * 当无法确定类的来源时使用此字符串
     */
    private static final String UNKNOWN_CODE_SOURCE = "<unknown>";

    /**
     * 是否以树状结构显示类加载器
     * true表示显示类加载器的继承树结构
     */
    private boolean isTree = false;

    /**
     * 类加载器的哈希码
     * 用于指定要操作的特定类加载器
     */
    private String hashCode;

    /**
     * 类加载器的类名
     * 用于通过类名查找类加载器
     */
    private String classLoaderClass;

    /**
     * 是否显示所有已加载的类
     * true表示显示类加载器加载的所有类
     */
    private boolean all = false;

    /**
     * 要查找的资源名称
     * 使用类加载器查找指定的资源文件
     */
    private String resource;

    /**
     * 是否包含反射类加载器
     * true表示包含sun.reflect.DelegatingClassLoader等反射类加载器
     */
    private boolean includeReflectionClassLoader = true;

    /**
     * 是否按类加载器实例列出统计信息
     * true表示显示每个类加载器实例的统计信息
     */
    private boolean listClassLoader = false;

    /**
     * 是否显示URL统计信息
     * true表示显示类加载器的URL使用情况统计
     */
    private boolean urlStat = false;

    /**
     * 是否显示URL与类的关系
     * true表示显示每个URL加载了哪些类
     */
    private boolean urlClasses = false;

    /**
     * 是否显示URL类的详细信息
     * true表示在--url-classes模式下显示每个URL的类列表
     */
    private boolean urlClassesDetail = false;

    /**
     * 是否对jar和class过滤器使用正则表达式
     * true表示使用正则表达式匹配，false表示使用简单字符串包含匹配
     */
    private boolean urlClassesRegEx = false;

    /**
     * URL类的显示限制
     * 在详细模式下，每个URL最多显示的类数量
     */
    private int urlClassesLimit = DEFAULT_URL_CLASSES_LIMIT;

    /**
     * JAR文件过滤器
     * 用于过滤特定JAR包的URL
     */
    private String jarFilter;

    /**
     * 类过滤器
     * 用于过滤特定类名或包名的类
     */
    private String classFilter;

    /**
     * 要加载的类名
     * 使用指定的类加载器加载此类
     */
    private String loadClass = null;

    /**
     * 命令是否被中断
     * 使用volatile保证多线程可见性，用于支持Ctrl+C中断
     */
    private volatile boolean isInterrupted = false;

    /**
     * 设置是否以树状结构显示类加载器
     *
     * @param tree true表示显示树状结构，false表示显示列表
     */
    @Option(shortName = "t", longName = "tree", flag = true)
    @Description("Display ClassLoader tree")
    public void setTree(boolean tree) {
        isTree = tree;
    }

    /**
     * 设置类加载器的类名
     * 通过类名来查找特定的类加载器
     *
     * @param classLoaderClass 类加载器的类名
     */
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    /**
     * 设置类加载器的哈希码
     * 通过哈希码来指定要操作的特定类加载器
     *
     * @param hashCode 类加载器的十六进制哈希码字符串
     */
    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special ClassLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    /**
     * 设置是否显示所有已加载的类
     *
     * @param all true表示显示所有类，false表示不显示
     */
    @Option(shortName = "a", longName = "all", flag = true)
    @Description("Display all classes loaded by ClassLoader")
    public void setAll(boolean all) {
        this.all = all;
    }

    /**
     * 设置要查找的资源名称
     * 使用类加载器查找指定的资源文件
     *
     * @param resource 资源文件的路径
     */
    @Option(shortName = "r", longName = "resource")
    @Description("Use ClassLoader to find resources, won't work without -c specified")
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * 设置是否包含反射类加载器
     *
     * @param includeReflectionClassLoader true表示包含，false表示排除
     */
    @Option(shortName = "i", longName = "include-reflection-classloader", flag = true)
    @Description("Include sun.reflect.DelegatingClassLoader")
    public void setIncludeReflectionClassLoader(boolean includeReflectionClassLoader) {
        this.includeReflectionClassLoader = includeReflectionClassLoader;
    }

    /**
     * 设置是否按类加载器实例列出统计信息
     *
     * @param listClassLoader true表示列出统计信息，false表示不列出
     */
    @Option(shortName = "l", longName = "list-classloader", flag = true)
    @Description("Display statistics info by classloader instance")
    public void setListClassLoader(boolean listClassLoader) {
        this.listClassLoader = listClassLoader;
    }

    /**
     * 设置要加载的类名
     * 使用指定的类加载器加载此类
     *
     * @param className 要加载的类的全限定名
     */
    @Option(longName = "load")
    @Description("Use ClassLoader to load class, won't work without -c specified")
    public void setLoadClass(String className) {
        this.loadClass = className;
    }

    /**
     * 设置是否显示URL统计信息
     *
     * @param urlStat true表示显示URL统计，false表示不显示
     */
    @Option(shortName = "u", longName = "url-stat", flag = true)
    @Description("Display classloader url statistics")
    public void setUrlStat(boolean urlStat) {
        this.urlStat = urlStat;
    }

    /**
     * 设置是否显示URL与类的关系
     *
     * @param urlClasses true表示显示URL与类的关系，false表示不显示
     */
    @Option(longName = "url-classes", flag = true)
    @Description("Display relationship between jar(URL) and loaded classes in the specified ClassLoader")
    public void setUrlClasses(boolean urlClasses) {
        this.urlClasses = urlClasses;
    }

    /**
     * 设置是否显示URL类的详细信息
     *
     * @param urlClassesDetail true表示显示详细信息，false表示不显示
     */
    @Option(shortName = "d", longName = "details", flag = true)
    @Description("Display class list for each jar(URL), only works with --url-classes")
    public void setUrlClassesDetail(boolean urlClassesDetail) {
        this.urlClassesDetail = urlClassesDetail;
    }

    /**
     * 设置是否对过滤器使用正则表达式
     *
     * @param urlClassesRegEx true表示使用正则表达式，false表示使用简单字符串匹配
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match for --jar/--class, only works with --url-classes")
    public void setUrlClassesRegEx(boolean urlClassesRegEx) {
        this.urlClassesRegEx = urlClassesRegEx;
    }

    /**
     * 设置URL类的显示限制
     * 在详细模式下，限制每个URL显示的类数量
     *
     * @param urlClassesLimit 每个URL最多显示的类数量
     */
    @Option(shortName = "n", longName = "limit")
    @Description("Maximum number of classes to display per jar(URL) in details mode (100 by default), only works with --url-classes -d")
    public void setUrlClassesLimit(int urlClassesLimit) {
        this.urlClassesLimit = urlClassesLimit;
    }

    /**
     * 设置JAR文件过滤器
     * 用于过滤特定JAR包的URL
     *
     * @param jarFilter JAR文件的关键词或正则表达式
     */
    @Option(longName = "jar")
    @Description("Filter jar(URL) by keyword (or regex with -E), only works with --url-classes")
    public void setJarFilter(String jarFilter) {
        this.jarFilter = jarFilter;
    }

    /**
     * 设置类过滤器
     * 用于过滤特定类名或包名的类
     *
     * @param classFilter 类名或包名的关键词或正则表达式
     */
    @Option(longName = "class")
    @Description("Filter classes by keyword/package (or regex with -E), only works with --url-classes")
    public void setClassFilter(String classFilter) {
        // 规范化类名，将'/'替换为'.'
        this.classFilter = StringUtils.normalizeClassName(classFilter);
    }

    /**
     * 处理命令
     *
     * 这是命令的主入口方法，根据用户指定的参数执行相应的操作。
     * 该方法支持多种操作模式，包括显示类加载器树、统计信息、URL信息等。
     *
     * @param process 命令处理上下文，包含会话信息和结果输出
     */
    @Override
    public void process(CommandProcess process) {
        // 注册Ctrl+C中断处理器，支持用户中断命令执行
        process.interruptHandler(new ClassLoaderInterruptHandler(this));
        // 目标类加载器，用户指定的要操作的类加载器
        ClassLoader targetClassLoader = null;
        // 是否指定了类加载器（通过hashCode或classLoaderClass）
        boolean classLoaderSpecified = false;

        // 获取Java Instrumentation实例，用于访问JVM的类加载信息
        Instrumentation inst = process.session().getInstrumentation();

        // 处理URL统计信息请求
        if (urlStat) {
            // 获取所有类加载器的URL统计信息
            Map<ClassLoaderVO, ClassLoaderUrlStat> urlStats = this.urlStats(inst);
            // 创建结果模型并设置URL统计信息
            ClassLoaderModel model = new ClassLoaderModel();
            model.setUrlStats(urlStats);
            // 将结果添加到处理上下文
            process.appendResult(model);
            // 结束命令处理
            process.end();
            return;
        }

        // 验证参数组合：URL类相关选项必须与--url-classes一起使用
        if (!urlClasses && (urlClassesDetail || urlClassesRegEx || jarFilter != null || classFilter != null
                || urlClassesLimit != DEFAULT_URL_CLASSES_LIMIT)) {
            process.end(-1, "Options -d/-E/-n/--jar/--class only work with --url-classes.");
            return;
        }

        // 检查是否通过hashCode或classLoaderClass指定了类加载器
        if (hashCode != null || classLoaderClass != null) {
            classLoaderSpecified = true;
        }

        // 通过哈希码查找类加载器
        if (hashCode != null) {
            // 获取所有类加载器
            Set<ClassLoader> allClassLoader = getAllClassLoaders(inst);
            // 遍历查找匹配哈希码的类加载器
            for (ClassLoader cl : allClassLoader) {
                if (Integer.toHexString(cl.hashCode()).equals(hashCode)) {
                    targetClassLoader = cl;
                    break;
                }
            }
        } else if (classLoaderClass != null) {
            // 通过类名查找类加载器
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                // 找到唯一匹配的类加载器
                targetClassLoader = matchedClassLoaders.get(0);
            } else if (matchedClassLoaders.size() > 1) {
                // 找到多个匹配的类加载器，提示用户使用哈希码指定
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                ClassLoaderModel classloaderModel = new ClassLoaderModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(classloaderModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                // 没有找到匹配的类加载器
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        // 处理URL与类关系查询
        if (urlClasses) {
            // 验证是否指定了类加载器
            if (!classLoaderSpecified) {
                process.end(-1, "Please specify classloader with '-c <classloader hash>' or '--classLoaderClass <classloader class name>' for --url-classes.");
                return;
            }
            // 验证是否找到目标类加载器
            if (targetClassLoader == null) {
                process.end(-1, "Can not find classloader by hashcode: " + hashCode + ".");
                return;
            }
            // 处理URL类关系查询
            processUrlClasses(process, inst, targetClassLoader);
            return;
        }

        // 根据不同的参数组合执行相应的操作
        if (all) {
            // 显示所有已加载的类
            String hashCode = this.hashCode;
            // 如果没有指定hashCode但找到了目标类加载器，使用其哈希码
            if (StringUtils.isBlank(hashCode) && targetClassLoader != null) {
                hashCode = "" + Integer.toHexString(targetClassLoader.hashCode());
            }
            processAllClasses(process, inst, hashCode);
        } else if (classLoaderSpecified && resource != null) {
            // 查找资源文件
            processResources(process, inst, targetClassLoader);
        } else if (classLoaderSpecified && this.loadClass != null) {
            // 加载指定的类
            processLoadClass(process, inst, targetClassLoader);
        } else if (classLoaderSpecified) {
            // 显示指定类加载器的信息
            processClassLoader(process, inst, targetClassLoader);
        } else if (listClassLoader || isTree){
            // 列出所有类加载器或显示树状结构
            processClassLoaders(process, inst);
        } else {
            // 默认：显示类加载器统计信息
            processClassLoaderStats(process, inst);
        }
    }

    /**
     * 计算类加载器统计信息
     *
     * 该方法按类加载器类型统计加载的类数量和实例数量。
     * 例如：JVM中有100个GroovyClassLoader实例，总共加载了200个类。
     *
     * @param process 命令处理上下文
     * @param inst Java Instrumentation实例
     */
    private void processClassLoaderStats(CommandProcess process, Instrumentation inst) {
        // 创建行影响统计对象
        RowAffect affect = new RowAffect();
        // 获取所有类加载器的信息
        List<ClassLoaderInfo> classLoaderInfos = getAllClassLoaderInfo(inst);
        // 创建统计信息映射表，键为类加载器类型名，值为统计对象
        Map<String, ClassLoaderStat> classLoaderStats = new HashMap<String, ClassLoaderStat>();
        // 遍历所有类加载器信息，按类型汇总统计
        for (ClassLoaderInfo info: classLoaderInfos) {
            // 获取类加载器名称，null表示BootstrapClassLoader
            String name = info.classLoader == null ? "BootstrapClassLoader" : info.classLoader.getClass().getName();
            // 获取或创建该类型的统计对象
            ClassLoaderStat stat = classLoaderStats.get(name);
            if (null == stat) {
                stat = new ClassLoaderStat();
                classLoaderStats.put(name, stat);
            }
            // 累加已加载的类数量
            stat.addLoadedCount(info.loadedClassCount);
            // 增加实例数量
            stat.addNumberOfInstance(1);
        }

        // 按加载的类数量降序排序
        TreeMap<String, ClassLoaderStat> sorted =
                new TreeMap<String, ClassLoaderStat>(new ValueComparator(classLoaderStats));
        sorted.putAll(classLoaderStats);
        // 将排序后的统计信息添加到结果中
        process.appendResult(new ClassLoaderModel().setClassLoaderStats(sorted));

        // 记录影响的行数
        affect.rCnt(sorted.keySet().size());
        process.appendResult(new RowAffectModel(affect));
        // 结束命令处理
        process.end();
    }

    /**
     * 处理类加载器列表显示
     *
     * 该方法列出所有的类加载器，可以选择是否包含反射类加载器，
     * 也可以选择以树状结构显示类加载器的继承关系。
     *
     * @param process 命令处理上下文
     * @param inst Java Instrumentation实例
     */
    private void processClassLoaders(CommandProcess process, Instrumentation inst) {
        // 创建行影响统计对象
        RowAffect affect = new RowAffect();
        // 根据是否包含反射类加载器获取类加载器信息
        List<ClassLoaderInfo> classLoaderInfos = includeReflectionClassLoader ? getAllClassLoaderInfo(inst) :
                getAllClassLoaderInfo(inst, new SunReflectionClassLoaderFilter());

        // 创建类加载器VO列表
        List<ClassLoaderVO> classLoaderVOs = new ArrayList<ClassLoaderVO>(classLoaderInfos.size());
        // 遍历类加载器信息，创建VO对象
        for (ClassLoaderInfo classLoaderInfo : classLoaderInfos) {
            // 创建类加载器值对象
            ClassLoaderVO classLoaderVO = ClassUtils.createClassLoaderVO(classLoaderInfo.classLoader);
            // 设置已加载的类数量
            classLoaderVO.setLoadedCount(classLoaderInfo.loadedClassCount());
            classLoaderVOs.add(classLoaderVO);
        }
        // 如果需要以树状结构显示
        if (isTree){
            // 构建类加载器树
            classLoaderVOs = processClassLoaderTree(classLoaderVOs);
        }
        // 将结果添加到处理上下文
        process.appendResult(new ClassLoaderModel().setClassLoaders(classLoaderVOs).setTree(isTree));

        // 记录影响的行数
        affect.rCnt(classLoaderInfos.size());
        process.appendResult(new RowAffectModel(affect));
        // 结束命令处理
        process.end();
    }

    /**
     * 处理类加载器的URL信息显示
     *
     * 根据指定的类加载器打印其URL列表。
     * 如果类加载器是URLClassLoader，则显示其所有的URL路径。
     *
     * @param process 命令处理上下文
     * @param inst Java Instrumentation实例
     * @param targetClassLoader 目标类加载器
     */
    // 根据 ClassLoader 来打印URLClassLoader的urls
    private void processClassLoader(CommandProcess process, Instrumentation inst, ClassLoader targetClassLoader) {
        // 创建行影响统计对象
        RowAffect affect = new RowAffect();
        // 如果目标类加载器不为空
        if (targetClassLoader != null) {
            // 尝试获取类加载器的URL列表
            URL[] classLoaderUrls = ClassLoaderUtils.getUrls(targetClassLoader);
            if (classLoaderUrls != null) {
                // 成功获取URL列表
                affect.rCnt(classLoaderUrls.length);
                if (classLoaderUrls.length == 0) {
                    // URL列表为空
                    process.appendResult(new MessageModel("urls is empty."));
                } else {
                    // 将URL列表转换为字符串列表并添加到结果中
                    process.appendResult(new ClassLoaderModel().setUrls(StringUtils.toStringList(classLoaderUrls)));
                    affect.rCnt(classLoaderUrls.length);
                }
            } else {
                // 该类加载器不是URLClassLoader
                process.appendResult(new MessageModel("not a URLClassLoader."));
            }
        }
        // 添加行影响统计到结果中
        process.appendResult(new RowAffectModel(affect));
        // 结束命令处理
        process.end();
    }

    /**
     * 处理资源查找
     *
     * 使用指定的类加载器查找资源文件。
     * 资源文件可以是配置文件、属性文件等任意类型。
     *
     * @param process 命令处理上下文
     * @param inst Java Instrumentation实例
     * @param targetClassLoader 目标类加载器
     */
    // 使用ClassLoader去getResources
    private void processResources(CommandProcess process, Instrumentation inst, ClassLoader targetClassLoader) {
        // 创建行影响统计对象
        RowAffect affect = new RowAffect();
        // 资源行数计数器
        int rowCount = 0;
        // 资源URL列表
        List<String> resources = new ArrayList<String>();
        // 如果目标类加载器不为空
        if (targetClassLoader != null) {
            try {
                // 使用类加载器查找所有匹配的资源
                Enumeration<URL> urls = targetClassLoader.getResources(resource);
                // 遍历所有找到的资源URL
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    // 将URL转换为字符串并添加到列表
                    resources.add(url.toString());
                    rowCount++;
                }
            } catch (Throwable e) {
                // 记录查找资源失败的警告
                logger.warn("get resource failed, resource: {}", resource, e);
            }
        }
        // 记录影响的行数
        affect.rCnt(rowCount);

        // 将资源列表添加到结果中
        process.appendResult(new ClassLoaderModel().setResources(resources));
        process.appendResult(new RowAffectModel(affect));
        // 结束命令处理
        process.end();
    }

    /**
     * 处理类加载操作
     *
     * 使用指定的类加载器加载指定的类。
     * 这对于测试类是否可以被特定类加载器加载非常有用。
     *
     * @param process 命令处理上下文
     * @param inst Java Instrumentation实例
     * @param targetClassLoader 目标类加载器
     */
    // Use ClassLoader to loadClass
    private void processLoadClass(CommandProcess process, Instrumentation inst, ClassLoader targetClassLoader) {
        // 如果目标类加载器不为空
        if (targetClassLoader != null) {
            try {
                // 使用类加载器加载指定的类
                Class<?> clazz = targetClassLoader.loadClass(this.loadClass);
                // 添加加载成功的消息
                process.appendResult(new MessageModel("load class success."));
                // 创建类的详细信息对象
                ClassDetailVO classInfo = ClassUtils.createClassInfo(clazz, false, null);
                // 将类信息添加到结果中
                process.appendResult(new ClassLoaderModel().setLoadClass(classInfo));

            } catch (Throwable e) {
                // 记录加载类失败的警告
                logger.warn("load class error, class: {}", this.loadClass, e);
                // 返回错误信息并结束命令处理
                process.end(-1, "load class error, class: "+this.loadClass+", error: "+e.toString());
                return;
            }
        }
        // 结束命令处理
        process.end();
    }

    /**
     * 处理所有已加载类的显示
     *
     * 获取并显示指定类加载器（或所有类加载器）加载的所有类。
     * 类按类加载器分组，并按类名排序。
     *
     * @param process 命令处理上下文
     * @param inst Java Instrumentation实例
     * @param hashCode 类加载器的十六进制哈希码，如果为null则显示所有类加载器的类
     */
    private void processAllClasses(CommandProcess process, Instrumentation inst,String hashCode) {
        // 创建行影响统计对象
        RowAffect affect = new RowAffect();
        // 获取所有类信息
        getAllClasses(hashCode, inst, affect, process);
        // 检查是否被中断
        if (checkInterrupted(process)) {
            return;
        }
        // 添加行影响统计到结果中
        process.appendResult(new RowAffectModel(affect));
        // 结束命令处理
        process.end();
    }

    /**
     * 获取所有已加载的类并按类加载器分组
     *
     * 该方法获取JVM中所有已加载的类，并按照类加载器进行分组。
     * 对于BootstrapClassLoader加载的类单独处理。
     * 如果指定了hashCode，则只显示该类加载器加载的类。
     *
     * 支持中断操作，在处理大量类时可以通过Ctrl+C中断。
     *
     * @param hashCode 类加载器的十六进制哈希码，如果为null则显示所有类加载器的类
     * @param inst Java Instrumentation实例
     * @param affect 行影响统计对象，用于记录处理的类数量
     * @param process 命令处理上下文
     */
    /**
     * 获取到所有的class, 还有它们的classloader，按classloader归类好，统一输出每个classloader里有哪些class
     * <p>
     * 当hashCode是null，则把所有的classloader的都打印
     *
     */
    @SuppressWarnings("rawtypes")
    private void getAllClasses(String hashCode, Instrumentation inst, RowAffect affect, CommandProcess process) {
        // 将十六进制哈希码转换为整数
        int hashCodeInt = -1;
        if (hashCode != null) {
            hashCodeInt = Integer.valueOf(hashCode, 16);
        }

        // 创建BootstrapClassLoader加载的类的有序集合
        SortedSet<Class<?>> bootstrapClassSet = new TreeSet<Class<?>>(new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                // 按类名字母顺序排序
                return o1.getName().compareTo(o2.getName());
            }
        });

        // 获取JVM中所有已加载的类
        Class[] allLoadedClasses = inst.getAllLoadedClasses();
        // 创建类加载器到类集合的映射表
        Map<ClassLoader, SortedSet<Class<?>>> classLoaderClassMap = new HashMap<ClassLoader, SortedSet<Class<?>>>();
        // 遍历所有已加载的类
        for (Class clazz : allLoadedClasses) {
            // 获取加载该类的类加载器
            ClassLoader classLoader = clazz.getClassLoader();
            // 处理BootstrapClassLoader加载的类（classLoader为null）
            if (classLoader == null) {
                // 如果没有指定hashCode，则将Bootstrap类添加到集合
                if (hashCode == null) {
                    bootstrapClassSet.add(clazz);
                }
                continue;
            }

            // 如果指定了hashCode，只处理匹配的类加载器
            if (hashCode != null && classLoader.hashCode() != hashCodeInt) {
                continue;
            }

            // 获取或创建该类加载器的类集合
            SortedSet<Class<?>> classSet = classLoaderClassMap.get(classLoader);
            if (classSet == null) {
                // 创建新的有序类集合，按类名排序
                classSet = new TreeSet<Class<?>>(new Comparator<Class<?>>() {
                    @Override
                    public int compare(Class<?> o1, Class<?> o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                classLoaderClassMap.put(classLoader, classSet);
            }
            // 将类添加到对应的类加载器集合中
            classSet.add(clazz);
        }

        // 输出BootstrapClassLoader加载的类
        int pageSize = 256;
        processClassSet(process, ClassUtils.createClassLoaderVO(null), bootstrapClassSet, pageSize, affect);

        // 输出其他类加载器加载的类
        for (Entry<ClassLoader, SortedSet<Class<?>>> entry : classLoaderClassMap.entrySet()) {
            // 检查是否被中断
            if (checkInterrupted(process)) {
                return;
            }
            // 获取类加载器和对应的类集合
            ClassLoader classLoader = entry.getKey();
            SortedSet<Class<?>> classSet = entry.getValue();
            // 处理并输出类集合
            processClassSet(process, ClassUtils.createClassLoaderVO(classLoader), classSet, pageSize, affect);
        }
    }

    /**
     * 处理类集合的分批输出
     *
     * 该方法将类集合分批输出，每批包含指定数量的类。
     * 支持中断操作，在处理大量类时可以通过Ctrl+C中断。
     *
     * @param process 命令处理上下文
     * @param classLoaderVO 类加载器值对象
     * @param classes 要处理的类集合
     * @param pageSize 每批输出的类数量
     * @param affect 行影响统计对象
     */
    private void processClassSet(final CommandProcess process, final ClassLoaderVO classLoaderVO, Collection<Class<?>> classes, int pageSize, final RowAffect affect) {
        // 分批输出classNames, Ctrl+C可以中断执行
        ResultUtils.processClassNames(classes, pageSize, new ResultUtils.PaginationHandler<List<String>>() {
            @Override
            public boolean handle(List<String> classNames, int segment) {
                // 将类集合添加到结果中
                process.appendResult(new ClassLoaderModel().setClassSet(new ClassSetVO(classLoaderVO, classNames, segment)));
                // 更新行影响统计
                affect.rCnt(classNames.size());
                // 返回是否继续处理，如果被中断则停止
                return !checkInterrupted(process);
            }
        });
    }

    /**
     * 检查命令是否被中断
     *
     * 该方法检查命令处理是否应该中断。
     * 中断可能由以下原因引起：
     * 1. 用户按Ctrl+C
     * 2. 进程不再运行
     *
     * @param process 命令处理上下文
     * @return true表示应该中断，false表示可以继续
     */
    private boolean checkInterrupted(CommandProcess process) {
        // 检查进程是否还在运行
        if (!process.isRunning()) {
            return true;
        }
        // 检查中断标志是否被设置
        if(isInterrupted){
            // 结束命令处理并返回中断消息
            process.end(-1, "Processing has been interrupted");
            return true;
        } else {
            return false;
        }
    }

    /**
     * 处理URL与类的关系查询
     *
     * 该方法分析指定类加载器中所有已加载的类，按URL分组统计，
     * 并可以应用过滤器和限制条件。
     *
     * 主要功能：
     * 1. 统计每个URL加载的类数量
     * 2. 支持JAR包和类名的过滤
     * 3. 支持正则表达式和简单字符串匹配
     * 4. 在详细模式下显示每个URL的类列表
     * 5. 支持中断操作
     *
     * @param process 命令处理上下文
     * @param inst Java Instrumentation实例
     * @param targetClassLoader 目标类加载器
     */
    private void processUrlClasses(CommandProcess process, Instrumentation inst, ClassLoader targetClassLoader) {
        // 验证参数：-n/--limit选项必须与-d一起使用
        if (!urlClassesDetail && urlClassesLimit != DEFAULT_URL_CLASSES_LIMIT) {
            process.end(-1, "Option -n/--limit only works with --url-classes -d.");
            return;
        }
        // 验证参数：限制值必须大于0
        if (urlClassesDetail && urlClassesLimit <= 0) {
            process.end(-1, "Option -n/--limit must be greater than 0.");
            return;
        }

        // 编译正则表达式模式（如果启用）
        Pattern jarPattern = null;
        Pattern classPattern = null;
        if (urlClassesRegEx) {
            try {
                // 编译JAR过滤器正则表达式
                if (jarFilter != null) {
                    jarPattern = Pattern.compile(jarFilter);
                }
                // 编译类过滤器正则表达式
                if (classFilter != null) {
                    classPattern = Pattern.compile(classFilter);
                }
            } catch (Throwable e) {
                // 正则表达式编译错误
                process.end(-1, "Regex compile error: " + e.getMessage());
                return;
            }
        }

        // 创建URL统计信息映射表
        Map<String, UrlClassStatBuilder> statsMap = new HashMap<String, UrlClassStatBuilder>();
        // 获取所有已加载的类
        Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();
        // 遍历所有已加载的类
        for (int i = 0; i < allLoadedClasses.length; i++) {
            // 每处理16384个类检查一次是否中断
            if ((i & 0x3FFF) == 0 && checkInterrupted(process)) {
                return;
            }
            Class<?> clazz = allLoadedClasses[i];
            if (clazz == null) {
                continue;
            }
            // 只处理目标类加载器加载的类
            if (clazz.getClassLoader() != targetClassLoader) {
                continue;
            }

            // 获取类的代码源位置（URL）
            String url = codeSourceLocation(clazz);
            // 检查是否匹配JAR过滤器
            if (!matchJarFilter(url, jarPattern)) {
                continue;
            }

            // 获取或创建该URL的统计构建器
            UrlClassStatBuilder builder = statsMap.get(url);
            if (builder == null) {
                builder = new UrlClassStatBuilder(url, classFilter != null, urlClassesDetail ? urlClassesLimit : 0);
                statsMap.put(url, builder);
            }
            // 增加已加载类数量
            builder.increaseLoadedCount();

            // 如果设置了类过滤器
            if (classFilter != null) {
                // 检查是否匹配类过滤器
                if (matchClassFilter(clazz.getName(), classPattern)) {
                    // 增加匹配类数量并尝试添加类名
                    builder.increaseMatchedCount();
                    builder.tryAddClass(clazz.getName());
                }
            } else {
                // 没有类过滤器，直接添加类名
                builder.tryAddClass(clazz.getName());
            }
        }

        // 检查是否有类过滤器
        boolean hasClassFilter = classFilter != null;
        // 构建最终的统计信息列表
        List<UrlClassStat> stats = new ArrayList<UrlClassStat>(statsMap.size());
        for (UrlClassStatBuilder builder : statsMap.values()) {
            // 如果有类过滤器但没有匹配的类，跳过
            if (hasClassFilter && builder.getMatchedClassCount() == 0) {
                continue;
            }
            // 构建统计对象并添加到列表
            stats.add(builder.build());
        }

        // 对统计结果进行排序
        Collections.sort(stats, new Comparator<UrlClassStat>() {
            @Override
            public int compare(UrlClassStat o1, UrlClassStat o2) {
                // 根据是否有类过滤器选择排序字段
                int c1 = hasClassFilter ? safeInt(o1.getMatchedClassCount()) : o1.getLoadedClassCount();
                int c2 = hasClassFilter ? safeInt(o2.getMatchedClassCount()) : o2.getLoadedClassCount();
                // 按类数量降序排序
                int diff = c2 - c1;
                if (diff != 0) {
                    return diff;
                }
                // 类数量相同时按URL字母顺序排序
                return o1.getUrl().compareTo(o2.getUrl());
            }
        });

        // 创建行影响统计对象
        RowAffect affect = new RowAffect();
        affect.rCnt(stats.size());
        // 创建结果模型并设置相关属性
        ClassLoaderModel model = new ClassLoaderModel()
                .setClassLoader(ClassUtils.createClassLoaderVO(targetClassLoader))
                .setUrlClassStats(stats)
                .setUrlClassStatsDetail(urlClassesDetail);
        // 添加结果到处理上下文
        process.appendResult(model);
        process.appendResult(new RowAffectModel(affect));
        // 结束命令处理
        process.end();
    }

    /**
     * 安全地获取整数值
     *
     * 该方法处理可能为null的Integer对象，返回0而非抛出异常。
     *
     * @param v Integer对象，可能为null
     * @return 如果v为null返回0，否则返回其整数值
     */
    private static int safeInt(Integer v) {
        return v == null ? 0 : v.intValue();
    }

    /**
     * 检查URL是否匹配JAR过滤器
     *
     * 该方法根据JAR过滤器设置（简单字符串或正则表达式）
     * 检查给定的URL是否应该被包含在结果中。
     *
     * @param url 要检查的URL字符串
     * @param jarPattern 编译后的正则表达式模式，如果未启用正则则为null
     * @return true表示URL匹配过滤器，false表示不匹配
     */
    private boolean matchJarFilter(String url, Pattern jarPattern) {
        // 如果没有设置JAR过滤器，则匹配所有URL
        if (jarFilter == null) {
            return true;
        }
        // 从URL中猜测JAR文件名
        String jarName = guessJarName(url);
        // 如果启用了正则表达式模式
        if (urlClassesRegEx) {
            // 使用正则表达式匹配URL或JAR名
            return jarPattern != null && (jarPattern.matcher(url).find() || jarPattern.matcher(jarName).find());
        }
        // 使用简单字符串包含匹配（忽略大小写）
        return containsIgnoreCase(url, jarFilter) || containsIgnoreCase(jarName, jarFilter);
    }

    /**
     * 检查类名是否匹配类过滤器
     *
     * 该方法根据类过滤器设置（简单字符串或正则表达式）
     * 检查给定的类名是否应该被包含在结果中。
     *
     * @param className 要检查的类名
     * @param classPattern 编译后的正则表达式模式，如果未启用正则则为null
     * @return true表示类名匹配过滤器，false表示不匹配
     */
    private boolean matchClassFilter(String className, Pattern classPattern) {
        // 如果没有设置类过滤器，则匹配所有类
        if (classFilter == null) {
            return true;
        }
        // 如果启用了正则表达式模式
        if (urlClassesRegEx) {
            // 使用正则表达式匹配类名
            return classPattern != null && classPattern.matcher(className).find();
        }
        // 使用简单字符串包含匹配（忽略大小写）
        return containsIgnoreCase(className, classFilter);
    }

    /**
     * 忽略大小写的字符串包含检查
     *
     * 该方法检查text字符串中是否包含keyword字符串，
     * 匹配时忽略大小写。
     *
     * @param text 要搜索的文本
     * @param keyword 要查找的关键词
     * @return true表示text包含keyword（忽略大小写），false表示不包含
     */
    static boolean containsIgnoreCase(String text, String keyword) {
        // 处理null值
        if (text == null || keyword == null) {
            return false;
        }
        // 转换为小写后进行包含检查
        return text.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * 获取类的代码源位置
     *
     * 该方法尝试获取类的代码源位置（即类文件所在的URL）。
     * 如果无法确定位置，返回未知标识符。
     *
     * @param clazz 要检查的类对象
     * @return 类的代码源位置URL，或未知标识符
     */
    private static String codeSourceLocation(Class<?> clazz) {
        try {
            // 获取类的保护域
            ProtectionDomain protectionDomain = clazz.getProtectionDomain();
            if (protectionDomain == null) {
                return UNKNOWN_CODE_SOURCE;
            }
            // 获取代码源
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource == null) {
                return UNKNOWN_CODE_SOURCE;
            }
            // 获取代码源的位置
            URL location = codeSource.getLocation();
            if (location == null) {
                return UNKNOWN_CODE_SOURCE;
            }
            // 返回位置字符串
            return location.toString();
        } catch (Throwable t) {
            // 发生任何异常都返回未知标识符
            return UNKNOWN_CODE_SOURCE;
        }
    }

    /**
     * 从URL中猜测JAR文件名
     *
     * 该方法从URL字符串中提取JAR文件名。
     * 处理各种URL格式，包括：
     * - 普通文件路径
     * - JAR文件URL（包含!分隔符）
     * - 带路径分隔符的URL
     *
     * @param url URL字符串
     * @return 提取的JAR文件名，如果url为null则返回空字符串
     */
    static String guessJarName(String url) {
        // 处理null值
        if (url == null) {
            return com.taobao.arthas.core.util.Constants.EMPTY_STRING;
        }
        String s = url;
        // 处理JAR文件URL（如：jar:file:/path/to/jar.jar!/package/Class.class）
        int bangIndex = s.lastIndexOf('!');
        if (bangIndex >= 0) {
            // 去掉!之后的部分
            s = s.substring(0, bangIndex);
        }
        // 去掉末尾的斜杠
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        // 找到最后一个路径分隔符
        int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        if (slash >= 0 && slash < s.length() - 1) {
            // 提取最后一个路径分隔符之后的部分作为文件名
            s = s.substring(slash + 1);
        }
        return s;
    }

    /**
     * 计算类加载器的URL统计信息
     *
     * 该方法分析所有已加载的类，统计每个类加载器使用的URL和未使用的URL。
     * 对于URLClassLoader，可以显示哪些JAR包实际被使用，哪些没有被使用。
     *
     * @param inst Java Instrumentation实例
     * @return 类加载器到URL统计信息的映射表
     */
    private Map<ClassLoaderVO, ClassLoaderUrlStat> urlStats(Instrumentation inst) {
        // 创建结果映射表
        Map<ClassLoaderVO, ClassLoaderUrlStat> urlStats = new HashMap<ClassLoaderVO, ClassLoaderUrlStat>();
        // 创建已使用URL的映射表，用于临时统计
        Map<ClassLoader, Set<String>> usedUrlsMap = new HashMap<ClassLoader, Set<String>>();
        // 遍历所有已加载的类
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            // 获取加载该类的类加载器
            ClassLoader classLoader = clazz.getClassLoader();
            // 只处理非Bootstrap类加载器
            if (classLoader != null) {
                // 获取类的保护域
                ProtectionDomain protectionDomain = clazz.getProtectionDomain();
                CodeSource codeSource = protectionDomain.getCodeSource();
                if (codeSource != null) {
                    // 获取代码源的位置
                    URL location = codeSource.getLocation();
                    if (location != null) {
                        // 获取或创建该类加载器的已使用URL集合
                        Set<String> urls = usedUrlsMap.get(classLoader);
                        if (urls == null) {
                            urls = new HashSet<String>();
                            usedUrlsMap.put(classLoader, urls);
                        }
                        // 将URL添加到已使用集合
                        urls.add(location.toString());
                    }
                }
            }
        }
        // 遍历已使用URL映射表，计算未使用的URL
        for (Entry<ClassLoader, Set<String>> entry : usedUrlsMap.entrySet()) {
            ClassLoader loader = entry.getKey();
            Set<String> usedUrls = entry.getValue();
            // 获取类加载器的所有URL
            URL[] allUrls = ClassLoaderUtils.getUrls(loader);
            List<String> unusedUrls = new ArrayList<String>();
            if (allUrls != null) {
                // 遍历所有URL，找出未使用的
                for (URL url : allUrls) {
                    String urlStr = url.toString();
                    if (!usedUrls.contains(urlStr)) {
                        // 该URL没有被任何已加载的类使用
                        unusedUrls.add(urlStr);
                    }
                }
            }

            // 创建URL统计对象并添加到结果中
            urlStats.put(ClassUtils.createClassLoaderVO(loader), new ClassLoaderUrlStat(usedUrls, unusedUrls));
        }
        return urlStats;
    }

    /**
     * 将类加载器列表转换为树状结构
     *
     * 该方法将扁平的类加载器列表转换为树状结构，
     * 反映类加载器的父子继承关系。
     *
     * @param classLoaders 类加载器VO列表
     * @return 根类加载器列表（已经构建好树状结构）
     */
    // 以树状列出ClassLoader的继承结构
    private static List<ClassLoaderVO> processClassLoaderTree(List<ClassLoaderVO> classLoaders) {
        // 创建根类加载器列表
        List<ClassLoaderVO> rootClassLoaders = new ArrayList<>();
        // 创建子类加载器映射表，键为父类加载器名称，值为子类加载器列表
        Map<String, List<ClassLoaderVO>> childMap = new HashMap<>();

        // 分离根节点和非根节点，并构建父子关系映射
        for (ClassLoaderVO classLoaderVO : classLoaders) {
            if (classLoaderVO.getParent() == null) {
                // 没有父类加载器，是根节点
                rootClassLoaders.add(classLoaderVO);
            } else {
                // 有父类加载器，添加到父类加载器的子列表中
                childMap.computeIfAbsent(classLoaderVO.getParent(), k -> new ArrayList<>()).add(classLoaderVO);
            }
        }

        // 构建树
        for (ClassLoaderVO root : rootClassLoaders) {
            // 递归构建每个根节点的子树
            buildTree(root, childMap);
        }

        return rootClassLoaders;
    }

    /**
     * 递归构建类加载器树
     *
     * 该方法递归地为给定的父类加载器添加所有子类加载器，
     * 构建完整的树状结构。
     *
     * @param parent 父类加载器VO
     * @param childMap 子类加载器映射表
     */
    private static void buildTree(ClassLoaderVO parent, Map<String, List<ClassLoaderVO>> childMap) {
        // 获取当前节点的子类加载器列表
        List<ClassLoaderVO> children = childMap.get(parent.getName());
        if (children != null) {
            // 遍历所有子类加载器
            for (ClassLoaderVO child : children) {
                // 将子类加载器添加到父节点的子列表中
                parent.addChild(child);
                // 递归构建子树
                buildTree(child, childMap);
            }
        }
    }


    /**
     * 获取所有类加载器
     *
     * 该方法遍历所有已加载的类，提取出所有的类加载器。
     * 可以应用过滤器来排除特定类型的类加载器。
     *
     * @param inst Java Instrumentation实例
     * @param filters 可选的过滤器数组，用于过滤类加载器
     * @return 所有类加载器的集合
     */
    private static Set<ClassLoader> getAllClassLoaders(Instrumentation inst, Filter... filters) {
        // 创建类加载器集合，使用Set去重
        Set<ClassLoader> classLoaderSet = new HashSet<ClassLoader>();

        // 遍历所有已加载的类
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            // 获取加载该类的类加载器
            ClassLoader classLoader = clazz.getClassLoader();
            // 只处理非Bootstrap类加载器
            if (classLoader != null) {
                // 检查是否应该包含此加载器
                if (shouldInclude(classLoader, filters)) {
                    classLoaderSet.add(classLoader);
                }
            }
        }
        return classLoaderSet;
    }

    /**
     * 获取所有类加载器的详细信息
     *
     * 该方法收集所有类加载器的详细信息，包括：
     * - 每个类加载器加载的类数量
     * - 类加载器的父子关系
     * - Bootstrap类加载器加载的类数量
     *
     * 返回的列表经过排序，用户自定义的类加载器排在前面，
     * sun.开头的类加载器排在后面。
     *
     * @param inst Java Instrumentation实例
     * @param filters 可选的过滤器数组，用于过滤类加载器
     * @return 类加载器信息列表
     */
    private static List<ClassLoaderInfo> getAllClassLoaderInfo(Instrumentation inst, Filter... filters) {
        // 创建Bootstrap类加载器信息对象
        // 这里认为class.getClassLoader()返回是null的是由BootstrapClassLoader加载的，特殊处理
        ClassLoaderInfo bootstrapInfo = new ClassLoaderInfo(null);

        // 创建类加载器到信息对象的映射表
        Map<ClassLoader, ClassLoaderInfo> loaderInfos = new HashMap<ClassLoader, ClassLoaderInfo>();

        // 遍历所有已加载的类
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            // 获取加载该类的类加载器
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader == null) {
                // Bootstrap类加载器加载的类
                bootstrapInfo.increase();
            } else {
                // 检查是否应该包含此加载器
                if (shouldInclude(classLoader, filters)) {
                    // 获取或创建类加载器信息对象
                    ClassLoaderInfo loaderInfo = loaderInfos.get(classLoader);
                    if (loaderInfo == null) {
                        // 首次遇到此加载器，创建信息对象
                        loaderInfo = new ClassLoaderInfo(classLoader);
                        loaderInfos.put(classLoader, loaderInfo);
                        // 同时创建所有祖先类加载器的信息对象
                        ClassLoader parent = classLoader.getParent();
                        while (parent != null) {
                            ClassLoaderInfo parentLoaderInfo = loaderInfos.get(parent);
                            if (parentLoaderInfo == null) {
                                parentLoaderInfo = new ClassLoaderInfo(parent);
                                loaderInfos.put(parent, parentLoaderInfo);
                            }
                            parent = parent.getParent();
                        }
                    }
                    // 增加此类加载器加载的类数量
                    loaderInfo.increase();
                }
            }
        }

        // 排序时，把用户自己定的ClassLoader排在最前面，以sun.
        // 开头的放后面，因为sun.reflect.DelegatingClassLoader的实例太多
        // 创建sun.开头的类加载器列表
        List<ClassLoaderInfo> sunClassLoaderList = new ArrayList<ClassLoaderInfo>();

        // 创建其他类加载器列表
        List<ClassLoaderInfo> otherClassLoaderList = new ArrayList<ClassLoaderInfo>();

        // 根据类加载器类型分类
        for (Entry<ClassLoader, ClassLoaderInfo> entry : loaderInfos.entrySet()) {
            ClassLoader classLoader = entry.getKey();
            if (classLoader.getClass().getName().startsWith("sun.")) {
                // sun.开头的类加载器
                sunClassLoaderList.add(entry.getValue());
            } else {
                // 其他类加载器
                otherClassLoaderList.add(entry.getValue());
            }
        }

        // 对两个列表分别排序
        Collections.sort(sunClassLoaderList);
        Collections.sort(otherClassLoaderList);

        // 合并结果：Bootstrap -> 其他 -> sun.
        List<ClassLoaderInfo> result = new ArrayList<ClassLoaderInfo>();
        result.add(bootstrapInfo);
        result.addAll(otherClassLoaderList);
        result.addAll(sunClassLoaderList);
        return result;
    }

    /**
     * 检查类加载器是否应该被包含
     *
     * 该方法使用所有过滤器检查类加载器，
     * 只有通过所有过滤器的类加载器才会被包含。
     *
     * @param classLoader 要检查的类加载器
     * @param filters 过滤器数组，可能为null
     * @return true表示应该包含，false表示应该排除
     */
    private static boolean shouldInclude(ClassLoader classLoader, Filter... filters) {
        // 如果没有过滤器，包含所有类加载器
        if (filters == null) {
            return true;
        }

        // 检查所有过滤器，必须全部通过
        for (Filter filter : filters) {
            if (!filter.accept(classLoader)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 类加载器信息
     *
     * 该内部类用于存储类加载器的详细信息，
     * 包括类加载器本身和它加载的类数量。
     * 实现了Comparable接口以支持排序。
     */
    private static class ClassLoaderInfo implements Comparable<ClassLoaderInfo> {
        /**
         * 类加载器对象，null表示Bootstrap类加载器
         */
        private ClassLoader classLoader;

        /**
         * 该类加载器加载的类数量
         */
        private int loadedClassCount = 0;

        /**
         * 构造函数
         *
         * @param classLoader 类加载器对象，null表示Bootstrap类加载器
         */
        ClassLoaderInfo(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        /**
         * 获取类加载器名称
         *
         * @return 类加载器的字符串表示，Bootstrap类加载器返回"BootstrapClassLoader"
         */
        public String getName() {
            if (classLoader != null) {
                return classLoader.toString();
            }
            return "BootstrapClassLoader";
        }

        /**
         * 获取类加载器的哈希码字符串
         *
         * @return 十六进制哈希码字符串，Bootstrap类加载器返回"null"
         */
        String hashCodeStr() {
            if (classLoader != null) {
                return "" + Integer.toHexString(classLoader.hashCode());
            }
            return "null";
        }

        /**
         * 增加已加载类数量
         */
        void increase() {
            loadedClassCount++;
        }

        /**
         * 获取已加载类数量
         *
         * @return 已加载的类数量
         */
        int loadedClassCount() {
            return loadedClassCount;
        }

        /**
         * 获取父类加载器
         *
         * @return 父类加载器对象，如果没有或为Bootstrap则返回null
         */
        ClassLoader parent() {
            return classLoader == null ? null : classLoader.getParent();
        }

        /**
         * 获取父类加载器的字符串表示
         *
         * @return 父类加载器的字符串表示，如果没有则返回"null"
         */
        String parentStr() {
            if (classLoader == null) {
                return "null";
            }
            ClassLoader parent = classLoader.getParent();
            if (parent == null) {
                return "null";
            }
            return parent.toString();
        }

        /**
         * 比较两个类加载器信息
         *
         * 按类加载器的类名排序，用于列表排序显示。
         *
         * @param other 要比较的另一个类加载器信息
         * @return 比较结果
         */
        @Override
        public int compareTo(ClassLoaderInfo other) {
            if (other == null) {
                return -1;
            }
            if (other.classLoader == null) {
                return -1;
            }
            if (this.classLoader == null) {
                return -1;
            }

            // 按类名字母顺序排序
            return this.classLoader.getClass().getName().compareTo(other.classLoader.getClass().getName());
        }

    }

    /**
     * 类加载器过滤器接口
     *
     * 该接口用于过滤类加载器，决定哪些类加载器应该被包含在结果中。
     */
    private interface Filter {
        /**
         * 检查是否接受该类加载器
         *
         * @param classLoader 要检查的类加载器
         * @return true表示接受，false表示拒绝
         */
        boolean accept(ClassLoader classLoader);
    }

    /**
     * Sun反射类加载器过滤器
     *
     * 该过滤器排除Sun的反射类加载器，
     * 如sun.reflect.DelegatingClassLoader等。
     * 这些类加载器实例数量通常非常多，对用户价值较小。
     */
    private static class SunReflectionClassLoaderFilter implements Filter {
        /**
         * 需要排除的反射类加载器类名列表
         */
        private static final List<String> REFLECTION_CLASSLOADERS = Arrays.asList("sun.reflect.DelegatingClassLoader",
                "jdk.internal.reflect.DelegatingClassLoader");

        /**
         * 检查是否接受该类加载器
         *
         * 拒绝所有在排除列表中的反射类加载器。
         *
         * @param classLoader 要检查的类加载器
         * @return true表示接受（非反射类加载器），false表示拒绝（反射类加载器）
         */
        @Override
        public boolean accept(ClassLoader classLoader) {
            // 检查类加载器是否在排除列表中
            return !REFLECTION_CLASSLOADERS.contains(classLoader.getClass().getName());
        }
    }

    /**
     * URL类统计信息
     *
     * 该公共类表示一个URL（JAR包）的类统计信息，
     * 包括该URL加载的类数量、匹配的类数量和类列表。
     */
    public static class UrlClassStat {
        /**
         * URL字符串
         */
        private String url;

        /**
         * 该URL加载的类总数
         */
        private int loadedClassCount;

        /**
         * 匹配过滤器条件的类数量
         * 只有设置了类过滤器时此值才有意义
         */
        private Integer matchedClassCount;

        /**
         * 类名列表
         * 在详细模式下，包含该URL加载的类名（可能被截断）
         */
        private List<String> classes;

        /**
         * 类列表是否被截断
         * true表示由于数量限制，类列表被截断，还有更多类未显示
         */
        private boolean truncated;

        /**
         * 获取URL字符串
         *
         * @return URL字符串
         */
        public String getUrl() {
            return url;
        }

        /**
         * 设置URL字符串
         *
         * @param url URL字符串
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * 获取已加载类数量
         *
         * @return 已加载的类总数
         */
        public int getLoadedClassCount() {
            return loadedClassCount;
        }

        /**
         * 设置已加载类数量
         *
         * @param loadedClassCount 已加载的类总数
         */
        public void setLoadedClassCount(int loadedClassCount) {
            this.loadedClassCount = loadedClassCount;
        }

        /**
         * 获取匹配类数量
         *
         * @return 匹配过滤器条件的类数量，如果未设置过滤器则为null
         */
        public Integer getMatchedClassCount() {
            return matchedClassCount;
        }

        /**
         * 设置匹配类数量
         *
         * @param matchedClassCount 匹配过滤器条件的类数量
         */
        public void setMatchedClassCount(Integer matchedClassCount) {
            this.matchedClassCount = matchedClassCount;
        }

        /**
         * 获取类名列表
         *
         * @return 类名列表，在非详细模式下可能为null
         */
        public List<String> getClasses() {
            return classes;
        }

        /**
         * 设置类名列表
         *
         * @param classes 类名列表
         */
        public void setClasses(List<String> classes) {
            this.classes = classes;
        }

        /**
         * 检查类列表是否被截断
         *
         * @return true表示被截断，false表示完整
         */
        public boolean isTruncated() {
            return truncated;
        }

        /**
         * 设置类列表是否被截断
         *
         * @param truncated true表示被截断，false表示完整
         */
        public void setTruncated(boolean truncated) {
            this.truncated = truncated;
        }
    }

    /**
     * URL类统计信息构建器
     *
     * 该私有类用于构建UrlClassStat对象，
     * 在统计过程中临时存储数据。
     */
    private static class UrlClassStatBuilder {
        /**
         * URL字符串
         */
        private final String url;

        /**
         * 是否有类过滤器
         */
        private final boolean hasClassFilter;

        /**
         * 类列表限制
         * 0表示不限制，大于0表示最多保存的类数量
         */
        private final int limit;

        /**
         * 已加载类数量
         */
        private int loadedClassCount;

        /**
         * 匹配类数量
         */
        private int matchedClassCount;

        /**
         * 类名有序集合
         * 在详细模式下用于存储类名，按字母顺序排序
         */
        private SortedSet<String> classNames;

        /**
         * 是否被截断
         */
        private boolean truncated;

        /**
         * 构造函数
         *
         * @param url URL字符串
         * @param hasClassFilter 是否有类过滤器
         * @param limit 类列表限制，0表示不限制
         */
        UrlClassStatBuilder(String url, boolean hasClassFilter, int limit) {
            this.url = url;
            this.hasClassFilter = hasClassFilter;
            this.limit = limit;
            // 只有设置了限制才创建类名集合
            if (limit > 0) {
                this.classNames = new TreeSet<String>();
            }
        }

        /**
         * 增加已加载类数量
         */
        void increaseLoadedCount() {
            loadedClassCount++;
        }

        /**
         * 增加匹配类数量
         */
        void increaseMatchedCount() {
            matchedClassCount++;
        }

        /**
         * 获取匹配类数量
         *
         * @return 匹配的类数量
         */
        int getMatchedClassCount() {
            return matchedClassCount;
        }

        /**
         * 尝试添加类名
         *
         * 如果未达到限制，则添加类名到集合；
         * 如果已达到限制，则设置截断标志。
         *
         * @param className 要添加的类名
         */
        void tryAddClass(String className) {
            // 如果不需要保存类名，直接返回
            if (classNames == null) {
                return;
            }
            // 检查是否已达到限制
            if (classNames.size() >= limit) {
                // 设置截断标志
                truncated = true;
                return;
            }
            // 添加类名到有序集合
            classNames.add(className);
        }

        /**
         * 构建URL类统计对象
         *
         * @return 构建好的UrlClassStat对象
         */
        UrlClassStat build() {
            UrlClassStat stat = new UrlClassStat();
            stat.setUrl(url);
            stat.setLoadedClassCount(loadedClassCount);
            // 如果有类过滤器，设置匹配类数量
            if (hasClassFilter) {
                stat.setMatchedClassCount(matchedClassCount);
            }
            // 如果有类名集合，转换为列表
            if (classNames != null) {
                stat.setClasses(new ArrayList<String>(classNames));
            }
            stat.setTruncated(truncated);
            return stat;
        }
    }

    /**
     * 类加载器URL统计信息
     *
     * 该公共类表示一个类加载器的URL使用统计，
     * 包括已使用的URL和未使用的URL。
     */
    public static class ClassLoaderUrlStat {
        /**
         * 已使用的URL集合
         * 这些URL至少加载了一个类
         */
        private Collection<String> usedUrls;

        /**
         * 未使用的URL集合
         * 这些URL没有加载任何类
         */
        private Collection<String> unUsedUrls;

        /**
         * 默认构造函数
         */
        public ClassLoaderUrlStat() {
        }

        /**
         * 构造函数
         *
         * @param usedUrls 已使用的URL集合
         * @param unUsedUrls 未使用的URL集合
         */
        public ClassLoaderUrlStat(Collection<String> usedUrls, Collection<String> unUsedUrls) {
            super();
            this.usedUrls = usedUrls;
            this.unUsedUrls = unUsedUrls;
        }

        /**
         * 获取已使用的URL集合
         *
         * @return 已使用的URL集合
         */
        public Collection<String> getUsedUrls() {
            return usedUrls;
        }

        /**
         * 设置已使用的URL集合
         *
         * @param usedUrls 已使用的URL集合
         */
        public void setUsedUrls(Collection<String> usedUrls) {
            this.usedUrls = usedUrls;
        }

        /**
         * 获取未使用的URL集合
         *
         * @return 未使用的URL集合
         */
        public Collection<String> getUnUsedUrls() {
            return unUsedUrls;
        }

        /**
         * 设置未使用的URL集合
         *
         * @param unUsedUrls 未使用的URL集合
         */
        public void setUnUsedUrls(Collection<String> unUsedUrls) {
            this.unUsedUrls = unUsedUrls;
        }
    }

    /**
     * 类加载器统计信息
     *
     * 该公共类表示一种类型的类加载器的统计信息，
     * 包括该类型的实例数量和加载的类总数。
     */
    public static class ClassLoaderStat {
        /**
         * 该类型的类加载器加载的类总数
         */
        private int loadedCount;

        /**
         * 该类型的类加载器实例数量
         */
        private int numberOfInstance;

        /**
         * 增加已加载类数量
         *
         * @param count 要增加的数量
         */
        void addLoadedCount(int count) {
            this.loadedCount += count;
        }

        /**
         * 增加实例数量
         *
         * @param count 要增加的数量
         */
        void addNumberOfInstance(int count) {
            this.numberOfInstance += count;
        }

        /**
         * 获取已加载类数量
         *
         * @return 已加载的类总数
         */
        public int getLoadedCount() {
            return loadedCount;
        }

        /**
         * 获取实例数量
         *
         * @return 类加载器实例数量
         */
        public int getNumberOfInstance() {
            return numberOfInstance;
        }
    }

    /**
     * 值比较器
     *
     * 该比较器用于按类加载器的已加载类数量进行排序，
     * 实现降序排列（加载数量多的排在前面）。
     */
    private static class ValueComparator implements Comparator<String> {

        /**
         * 未排序的统计信息映射表
         */
        private Map<String, ClassLoaderStat> unsortedStats;

        /**
         * 构造函数
         *
         * @param stats 统计信息映射表
         */
        ValueComparator(Map<String, ClassLoaderStat> stats) {
            this.unsortedStats = stats;
        }

        /**
         * 比较两个类加载器类型的统计信息
         *
         * 按已加载类数量降序排序，数量多的排在前面。
         *
         * @param o1 第一个类加载器类型名
         * @param o2 第二个类加载器类型名
         * @return 比较结果，负数表示o1应该排在前面
         */
        @Override
        public int compare(String o1, String o2) {
            // 处理null情况
            if (null == unsortedStats) {
                return -1;
            }
            // 如果o1不在映射表中，排在后面
            if (!unsortedStats.containsKey(o1)) {
                return 1;
            }
            // 如果o2不在映射表中，排在前面
            if (!unsortedStats.containsKey(o2)) {
                return -1;
            }
            // 按已加载类数量降序排序（o2 - o1实现降序）
            return unsortedStats.get(o2).getLoadedCount() - unsortedStats.get(o1).getLoadedCount();
        }
    }

    /**
     * 类加载器命令中断处理器
     *
     * 该处理器在用户按Ctrl+C时被调用，
     * 设置命令的中断标志以停止命令执行。
     */
    private static class ClassLoaderInterruptHandler implements Handler<Void> {

        /**
         * 类加载器命令实例
         */
        private ClassLoaderCommand command;

        /**
         * 构造函数
         *
         * @param command 类加载器命令实例
         */
        public ClassLoaderInterruptHandler(ClassLoaderCommand command) {
            this.command = command;
        }

        /**
         * 处理中断事件
         *
         * 当用户按Ctrl+C时，此方法被调用，
         * 设置命令的中断标志。
         *
         * @param event 事件对象（Void类型，无实际数据）
         */
        @Override
        public void handle(Void event) {
            // 设置命令的中断标志
            command.isInterrupted = true;
        }
    }
}
