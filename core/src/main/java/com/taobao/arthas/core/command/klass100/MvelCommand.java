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
 * @author xhinliang
 *
 */
@Name("mvel")
@Summary("Execute mvel expression.")
@Description(Constants.EXAMPLE //
        + "  mvel 'java.lang.System.out.println(\"hello\")' \n" //
        + "  mvel -x 2 'com.taobao.Singleton.getInstance()' \n" //
        + "  mvel 'com.taobao.Demo.staticFiled' \n" //
        + "  mvel 'def getBeanByName(name) { com.example.BeanFactory.getBean(name); }' \n" //
        + "  mvel -x 2 'xxxService.getInstance() // xxxService loaded by BeanFactory.' \n" //
        + "" + Constants.WIKI + Constants.WIKI_HOME + "mvel\n" //
        + "  http://mvel.documentnode.com/" //
)
public class MvelCommand extends AnnotatedCommand {

    private static final Logger logger = LogUtil.getArthasLogger();

    private String express;

    private String hashCode;

    private int expand = 3;

    @Argument(argName = "express", index = 0)
    @Description("The mvel expression.")
    public void setExpress(String express) {
        this.express = express;
    }

    @Option(shortName = "c", longName = "classLoader")
    @Description("The hash code of the special class's classLoader, default classLoader is SystemClassLoader.")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (3 by default).")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    @Override
    public void process(CommandProcess process) {
        int exitCode = 0;
        try {
            Instrumentation inst = process.session().getInstrumentation();
            ClassLoader classLoader;
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

            Express unpooledExpress = ExpressFactory.mvelExpress(classLoader);
            try {
                Object value = unpooledExpress.get(express);
                String result = StringUtils.objectToString(expand >= 0 ? new ObjectView(value, expand).draw() : value);
                process.write(result + "\n");
            } catch (ExpressException e) {
                logger.warn("mvel: failed execute express: " + express, e);
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
