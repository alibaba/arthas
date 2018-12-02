package com.taobao.arthas.core.command.klass100;

import java.lang.instrument.Instrumentation;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.express.ExpressFactoryProvider;
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
@Name("el")
@Summary("Execute el expression.")
@Description(Constants.EXAMPLE
                + "  el 'js::System.out.println(\"hello\")' \n"
                + "  el -x 2 'groovy:: println(\"hello\")' \n"
                + "  el '@Demo@staticFiled' \n"
                 + "  el -c 5d113a51 'groovy::it.appClass(\"com.taobao.arthas.core.GlobalOptions\").isDump' \n"
            //    + Constants.WIKI + Constants.WIKI_HOME + "ognl\n"
           //     + "  https://commons.apache.org/proper/commons-ognl/language-guide.html"
                )
public class ElCommand extends AnnotatedCommand {
    private static final Logger logger = LogUtil.getArthasLogger();
    
    protected final String prefix;
    protected final ExpressFactoryProvider elProvider;
    protected final ExpressFactoryProvider defaultElProvider;
    
    public ElCommand( String prefix, String defaultPrefix ) {
		super();
		this.elProvider = prefix == null ? null :  ExpressFactory.getExpressFactoryProvider(prefix);
		this.defaultElProvider = defaultPrefix == null ?  null : ExpressFactory.getExpressFactoryProvider(defaultPrefix);
		this.prefix = prefix;
	}
    public ElCommand( final String prefix ) {
    	this(prefix,null);
    }
	public ElCommand() {
		this(null,System.getProperty("arthas_el_default",ExpressFactory.getDefaultEL()));
	}

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
            try {
                Object value =elProvider == null ?  ExpressFactory.evalExpress(classLoader,express.trim(),this.defaultElProvider )
                		: elProvider.createExpress(classLoader).get(express.trim());
                String result = StringUtils.objectToString(expand >= 0 ? new ObjectView(value, expand).draw() : value);
                process.write(result + "\n");
            } catch (ExpressException e) {
                logger.warn("{}: failed execute express: " + express, prefix== null ? "el:":prefix, e);
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
