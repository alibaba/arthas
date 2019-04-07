package com.taobao.arthas.core.command.extention;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.klass100.OgnlCommand;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.MethodUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.taobao.text.Decoration.bold;
import static com.taobao.text.ui.Element.label;

/**
 * Date: 2019/4/7
 *
 * @author xuzhiyi
 */
@Name("springbean")
@Summary("Search the bean in spring applicationContext")
@Description(Constants.EXAMPLE +
             "  springbean -l\n" +
             "  springbean -l *Service\n" +
             "  springbean -l -d userController\n" +
             "  springbean -i welcomeController welcome\n" +
             "  springbean -i -m 73cf11cb welcomeController welcome '{\"id\":1,\"name\":\"tony\"}'\n" +
             Constants.WIKI + Constants.WIKI_HOME + "springbean")
public class SpringBeanCommand extends AnnotatedCommand {

    private String beanName;
    private String methodName;
    private String params;
    private Integer expand = 1;

    private String classHashCode;
    private ClassLoader classLoader;
    private boolean isShow = false;
    private boolean isInvoke = false;
    private boolean isDetail = false;
    private String methodHashCode;

    @Argument(argName = "beanName", index = 0, required = false)
    @Description("BeanName")
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Argument(argName = "method", index = 1, required = false)
    @Description("Method")
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Option(shortName = "l", longName = "list", flag = true)
    @Description("list")
    public void setShow(boolean show) {
        this.isShow = show;
    }

    @Option(shortName = "d", longName = "details", flag = true)
    @Description("Display the details of bean")
    public void setDetail(boolean detail) {
        this.isDetail = detail;
    }

    @Option(shortName = "i", longName = "invoke", flag = true)
    @Description("invoke")
    public void setInvoke(boolean invoke) {
        this.isInvoke = invoke;
    }

    @Argument(argName = "params", index = 2, required = false)
    @Description("Method params")
    public void setArgs(String params) {
        this.params = params;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default)")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    @Option(shortName = "m", longName = "method")
    @Description("The hash code of the special method")
    public void setCode(String methodHashCode) {
        this.methodHashCode = methodHashCode;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special ClassLoader")
    public void setHashCode(String classHashCode) {
        this.classHashCode = classHashCode;
    }

    private String getAdaptParams() {
        if (StringUtils.isEmpty(params)) {
            params = "[]";
        }
        if (!params.startsWith("[")) {
            params = "[" + params + "]";
        }
        return params;
    }

    @Override
    public void process(CommandProcess process) {
        if (StringUtils.isEmpty(GlobalOptions.springApplicationContextExpress)) {
            process.write("Spring application context express is empty.\n")
                .write("Please set spring-application-context use option command, such as\n")
                .write("options spring-application-context "
                       + "@com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory@contexts.iterator.next")
                .write("\n");
            process.end();
            return;
        }
        try {
            if (classHashCode == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            } else {
                classLoader = OgnlCommand.findClassLoader(process.session().getInstrumentation(), classHashCode);
            }
//            parseOgnl(process);
            Object beanFactory = getBeanFactory();
            if (isShow) {
                show(beanFactory, process);
            } else if (isInvoke) {
                invoke(beanFactory, process);
            } else {
                process.write("Option -l or -i is required\n")
                    .write("Use `help springbean` for detail usage\n");
            }
        } catch (Throwable e) {
            LogUtil.getArthasLogger().warn("springbean execute fail", e);
            process.write("Failed to execute springbean, exception message: " + e.getMessage()
                          + ", please check $HOME/logs/arthas/arthas.log for more details. \n");
        } finally {
            process.end();
        }
    }

    private void invoke(Object beanFactory, CommandProcess process) throws Exception {
        if (StringUtils.isEmpty(beanName)) {
            process.write("The argument 'beanName' is required\n");
            return;
        }
        if (StringUtils.isEmpty(methodName)) {
            process.write("The argument 'methodName' is required\n");
            return;
        }
        Object bean = getBean(beanName, beanFactory);
        Method[] methods = bean.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (isMethodMatch(method)) {
                boolean accessible = method.isAccessible();
                if (!accessible) {
                    method.setAccessible(true);
                }
                JSONArray objects = JSON.parseArray(getAdaptParams());
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length > 0 && objects.isEmpty()) {
                    process.write("The argument 'params' is required\n");
                    return;
                }
                if (parameterTypes.length != objects.size()) {
                    process.write("Params is not matched\n");
                    return;
                }
                Object[] args = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    Object param = objects.getObject(i, parameterTypes[i]);
                    args[i] = param;
                }
                Object result = method.invoke(bean, args);
                process.write(new ObjectView(result, expand).draw());
                process.write("\n");
                method.setAccessible(accessible);
                break;
            }
        }
    }

    private boolean isMethodMatch(Method method) {
        if (method.getName().equals(methodName)) {
            if (methodHashCode == null) {
                return true;
            }
            return MethodUtils.hashCode(method).equals(methodHashCode);
        }
        return false;
    }

    private Object getBeanFactory() throws Exception {
        Object applicationContext = getSpringApplicationContext();
        Class<?> applicationClass = applicationContext.getClass();
        Method beanFactoryMethod = applicationClass.getMethod("getAutowireCapableBeanFactory");
        return beanFactoryMethod.invoke(applicationContext);
    }

    private Object getBean(String beanName, Object beanFactory) throws Exception {
        Method getBeanMethod = beanFactory.getClass().getMethod("getBean", String.class);
        return getBeanMethod.invoke(beanFactory, beanName);
    }

    private Object getSpringApplicationContext() throws Exception {
        Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
        Class<?> applicationContextClass = classLoader.loadClass("org.springframework.context.ApplicationContext");
        Object value = unpooledExpress.get(GlobalOptions.springApplicationContextExpress);
        if (value == null) {
            throw new IllegalArgumentException("got null from spring application context express");
        }
        Class<?> valueClass = value.getClass();
        if (applicationContextClass.isAssignableFrom(valueClass)) {
            return value;
        } else {
            throw new IllegalArgumentException("got " + valueClass + " from spring application context express");
        }
    }

    private void show(Object beanFactory, CommandProcess process)
        throws Exception {
        Class<?> listableClass = classLoader.loadClass("org.springframework.beans.factory.ListableBeanFactory");
        Class<?> factoryClass = beanFactory.getClass();
        if (listableClass.isAssignableFrom(factoryClass)) {
            Method getBeanDefinitionNames = factoryClass.getMethod("getBeanDefinitionNames");
            String[] beanNames = (String[]) getBeanDefinitionNames.invoke(beanFactory);
            Set<String> beanNameSet = new TreeSet<String>(Arrays.asList(beanNames));
            for (String beanName : beanNameSet) {
                if (getNameMatcher().matching(beanName)) {
                    Object bean = getBean(beanName, beanFactory);
                    Class<?> beanClass = bean.getClass();
                    String beanClassName = beanClass.getName();
                    if (isDetail) {
                        Method isSingletonMethod = factoryClass.getMethod("isSingleton", String.class);
                        Boolean isSingleton = (Boolean) isSingletonMethod.invoke(beanFactory, beanName);
                        Method getAliasesMethod = factoryClass.getMethod("getAliases", String.class);
                        String[] aliases = (String[]) getAliasesMethod.invoke(beanFactory, beanName);
                        Class<?> springProxyClass = classLoader.loadClass("org.springframework.aop.SpringProxy");
                        boolean isSpringProxy = springProxyClass.isAssignableFrom(beanClass);
                        Object advisors = null;
                        if (isSpringProxy) {
                            Method getAdvisorsMethod = beanClass.getMethod("getAdvisors");
                            advisors = getAdvisorsMethod.invoke(bean);
                        }
                        process.write(RenderUtil.render(drawTableRow(beanName, beanClassName, isSingleton, isSpringProxy,
                                                                     aliases, new ObjectView(advisors, expand).draw()),
                                                        process.width()) + "\n");
                    } else {
                        process.write(RenderUtil.render(drawTableRow(beanName, beanClassName),
                                                        process.width()) + "\n");
                    }

                }
            }
        } else {
            process.write(beanFactory.getClass().getName() + " is not listable.\n");
        }
    }

    private Matcher<String> getNameMatcher() {
        if (StringUtils.isEmpty(beanName)) {
            beanName = "*";
        }
        return new WildcardMatcher(beanName);
    }

    private static TableElement drawTableRow(String beanName, String className) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        return table.row(label("bean-name").style(bold.bold()), label(beanName).style(bold.bold()))
            .row(label("class-name").style(bold.bold()), label(className));
    }

    private static TableElement drawTableRow(String beanName, String className, boolean isSingleton,
                                             boolean isSpringProxy, String[] aliases, String beanStr) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        return table.row(label("bean-name").style(bold.bold()), label(beanName).style(bold.bold()))
            .row(label("class-name").style(bold.bold()), label(className))
            .row(label("is-singleton").style(bold.bold()), label(String.valueOf(isSingleton)))
            .row(label("is-spring-proxy").style(bold.bold()), label(String.valueOf(isSpringProxy)))
            .row(label("aliases").style(bold.bold()), label(JSON.toJSONString(aliases)))
            .row(label("advisors").style(bold.bold()), label(beanStr));
    }
}
