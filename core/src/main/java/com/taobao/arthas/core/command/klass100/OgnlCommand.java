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
 *
 * @author hengyunabc 2018-10-18
 *
 */
@Name("ognl")
@Summary("Execute ognl expression.")
@Description(Constants.EXAMPLE
                + "  ognl '@java.lang.System@out.println(\"hello\")' \n"
                + "  ognl -x 2 '@Singleton@getInstance()' \n"
                + "  ognl '@Demo@staticFiled' \n"
                + "  ognl '#value1=@System@getProperty(\"java.home\"), #value2=@System@getProperty(\"java.runtime.name\"), {#value1, #value2}'\n"
                + "  ognl -c 5d113a51 '@com.taobao.arthas.core.GlobalOptions@isDump' \n"
                + Constants.WIKI + Constants.WIKI_HOME + "ognl\n"
                + "  https://commons.apache.org/proper/commons-ognl/language-guide.html")
public class OgnlCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(OgnlCommand.class);

    private String express;
    private String hashCode;
    private String classLoaderClass;
    private int expand = 1;

    @Argument(argName = "express", index = 0, required = true)
    @Description("The ognl expression.")
    public void setExpress(String express) {
        this.express = express;
    }

    @Option(shortName = "c", longName = "classLoader")
    @Description("The hash code of the special class's classLoader, default classLoader is SystemClassLoader.")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default).")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    @Override
    public void process(CommandProcess process) {
        Instrumentation inst = process.session().getInstrumentation();
        ClassLoader classLoader = null;
        if (hashCode != null) {
            classLoader = ClassLoaderUtils.getClassLoader(inst, hashCode);
            if (classLoader == null) {
                process.end(-1, "Can not find classloader with hashCode: " + hashCode + ".");
                return;
            }
        } else if (classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                classLoader = matchedClassLoaders.get(0);
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                OgnlModel ognlModel = new OgnlModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(ognlModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        } else {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
        try {
            Object value = unpooledExpress.get(express);
            OgnlModel ognlModel = new OgnlModel()
                    .setValue(value)
                    .setExpand(expand);
            process.appendResult(ognlModel);
            process.end();
        } catch (ExpressException e) {
            logger.warn("ognl: failed execute express: " + express, e);
            process.end(-1, "Failed to execute ognl, exception message: " + e.getMessage()
                    + ", please check $HOME/logs/arthas/arthas.log for more details. ");
        }
    }
}
