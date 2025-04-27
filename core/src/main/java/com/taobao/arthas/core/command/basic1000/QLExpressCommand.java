package com.taobao.arthas.core.command.basic1000;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.OgnlModel;
import com.taobao.arthas.core.command.model.QLExpressModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.middleware.cli.annotations.*;

import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.List;

/**
 * @Author TaoKan
 * @Date 2025/3/23 8:59 PM
 */
@Name("ql")
@Summary("Execute qlexpress expression.")
@Description(Constants.EXAMPLE
        + "  ql '@java.lang.System@out.println(\"hello \\u4e2d\\u6587\")' \n"
        + "  ql -x 2 '@Singleton@getInstance()' \n"
        + "  ql '@Demo@staticFiled' \n"
        + "  ql '#value1=@System@getProperty(\"java.home\"), #value2=@System@getProperty(\"java.runtime.name\"), {#value1, #value2}'\n"
        + "  ql -c 5d113a51 '@com.taobao.arthas.core.GlobalOptions@isDump' \n"
        + Constants.WIKI + Constants.WIKI_HOME + "ql\n"
        + "  https://commons.apache.org/proper/commons-ognl/language-guide.html")
public class QLExpressCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(QLExpressCommand.class);

    private String express;
    private String hashCode;
    private String classLoaderClass;
    private int expand = 1;

    @Argument(argName = "express", index = 0, required = true)
    @Description("The qlexpress expression.")
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
                QLExpressModel qlModel = new QLExpressModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(qlModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        } else {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        Express unpooledExpress = ExpressFactory.unpooledExpressByQL(classLoader);
        try {
            Object value = unpooledExpress.bind(new Object()).get(express);
            QLExpressModel qlModel = new QLExpressModel()
                    .setValue(new ObjectVO(value, expand));
            process.appendResult(qlModel);
            process.end();
        } catch (ExpressException e) {
            logger.warn("qlexpress: failed execute express: " + express, e);
            process.end(-1, "Failed to execute qlexpress, exception message: " + e.getMessage()
                    + ", please check $HOME/logs/arthas/arthas.log for more details. ");
        }
    }
}
