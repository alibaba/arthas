package com.taobao.arthas.core.command.monitor200;

import java.util.Arrays;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * Watch命令类
 * 用于观察方法的调用过程，包括方法入参、返回值、抛出异常等信息
 * 支持在方法调用前、调用后（正常返回或抛出异常）等不同时机进行观察
 */
@Name("watch")
// 命令摘要：显示指定方法调用的输入/输出参数、返回对象和抛出的异常
@Summary("Display the input/output parameter, return object, and thrown exception of specified method invocation")
// 命令详细描述，包含示例和文档链接
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  watch org.apache.commons.lang.StringUtils isBlank\n" +
        "  watch org.apache.commons.lang.StringUtils isBlank '{params, target, returnObj, throwExp}' -x 2\n" +
        "  watch *StringUtils isBlank params[0] params[0].length==1\n" +
        "  watch *StringUtils isBlank params '#cost>100'\n" +
        "  watch -f *StringUtils isBlank params\n" +
        "  watch *StringUtils isBlank params[0]\n" +
        "  watch -E -b org\\.apache\\.commons\\.lang\\.StringUtils isBlank params[0]\n" +
        "  watch javax.servlet.Filter * --exclude-class-pattern com.demo.TestFilter\n" +
        "  watch OuterClass$InnerClass\n" +
        Constants.WIKI + Constants.WIKI_HOME + "watch")
public class WatchCommand extends EnhancerCommand {

    // 类名匹配模式
    private String classPattern;
    // 方法名匹配模式
    private String methodPattern;
    // 观察表达式（OGNL表达式）
    private String express;
    // 条件表达式（OGNL表达式）
    private String conditionExpress;
    // 是否在方法调用前观察
    private boolean isBefore = false;
    // 是否在方法调用后观察（默认启用）
    private boolean isFinish = false;
    // 是否在方法抛出异常时观察
    private boolean isException = false;
    // 是否在方法正常返回时观察
    private boolean isSuccess = false;
    // 对象展开层级（默认为1）
    private Integer expand = 1;
    // 结果大小限制（字节数）
    private Integer sizeLimit;
    // 是否启用正则表达式匹配（默认使用通配符匹配）
    private boolean isRegEx = false;
    // 执行次数限制（默认为100次）
    private int numberOfLimit = 100;
    
    /**
     * 设置类名匹配模式（第0个位置参数）
     *
     * @param classPattern 类名模式，支持通配符
     */
    @Argument(index = 0, argName = "class-pattern")
    @Description("The full qualified class name you want to watch")
    public void setClassPattern(String classPattern) {
        // 标准化类名，将.替换为/
        this.classPattern = StringUtils.normalizeClassName(classPattern);
    }

    /**
     * 设置方法名匹配模式（第1个位置参数）
     *
     * @param methodPattern 方法名模式，支持通配符
     */
    @Argument(index = 1, argName = "method-pattern")
    @Description("The method name you want to watch")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    /**
     * 设置观察表达式（第2个位置参数）
     * 使用OGNL表达式指定要观察的内容
     *
     * @param express OGNL表达式，默认值为'{params, target, returnObj}'
     */
    @Argument(index = 2, argName = "express", required = false)
    @DefaultValue("{params, target, returnObj}")
    @Description("The content you want to watch, written by ognl. Default value is '{params, target, returnObj}'\n" + Constants.EXPRESS_EXAMPLES)
    public void setExpress(String express) {
        this.express = express;
    }

    /**
     * 设置条件表达式（第3个位置参数）
     * 只有当条件表达式满足时才进行观察
     *
     * @param conditionExpress OGNL条件表达式
     */
    @Argument(index = 3, argName = "condition-express", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    /**
     * 设置是否在方法调用前进行观察
     *
     * @param before true表示在方法调用前观察
     */
    @Option(shortName = "b", longName = "before", flag = true)
    @Description("Watch before invocation")
    public void setBefore(boolean before) {
        isBefore = before;
    }

    /**
     * 设置是否在方法调用后进行观察（默认启用）
     *
     * @param finish true表示在方法调用后观察
     */
    @Option(shortName = "f", longName = "finish", flag = true)
    @Description("Watch after invocation, enable by default")
    public void setFinish(boolean finish) {
        isFinish = finish;
    }

    /**
     * 设置是否在方法抛出异常时进行观察
     *
     * @param exception true表示在方法抛出异常时观察
     */
    @Option(shortName = "e", longName = "exception", flag = true)
    @Description("Watch after throw exception")
    public void setException(boolean exception) {
        isException = exception;
    }

    /**
     * 设置是否在方法正常返回时进行观察
     *
     * @param success true表示在方法正常返回时观察
     */
    @Option(shortName = "s", longName = "success", flag = true)
    @Description("Watch after successful invocation")
    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    /**
     * 设置结果大小限制
     * 用于防止观察结果过大导致性能问题
     *
     * @param sizeLimit 大小限制（字节数），必须大于0
     */
    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (must be greater than 0, default value comes from options object-size-limit)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    /**
     * 设置对象展开层级
     * 控制对象属性的显示深度
     *
     * @param expand 展开层级，默认为1，最大值为ObjectView.MAX_DEEP
     */
    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default), the max value is " + ObjectView.MAX_DEEP)
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    /**
     * 设置是否启用正则表达式匹配
     * 默认使用通配符匹配，启用后使用正则表达式匹配
     *
     * @param regEx true表示启用正则表达式匹配
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置执行次数限制
     * 当观察次数达到限制后，命令会自动终止
     *
     * @param numberOfLimit 执行次数阈值
     */
    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    /**
     * 设置类加载器的哈希码
     * 用于指定特定的类加载器来查找类
     *
     * @param hashCode 类加载器的哈希码
     */
    @Override
    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        super.setHashCode(hashCode);
    }

    // Getter方法

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public String getExpress() {
        return express;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public boolean isBefore() {
        return isBefore;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public boolean isException() {
        return isException;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public Integer getExpand() {
        return expand;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    /**
     * 获取类名匹配器
     * 使用延迟初始化，在第一次使用时创建匹配器
     *
     * @return 类名匹配器
     */
    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            // 根据类名模式和是否启用正则表达式创建匹配器
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
        }
        return classNameMatcher;
    }

    /**
     * 获取类名排除匹配器
     * 用于排除特定的类，使用延迟初始化
     *
     * @return 类名排除匹配器，如果没有设置排除模式则返回null
     */
    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            // 根据排除类名模式和是否启用正则表达式创建匹配器
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    /**
     * 获取方法名匹配器
     * 使用延迟初始化，在第一次使用时创建匹配器
     *
     * @return 方法名匹配器
     */
    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            // 根据方法名模式和是否启用正则表达式创建匹配器
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }

    /**
     * 处理命令执行
     * 在执行前验证参数，然后调用父类的处理逻辑
     *
     * @param process 命令进程对象
     */
    @Override
    public void process(CommandProcess process) {
        // 验证sizeLimit参数是否合法
        String validateError = validateSizeLimit(sizeLimit);
        if (validateError != null) {
            // 如果验证失败，结束命令并返回错误信息
            process.end(-1, validateError);
            return;
        }
        // 调用父类的处理逻辑
        super.process(process);
    }

    /**
     * 创建监听器
     * 为每个匹配的方法创建观察监听器
     *
     * @param process 命令进程对象
     * @return WatchAdviceListener监听器实例
     */
    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        // 创建WatchAdviceListener，传入命令对象、进程对象和是否启用详细输出
        return new WatchAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    /**
     * 完成第3个参数（条件表达式）的自动补全
     * 提供常用表达式的示例
     *
     * @param completion 补全上下文对象
     */
    @Override
    protected void completeArgument3(Completion completion) {
        // 使用预定义的表达式示例进行补全
        CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }

    /**
     * 验证sizeLimit参数
     * 确保sizeLimit的值大于0
     *
     * @param sizeLimit 要验证的sizeLimit值
     * @return 如果验证失败返回错误信息，否则返回null
     */
    static String validateSizeLimit(Integer sizeLimit) {
        // sizeLimit必须大于0
        if (sizeLimit != null && sizeLimit.intValue() <= 0) {
            return "sizeLimit must be greater than 0.";
        }
        return null;
    }
}
