package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.GetStaticModel;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.ExitStatus;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.CommandUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.Collection;

/**
 * getstatic 命令实现类
 * 用于查看类的静态字段值，支持通过 OGNL 表达式进一步处理结果
 *
 * @author diecui1202 on 2017/9/27.
 */

@Name("getstatic")
@Summary("Show the static field of a class")
@Description(Constants.EXAMPLE +
             "  getstatic demo.MathGame random\n" +
             "  getstatic -c 39eb305e org.apache.log4j.LogManager DEFAULT_CONFIGURATION_FILE\n" +
             Constants.WIKI + Constants.WIKI_HOME + "getstatic")
public class GetStaticCommand extends AnnotatedCommand {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(GetStaticCommand.class);

    // 类名匹配模式
    private String classPattern;
    // 字段名匹配模式
    private String fieldPattern;
    // OGNL 表达式，用于进一步处理字段值
    private String express;
    // 类加载器的哈希码
    private String hashCode = null;
    // 类加载器的类名
    private String classLoaderClass;
    // 是否使用正则表达式匹配
    private boolean isRegEx = false;
    // 对象展开层级，默认为 1
    private int expand = 1;

    /**
     * 设置类名匹配模式
     *
     * @param classPattern 类名模式，支持使用 '.' 或 '/' 作为分隔符
     */
    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    /**
     * 设置字段名匹配模式
     *
     * @param fieldPattern 字段名模式
     */
    @Argument(argName = "field-pattern", index = 1)
    @Description("Field name pattern")
    public void setFieldPattern(String fieldPattern) {
        this.fieldPattern = fieldPattern;
    }

    /**
     * 设置 OGNL 表达式
     * 用于对获取到的静态字段值进行进一步处理
     *
     * @param express OGNL 表达式字符串
     */
    @Argument(argName = "express", index = 2, required = false)
    @Description("the content you want to watch, written by ognl")
    public void setExpress(String express) {
        this.express = express;
    }

    /**
     * 设置类加载器的哈希码
     * 用于指定在特定的类加载器中查找类
     *
     * @param hashCode 类加载器的哈希码（十六进制字符串）
     */
    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    /**
     * 设置类加载器的类名
     * 通过类加载器的类名来指定使用哪个类加载器
     *
     * @param classLoaderClass 类加载器的类名
     */
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    /**
     * 设置是否使用正则表达式匹配
     * 默认使用通配符匹配
     *
     * @param regEx true 表示使用正则表达式，false 表示使用通配符
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置对象展开层级
     * 控制对象属性的展开深度
     *
     * @param expand 展开层级，默认为 1
     */
    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default)")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    /**
     * 处理 getstatic 命令
     * 这是命令的核心执行方法
     *
     * @param process 命令处理上下文对象
     */
    @Override
    public void process(CommandProcess process) {
        // 创建行影响统计对象，用于记录处理的行数
        RowAffect affect = new RowAffect();
        // 获取 JVM Instrumentation 实例，用于类操作
        Instrumentation inst = process.session().getInstrumentation();

        // 如果没有指定类加载器哈希码，但指定了类加载器类名
        if (hashCode == null && classLoaderClass != null) {
            // 通过类加载器类名查找匹配的类加载器
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            // 如果只找到一个匹配的类加载器
            if (matchedClassLoaders.size() == 1) {
                // 将类加载器的哈希码转换为十六进制字符串
                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                // 如果找到多个匹配的类加载器，提示用户使用 -c 参数指定
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                GetStaticModel getStaticModel = new GetStaticModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(getStaticModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                // 如果没有找到匹配的类加载器
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        // 根据类名模式搜索匹配的类
        Set<Class<?>> matchedClasses = SearchUtils.searchClassOnly(inst, classPattern, isRegEx, hashCode);
        try {
            // 如果没有找到匹配的类
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                process.end(-1, "No class found for: " + classPattern);
                return;
            }
            ExitStatus status = null;
            // 如果找到多个匹配的类
            if (matchedClasses.size() > 1) {
                // 处理多个匹配的情况，要求用户指定类加载器
                status = processMatches(process, matchedClasses);
            } else {
                // 只找到一个匹配的类，直接处理
                status = processExactMatch(process, affect, inst, matchedClasses);
            }
            // 添加行影响统计结果
            process.appendResult(new RowAffectModel(affect));
            // 结束命令处理
            CommandUtils.end(process, status);
        } catch (Throwable e){
            // 捕获所有异常，记录错误日志
            logger.error("processing error", e);
            process.appendResult(new RowAffectModel(affect));
            process.end(-1, "processing error");
        }
    }

    /**
     * 处理精确匹配的情况
     * 当只找到一个匹配的类时，获取该类的静态字段值
     *
     * @param process 命令处理上下文
     * @param affect 行影响统计对象
     * @param inst JVM Instrumentation 实例
     * @param matchedClasses 匹配的类集合（只包含一个类）
     * @return 命令执行状态
     */
    private ExitStatus processExactMatch(CommandProcess process, RowAffect affect, Instrumentation inst,
                                   Set<Class<?>> matchedClasses) {
        // 创建字段名匹配器，根据配置选择正则表达式或通配符匹配
        Matcher<String> fieldNameMatcher = fieldNameMatcher();

        // 获取匹配的类（集合中只有一个元素）
        Class<?> clazz = matchedClasses.iterator().next();

        // 标记是否找到了匹配的静态字段
        boolean found = false;

        // 遍历类的所有声明字段
        for (Field field : clazz.getDeclaredFields()) {
            // 跳过非静态字段或字段名不匹配的字段
            if (!Modifier.isStatic(field.getModifiers()) || !fieldNameMatcher.matching(field.getName())) {
                continue;
            }
            // 如果字段不可访问，设置为可访问（打破封装）
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                // 获取静态字段的值（传入 null 因为是静态字段）
                Object value = field.get(null);

                // 如果指定了 OGNL 表达式，使用表达式处理字段值
                if (!StringUtils.isEmpty(express)) {
                    value = ExpressFactory.threadLocalExpress(value).get(express);
                }

                // 将结果添加到命令输出
                process.appendResult(new GetStaticModel(field.getName(), value, expand));

                // 增加行影响计数
                affect.rCnt(1);
            } catch (IllegalAccessException e) {
                // 处理非法访问异常
                logger.warn("getstatic: failed to get static value, class: {}, field: {} ", clazz, field.getName(), e);
                process.appendResult(new MessageModel("Failed to get static, exception message: " + e.getMessage()
                              + ", please check $HOME/logs/arthas/arthas.log for more details. "));
            } catch (ExpressException e) {
                // 处理表达式执行异常
                logger.warn("getstatic: failed to get express value, class: {}, field: {}, express: {}", clazz, field.getName(), express, e);
                process.appendResult(new MessageModel("Failed to get static, exception message: " + e.getMessage()
                              + ", please check $HOME/logs/arthas/arthas.log for more details. "));
            } finally {
                // 标记至少找到了一个匹配的字段
                found = true;
            }
        }

        // 根据是否找到匹配的字段返回相应的状态
        if (!found) {
            return ExitStatus.failure(-1, "getstatic: no matched static field was found");
        } else {
            return ExitStatus.success();
        }
    }

    /**
     * 处理多个类匹配的情况
     * 当找到多个匹配的类时，提示用户使用 -c 参数指定类加载器
     *
     * @param process 命令处理上下文
     * @param matchedClasses 匹配的类集合
     * @return 命令执行状态（失败状态）
     */
    private ExitStatus processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {

//        Element usage = new LabelElement("getstatic -c <hashcode> " + classPattern + " " + fieldPattern).style(
//                Decoration.bold.fg(Color.blue));
//        process.write("\n Found more than one class for: " + classPattern + ", Please use " + RenderUtil.render(usage, process.width()));
        //TODO support message style
        // 构建使用提示字符串
        String usage = "getstatic -c <hashcode> " + classPattern + " " + fieldPattern;
        process.appendResult(new MessageModel("Found more than one class for: " + classPattern + ", Please use: "+usage));

        // 将匹配的类信息转换为视图对象并添加到结果
        List<ClassVO> matchedClassVOs = ClassUtils.createClassVOList(matchedClasses);
        process.appendResult(new GetStaticModel(matchedClassVOs));
        return ExitStatus.failure(-1, "Found more than one class for: " + classPattern + ", Please use: "+usage);
    }

    /**
     * 创建字段名匹配器
     * 根据配置返回正则表达式匹配器或通配符匹配器
     *
     * @return 字段名匹配器
     */
    private Matcher<String> fieldNameMatcher() {
        // 根据是否启用正则表达式返回相应的匹配器
        return isRegEx ? new RegexMatcher(fieldPattern) : new WildcardMatcher(fieldPattern);
    }
}
