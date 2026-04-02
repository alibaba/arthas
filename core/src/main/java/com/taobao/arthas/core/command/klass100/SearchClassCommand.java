package com.taobao.arthas.core.command.klass100;


// Java Instrumentation API 导入
import java.lang.instrument.Instrumentation;
// 集合类导入
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// Arthas 核心类导入
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassDetailVO;
import com.taobao.arthas.core.command.model.SearchClassModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ResultUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
// CLI 注解导入
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * SearchClass 命令 - 搜索和展示 JVM 中已加载的类信息
 *
 * 该命令提供了强大的类搜索功能：
 * 1. 支持通配符模式匹配（默认）和正则表达式匹配
 * 2. 支持显示类的详细信息（包括类加载器、接口、父类等）
 * 3. 支持显示类的所有成员变量
 * 4. 支持按类加载器进行过滤
 * 5. 支持分页显示大量类信息
 *
 * 使用场景：
 * - 查看某个类是否已被加载
 * - 查看类的详细结构信息
 * - 查看类是由哪个类加载器加载的
 * - 查看特定类加载器加载的所有类
 *
 * @author vlinux
 */
@Name("sc")
@Summary("Search all the classes loaded by JVM")
@Description(Constants.EXAMPLE +
        "  sc -d org.apache.commons.lang.StringUtils\n" +
        "  sc -d org/apache/commons/lang/StringUtils\n" +
        "  sc -d *StringUtils\n" +
        "  sc -d -f org.apache.commons.lang.StringUtils\n" +
        "  sc -E org\\\\.apache\\\\.commons\\\\.lang\\\\.StringUtils\n" +
        Constants.WIKI + Constants.WIKI_HOME + "sc")
public class SearchClassCommand extends AnnotatedCommand {
    // 类名匹配模式，支持通配符和正则表达式
    private String classPattern;
    // 是否显示类的详细信息
    private boolean isDetail = false;
    // 是否显示类的所有成员变量
    private boolean isField = false;
    // 是否使用正则表达式进行匹配（false 表示使用通配符）
    private boolean isRegEx = false;
    // 类加载器的哈希码，用于过滤特定类加载器加载的类
    private String hashCode = null;
    // 类加载器的类名，用于通过类名查找类加载器
    private String classLoaderClass;
    // 类加载器的 toString() 返回值，用于匹配特定的类加载器
    private String classLoaderToString;
    // 对象展开的层级数，用于控制详细信息的展示深度
    private Integer expand;
    // 匹配类的数量限制，默认为 100，防止返回过多结果
    private int numberOfLimit = 100;

    /**
     * 设置类名匹配模式
     * 支持使用 '.' 或 '/' 作为包名分隔符，会自动规范化
     *
     * @param classPattern 类名匹配模式
     */
    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        // 规范化类名，统一使用 '.' 作为分隔符
        this.classPattern = StringUtils.normalizeClassName(classPattern);
    }

    /**
     * 设置是否显示类的详细信息
     *
     * @param detail true 表示显示详细信息
     */
    @Option(shortName = "d", longName = "details", flag = true)
    @Description("Display the details of class")
    public void setDetail(boolean detail) {
        isDetail = detail;
    }

    /**
     * 设置是否显示类的所有成员变量
     *
     * @param field true 表示显示所有成员变量
     */
    @Option(shortName = "f", longName = "field", flag = true)
    @Description("Display all the member variables")
    public void setField(boolean field) {
        isField = field;
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
     * 设置对象展开的层级数
     * 控制详细信息的展示深度
     *
     * @param expand 展开层级数
     */
    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (0 by default)")
    public void setExpand(Integer expand) {
        this.expand = expand;
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
    @Description("Maximum number of matching classes with details (100 by default)")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    /**
     * 设置类加载器的 toString() 返回值
     * 用于匹配特定的类加载器实例
     *
     * @param classLoaderToString 类加载器的 toString() 返回值
     */
    @Option(shortName = "cs", longName = "classLoaderStr")
    @Description("The return value of the special class's ClassLoader#toString().")
    public void setClassLoaderToString(String classLoaderToString) {
        this.classLoaderToString = classLoaderToString;
    }

    /**
     * 处理 sc 命令的核心方法
     * 执行类搜索并返回结果
     *
     * @param process 命令处理进程对象，用于获取 Instrumentation 实例和返回结果
     */
    @Override
    public void process(final CommandProcess process) {
        // 创建影响行数统计对象，用于记录搜索结果的数量
        RowAffect affect = new RowAffect();
        // 获取 Java Instrumentation 实例，用于访问 JVM 中已加载的类
        Instrumentation inst = process.session().getInstrumentation();

        // 处理类加载器的匹配逻辑
        // 如果没有指定 hashCode，但指定了 classLoaderClass 或 classLoaderToString，需要查找类加载器
        if (hashCode == null && (classLoaderClass != null || classLoaderToString != null)) {
            // 根据类加载器的类名或 toString() 值查找匹配的类加载器
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoader(inst, classLoaderClass, classLoaderToString);
            // 构建提示信息，用于错误消息
            String tips = "";
            if (classLoaderClass != null) {
                tips = "class name: " + classLoaderClass;
            }
            if (classLoaderToString != null) {
                tips = tips + (StringUtils.isEmpty(tips) ? "ClassLoader#toString(): " : ", ClassLoader#toString(): ") + classLoaderToString;
            }
            // 根据匹配结果进行处理
            if (matchedClassLoaders.size() == 1) {
                // 找到唯一的匹配类加载器，使用其哈希码
                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                // 找到多个匹配的类加载器，需要用户明确指定
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                SearchClassModel searchclassModel = new SearchClassModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(searchclassModel);
                process.end(-1, "Found more than one classloader by " + tips + ", please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                // 没有找到匹配的类加载器
                process.end(-1, "Can not find classloader by " + tips + ".");
                return;
            }
        }

        // 执行类搜索操作
        // 根据 classPattern、isRegEx 和 hashCode 搜索匹配的类
        List<Class<?>> matchedClasses = new ArrayList<Class<?>>(SearchUtils.searchClass(inst, classPattern, isRegEx, hashCode));
        // 按类名对搜索结果进行排序，使输出更易读
        Collections.sort(matchedClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> c1, Class<?> c2) {
                return StringUtils.classname(c1).compareTo(StringUtils.classname(c2));
            }
        });

        // 根据是否需要详细信息来决定输出格式
        if (isDetail) {
            // 需要显示详细信息
            // 检查匹配的类数量是否超过限制
            if (numberOfLimit > 0 && matchedClasses.size() > numberOfLimit) {
                process.end(-1, "The number of matching classes is greater than : " + numberOfLimit+". \n" +
                        "Please specify a more accurate 'class-patten' or use the parameter '-n' to change the maximum number of matching classes.");
                return;
            }
            // 遍历所有匹配的类，输出详细信息
            for (Class<?> clazz : matchedClasses) {
                // 创建类的详细信息对象
                ClassDetailVO classInfo = ClassUtils.createClassInfo(clazz, isField, expand);
                // 将信息添加到输出结果中
                process.appendResult(new SearchClassModel(classInfo, isDetail, isField));
            }
        } else {
            // 只需要显示类名列表，使用分页方式输出
            // 每页最多显示 256 个类名
            int pageSize = 256;
            // 使用分页处理器输出类名
            ResultUtils.processClassNames(matchedClasses, pageSize, new ResultUtils.PaginationHandler<List<String>>() {
                @Override
                public boolean handle(List<String> classNames, int segment) {
                    // 输出当前页的类名列表
                    process.appendResult(new SearchClassModel(classNames, segment));
                    return true;
                }
            });
        }

        // 记录影响的行数（匹配的类数量）
        affect.rCnt(matchedClasses.size());
        // 添加影响行数到输出结果
        process.appendResult(new RowAffectModel(affect));
        // 正常结束命令处理
        process.end();
    }

    /**
     * 实现命令行的自动补全功能
     * 当用户输入类名时，可以自动补全已加载的类名
     *
     * @param completion 补全上下文对象，包含当前输入信息
     */
    @Override
    public void complete(Completion completion) {
        // 尝试进行类名补全
        if (!CompletionUtils.completeClassName(completion)) {
            // 如果类名补全失败，使用父类的默认补全逻辑
            super.complete(completion);
        }
    }
}
