package com.taobao.arthas.core.command.klass100;


// Java Instrumentation API 导入
import java.lang.instrument.Instrumentation;
// Java 反射 API 导入
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
// 集合类导入
import java.util.Set;
import java.util.Collection;
import java.util.List;

// 日志相关导入
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
// Arthas 核心类导入
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.SearchMethodModel;
import com.taobao.arthas.core.command.model.MethodVO;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
// 匹配器相关导入
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
// CLI 注解导入
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * SearchMethod 命令 - 搜索和展示类的方法信息
 *
 * 该命令提供了强大的方法搜索功能：
 * 1. 支持通配符模式匹配（默认）和正则表达式匹配类名和方法名
 * 2. 支持显示方法的详细信息（包括参数类型、返回类型、修饰符等）
 * 3. 支持搜索构造函数（使用 <init> 表示）
 * 4. 支持按类加载器进行过滤
 * 5. 支持限制匹配结果的数量
 *
 * 使用场景：
 * - 查看某个类有哪些方法
 * - 查看某个方法的详细信息（参数、返回值、修饰符等）
 * - 查找特定命名模式的方法
 * - 了解类的方法结构
 *
 * @author vlinux
 * @author hengyunabc 2019-02-13
 */
@Name("sm")
@Summary("Search the method of classes loaded by JVM")
@Description(Constants.EXAMPLE +
        "  sm java.lang.String\n" +
        "  sm -d org.apache.commons.lang.StringUtils\n" +
        "  sm -d org/apache/commons/lang/StringUtils\n" +
        "  sm *StringUtils *\n" +
        "  sm -Ed org\\\\.apache\\\\.commons\\\\.lang\\.StringUtils .*\n" +
        Constants.WIKI + Constants.WIKI_HOME + "sm")
public class SearchMethodCommand extends AnnotatedCommand {
    // 日志记录器，用于记录命令执行过程中的各种信息
    private static final Logger logger = LoggerFactory.getLogger(SearchMethodCommand.class);

    // 类名匹配模式，支持通配符和正则表达式
    private String classPattern;
    // 方法名匹配模式，支持通配符和正则表达式
    private String methodPattern;
    // 类加载器的哈希码，用于过滤特定类加载器加载的类
    private String hashCode = null;
    // 类加载器的类名，用于通过类名查找类加载器
    private String classLoaderClass;
    // 是否显示方法的详细信息
    private boolean isDetail = false;
    // 是否使用正则表达式进行匹配（false 表示使用通配符）
    private boolean isRegEx = false;
    // 匹配类的数量限制，默认为 100，防止返回过多结果
    private int numberOfLimit = 100;

    /**
     * 设置类名匹配模式
     * 支持使用 '.' 或 '/' 作为包名分隔符
     *
     * @param classPattern 类名匹配模式
     */
    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    /**
     * 设置方法名匹配模式
     * 可选参数，如果不指定则匹配所有方法
     *
     * @param methodPattern 方法名匹配模式
     */
    @Argument(argName = "method-pattern", index = 1, required = false)
    @Description("Method name pattern")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    /**
     * 设置是否显示方法的详细信息
     *
     * @param detail true 表示显示详细信息
     */
    @Option(shortName = "d", longName = "details", flag = true)
    @Description("Display the details of method")
    public void setDetail(boolean detail) {
        isDetail = detail;
    }

    /**
     * 设置是否使用正则表达式进行匹配
     *
     * @param regEx true 表示使用正则表达式，false 表示使用通配符
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置类加载器的哈希码
     *
     * @param hashCode 类加载器的十六进制哈希码字符串
     */
    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    /**
     * 设置类加载器的类名
     *
     * @param classLoaderClass 类加载器的完整类名
     */
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    /**
     * 设置匹配类的数量限制
     *
     * @param numberOfLimit 最大匹配类数量，默认为 100
     */
    @Option(shortName = "n", longName = "limits")
    @Description("Maximum number of matching classes (100 by default)")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    /**
     * 处理 sm 命令的核心方法
     * 执行方法搜索并返回结果
     *
     * @param process 命令处理进程对象，用于获取 Instrumentation 实例和返回结果
     */
    @Override
    public void process(CommandProcess process) {
        // 创建影响行数统计对象，用于记录搜索结果的数量
        RowAffect affect = new RowAffect();

        // 获取 Java Instrumentation 实例，用于访问 JVM 中已加载的类
        Instrumentation inst = process.session().getInstrumentation();
        // 创建方法名匹配器，用于过滤方法
        Matcher<String> methodNameMatcher = methodNameMatcher();

        // 处理类加载器的匹配逻辑
        // 如果没有指定 hashCode，但指定了 classLoaderClass，需要查找类加载器
        if (hashCode == null && classLoaderClass != null) {
            // 根据类加载器的类名查找匹配的类加载器
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                // 找到唯一的匹配类加载器，使用其哈希码
                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                // 找到多个匹配的类加载器，需要用户明确指定
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                SearchMethodModel searchmethodModel = new SearchMethodModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(searchmethodModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                // 没有找到匹配的类加载器
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        // 根据类模式搜索匹配的类
        Set<Class<?>> matchedClasses = SearchUtils.searchClass(inst, classPattern, isRegEx, hashCode);

        // 检查匹配的类数量是否超过限制
        if (numberOfLimit > 0 && matchedClasses.size() > numberOfLimit) {
            process.end(-1, "The number of matching classes is greater than : " + numberOfLimit+". \n" +
                    "Please specify a more accurate 'class-patten' or use the parameter '-n' to change the maximum number of matching classes.");
            return;
        }
        // 遍历所有匹配的类
        for (Class<?> clazz : matchedClasses) {
            try {
                // 处理构造函数
                // 在 Java 中，构造函数用 <init> 表示
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    // 检查构造函数是否匹配方法名模式
                    if (!methodNameMatcher.matching("<init>")) {
                        continue;
                    }

                    // 创建构造函数的信息对象
                    MethodVO methodInfo = ClassUtils.createMethodInfo(constructor, clazz, isDetail);
                    // 将信息添加到输出结果中
                    process.appendResult(new SearchMethodModel(methodInfo, isDetail));
                    // 增加影响行数计数
                    affect.rCnt(1);
                }

                // 处理普通方法
                for (Method method : clazz.getDeclaredMethods()) {
                    // 检查方法名是否匹配方法名模式
                    if (!methodNameMatcher.matching(method.getName())) {
                        continue;
                    }
                    // 创建方法的信息对象
                    MethodVO methodInfo = ClassUtils.createMethodInfo(method, clazz, isDetail);
                    // 将信息添加到输出结果中
                    process.appendResult(new SearchMethodModel(methodInfo, isDetail));
                    // 增加影响行数计数
                    affect.rCnt(1);
                }
            } catch (Error e) {
                // 处理类时发生错误，记录错误并终止命令
                //print failed className
                String msg = String.format("process class failed: %s, error: %s", clazz.getName(), e.toString());
                logger.error(msg, e);
                process.end(1, msg);
                return;
            }
        }

        // 添加影响行数到输出结果
        process.appendResult(new RowAffectModel(affect));
        // 正常结束命令处理
        process.end();
    }

    /**
     * 创建方法名匹配器
     * 根据配置返回正则表达式匹配器或通配符匹配器
     * 如果没有指定方法名模式，则自动设置为匹配所有方法
     *
     * @return 方法名匹配器
     */
    private Matcher<String> methodNameMatcher() {
        // 自动修正默认的方法名模式
        // 如果没有指定方法名模式，则设置为匹配所有方法
        if (StringUtils.isBlank(methodPattern)) {
            methodPattern = isRegEx ? ".*" : "*";
        }
        // 根据是否使用正则表达式返回相应的匹配器
        return isRegEx ? new RegexMatcher(methodPattern) : new WildcardMatcher(methodPattern);
    }

    /**
     * 实现命令行的自动补全功能
     * 支持两个参数的补全：
     * 1. 第一个参数：补全类名
     * 2. 第二个参数：补全方法名
     *
     * @param completion 补全上下文对象，包含当前输入信息
     */
    @Override
    public void complete(Completion completion) {
        // 检测当前正在补全第几个参数
        int argumentIndex = CompletionUtils.detectArgumentIndex(completion);

        // 第一个参数：类名补全
        if (argumentIndex == 1) {
            // 尝试补全类名
            if (!CompletionUtils.completeClassName(completion)) {
                // 如果类名补全失败，使用父类的默认补全逻辑
                super.complete(completion);
            }
            return;
        // 第二个参数：方法名补全
        } else if (argumentIndex == 2) {
            // 尝试补全方法名
            if (!CompletionUtils.completeMethodName(completion)) {
                // 如果方法名补全失败，使用父类的默认补全逻辑
                super.complete(completion);
            }
            return;
        }

        // 其他情况使用父类的默认补全逻辑
        super.complete(completion);
    }
}
