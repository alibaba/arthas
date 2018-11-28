package com.taobao.arthas.core.command.klass100;

import java.lang.instrument.Instrumentation;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.logger.Logger;

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
    private static final Logger logger = LogUtil.getArthasLogger();

    private String express;

    private String hashCode;
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

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default).")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    @Override
    public void process(CommandProcess process) {
        int exitCode = 0;
        try {
            Instrumentation inst = process.session().getInstrumentation();
            ClassLoader classLoader = null;
            if (hashCode == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            } else {
                classLoader = findClassLoader(inst, hashCode);
            }

            if (classLoader == null) {
                process.write("Can not find classloader with hashCode: " + hashCode + ".\n");
                exitCode = -1;
                return;
            }

            Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
            try {
                Object value = unpooledExpress.get(express);
                String result = StringUtils.objectToString(expand >= 0 ? new ObjectView(value, expand).draw() : value);
                process.write(result + "\n");
            } catch (ExpressException e) {
                logger.warn("ognl: failed execute express: " + express, e);
                process.write("Failed to get static, exception message: " + e.getMessage()
                                + ", please check $HOME/logs/arthas/arthas.log for more details. \n");
                exitCode = -1;
            }
        } finally {
            process.end(exitCode);
        }
    }

    private static ClassLoader findClassLoader(Instrumentation inst, String hashCode) {
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader != null && hashCode.equals(Integer.toHexString(classLoader.hashCode()))) {
                return classLoader;
            }
        }
        return null;
    }

}
