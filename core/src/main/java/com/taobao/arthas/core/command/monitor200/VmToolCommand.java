package com.taobao.arthas.core.command.monitor200;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.VmToolUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.SearchClassModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.cli.OptionCompleteHandler;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import arthas.VmTool;

/**
 * 
 * @author hengyunabc 2021-04-27
 * @author ZhangZiCheng 2021-04-29
 *
 */
//@formatter:off
@Name("vmtool")
@Summary("jvm tool")
@Description(Constants.EXAMPLE
        + "  vmtool --action getInstances --className demo.MathGame\n"
        + "  vmtool --action getInstances --className demo.MathGame --express 'instances.size()'\n"
        + Constants.WIKI + Constants.WIKI_HOME + "vmtool")
//@formatter:on
public class VmToolCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(VmToolCommand.class);

    private VmToolAction action;
    private String className;
    private String express;

    private String hashCode = null;
    private String classLoaderClass;
    /**
     * default value 2
     */
    private int expand;

    private static String libPath;
    private static VmTool vmTool = null;

    static {
        String libName = VmToolUtils.detectLibName();
        if (libName != null) {
            CodeSource codeSource = VmToolCommand.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                try {
                    File bootJarPath = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                    File soFile = new File(bootJarPath.getParentFile(), "lib" + File.separator + libName);
                    if (soFile.exists()) {
                        libPath = soFile.getAbsolutePath();
                    }
                } catch (Throwable e) {
                    logger.error("can not find VmTool so", e);
                }
            }
        }

    }

    @Option(shortName = "a", longName = "action", required = true)
    @Description("Action to execute")
    public void setAction(VmToolAction action) {
        this.action = action;
    }

    @Option(longName = "className")
    @Description("The class name")
    public void setClassName(String className) {
        this.className = className;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (2 by default)")
    @DefaultValue("2")
    public void setExpand(int expand) {
        this.expand = expand;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Option(longName = "express", required = false)
    @Description("The ognl expression, default valueis `instances`.")
    public void setExpress(String express) {
        this.express = express;
    }

    public enum VmToolAction {
        getInstances, load
    }

    @Override
    public void process(final CommandProcess process) {
        try {
            Instrumentation inst = process.session().getInstrumentation();

            if (VmToolAction.getInstances.equals(action)) {
                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                if (hashCode == null && classLoaderClass != null) {
                    List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst,
                            classLoaderClass);
                    if (matchedClassLoaders.size() == 1) {
                        classLoader = matchedClassLoaders.get(0);
                        hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
                    } else if (matchedClassLoaders.size() > 1) {
                        Collection<ClassLoaderVO> classLoaderVOList = ClassUtils
                                .createClassLoaderVOList(matchedClassLoaders);
                        SearchClassModel searchclassModel = new SearchClassModel().setClassLoaderClass(classLoaderClass)
                                .setMatchedClassLoaders(classLoaderVOList);
                        process.appendResult(searchclassModel);
                        process.end(-1,
                                "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                        return;
                    } else {
                        process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                        return;
                    }
                }

                List<Class<?>> matchedClasses = new ArrayList<Class<?>>(
                        SearchUtils.searchClass(inst, className, false, hashCode));
                int matchedClassSize = matchedClasses.size();
                if (matchedClassSize == 0) {
                    process.end(-1, "Can not find class by class name: " + className + ".");
                    return;
                } else if (matchedClassSize > 1) {
                    process.end(-1, "Found more than one class: " + matchedClasses + ".");
                    return;
                } else {
                    Object[] instances = vmToolInstance().getInstances(matchedClasses.get(0));
                    Object value = instances;
                    if (express != null) {
                        Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
                        try {
                            value = unpooledExpress.bind(new InstancesWrapper(instances)).get(express);
                        } catch (ExpressException e) {
                            logger.warn("ognl: failed execute express: " + express, e);
                            process.end(-1, "Failed to execute ognl, exception message: " + e.getMessage()
                                    + ", please check $HOME/logs/arthas/arthas.log for more details. ");
                        }
                    }

                    process.write(new ObjectView(value, this.expand).draw());
                    process.end();
                }
            }

            process.end();
        } catch (Throwable e) {
            logger.error("vmtool error", e);
            process.end(1, "vmtool error: " + e.getMessage());
        }
    }

    static class InstancesWrapper {
        Object instances;

        public InstancesWrapper(Object instances) {
            this.instances = instances;
        }

        public Object getInstances() {
            return instances;
        }

        public void setInstances(Object instances) {
            this.instances = instances;
        }
    }

    private VmTool vmToolInstance() {
        if (vmTool != null) {
            return vmTool;
        } else {
            vmTool = VmTool.getInstance(libPath);
        }
        return vmTool;
    }

    private Set<String> actions() {
        Set<String> values = new HashSet<String>();
        for (VmToolAction action : VmToolAction.values()) {
            values.add(action.toString());
        }
        return values;
    }

    @Override
    public void complete(Completion completion) {
        List<OptionCompleteHandler> handlers = new ArrayList<OptionCompleteHandler>();

        handlers.add(new OptionCompleteHandler() {

            @Override
            public boolean matchName(String token) {
                return "-a".equals(token) || "--action".equals(token);
            }

            @Override
            public boolean complete(Completion completion) {
                return CompletionUtils.complete(completion, actions());
            }

        });

        handlers.add(new OptionCompleteHandler() {
            @Override
            public boolean matchName(String token) {
                return "--className".equals(token);
            }

            @Override
            public boolean complete(Completion completion) {
                return CompletionUtils.completeClassName(completion);
            }
        });

        if (CompletionUtils.completeOptions(completion, handlers)) {
            return;
        }

        super.complete(completion);
    }

}