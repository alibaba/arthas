package com.taobao.arthas.core.command.klass100;

import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.List;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.OgnlModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * OGNL表达式执行命令
 *
 * 用于在应用程序中执行OGNL（Object-Graph Navigation Language）表达式。
 * 可以访问和修改应用程序中的静态字段、调用方法、创建对象等。
 * 支持指定ClassLoader来限定表达式执行的环境。
 *
 * @author hengyunabc 2018-10-18
 *
 */
@Name("ognl")
@Summary("Execute ognl expression.")
@Description(Constants.EXAMPLE
                + "  ognl '@java.lang.System@out.println(\"hello \\u4e2d\\u6587\")' \n"
                + "  ognl -x 2 '@Singleton@getInstance()' \n"
                + "  ognl '@Demo@staticFiled' \n"
                + "  ognl '#value1=@System@getProperty(\"java.home\"), #value2=@System@getProperty(\"java.runtime.name\"), {#value1, #value2}'\n"
                + "  ognl -c 5d113a51 '@com.taobao.arthas.core.GlobalOptions@isDump' \n"
                + Constants.WIKI + Constants.WIKI_HOME + "ognl\n"
                + "  https://commons.apache.org/proper/commons-ognl/language-guide.html")
public class OgnlCommand extends AnnotatedCommand {
    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(OgnlCommand.class);

    /**
     * OGNL表达式字符串
     */
    private String express;

    /**
     * ClassLoader的哈希码
     */
    private String hashCode;

    /**
     * ClassLoader的类名
     */
    private String classLoaderClass;

    /**
     * 对象展开级别，默认为1
     * 控制输出对象时的详细程度，值越大显示的层次越深
     */
    private int expand = 1;

    /**
     * 设置OGNL表达式
     *
     * @param express OGNL表达式字符串
     */
    @Argument(argName = "express", index = 0, required = true)
    @Description("The ognl expression.")
    public void setExpress(String express) {
        this.express = express;
    }

    /**
     * 设置ClassLoader的哈希码
     *
     * @param hashCode ClassLoader的哈希码，如果未设置则默认使用SystemClassLoader
     */
    @Option(shortName = "c", longName = "classLoader")
    @Description("The hash code of the special class's classLoader, default classLoader is SystemClassLoader.")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    /**
     * 设置ClassLoader的类名
     *
     * @param classLoaderClass 指定类的ClassLoader的类名
     */
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    /**
     * 设置对象展开级别
     *
     * @param expand 展开级别，默认为1
     */
    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default).")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    /**
     * 处理命令
     *
     * 根据参数确定ClassLoader，执行OGNL表达式并返回结果
     *
     * @param process 命令处理上下文
     */
    @Override
    public void process(CommandProcess process) {
        // 获取Instrumentation实例
        Instrumentation inst = process.session().getInstrumentation();
        ClassLoader classLoader = null;

        // 如果指定了ClassLoader哈希码，则查找对应的ClassLoader
        if (hashCode != null) {
            classLoader = ClassLoaderUtils.getClassLoader(inst, hashCode);
            if (classLoader == null) {
                process.end(-1, "Can not find classloader with hashCode: " + hashCode + ".");
                return;
            }
        } else if (classLoaderClass != null) {
            // 如果指定了ClassLoader类名，则通过类名查找ClassLoader
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);

            if (matchedClassLoaders.size() == 1) {
                // 只找到一个ClassLoader，直接使用
                classLoader = matchedClassLoaders.get(0);
            } else if (matchedClassLoaders.size() > 1) {
                // 找到多个ClassLoader，提示用户选择
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                OgnlModel ognlModel = new OgnlModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(ognlModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                // 没有找到ClassLoader
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        } else {
            // 如果没有指定ClassLoader，使用系统ClassLoader
            classLoader = ClassLoader.getSystemClassLoader();
        }

        // 创建非池化的表达式执行器
        Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);

        try {
            // 绑定一个空对象作为根对象，然后执行表达式
            // https://github.com/alibaba/arthas/issues/2892
            Object value = unpooledExpress.bind(new Object()).get(express);

            // 创建结果模型，包含表达式执行结果和对象展开信息
            OgnlModel ognlModel = new OgnlModel()
                    .setValue(new ObjectVO(value, expand));

            // 添加结果到处理上下文
            process.appendResult(ognlModel);

            // 成功结束命令处理
            process.end();
        } catch (ExpressException e) {
            // 表达式执行失败，记录警告日志
            logger.warn("ognl: failed execute express: " + express, e);

            // 返回错误信息
            process.end(-1, "Failed to execute ognl, exception message: " + e.getMessage()
                    + ", please check $HOME/logs/arthas/arthas.log for more details. ");
        }
    }
}
