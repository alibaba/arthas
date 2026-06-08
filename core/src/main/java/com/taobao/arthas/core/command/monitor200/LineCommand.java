package com.taobao.arthas.core.command.monitor200;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.LineNumberNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.LineEnhanceOptions;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.LineListModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

@Name("line")
@Summary("Watch local variables and expression result at specified source line numbers")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  line --class demo.MathGame --line 51\n" +
        "  line --class demo.MathGame --line 51,56 --express '{params, localVarMap}'\n" +
        "  line --class demo.MathGame --method primeFactors --line 51 --condition 'localVarMap[\"i\"] > 10'\n" +
        "  line --class demo.MathGame --method primeFactors --desc '(I)Ljava/util/List;' --line 51\n" +
        "  line --list-lines --class demo.MathGame\n")
public class LineCommand extends EnhancerCommand {
    static final int MAX_LINE_COUNT = 256;
    static final int MAX_STACK_DEPTH = 256;
    static final String DEFAULT_EXPRESS = "{params, localVarMap}";

    private String classPattern;
    private String methodPattern;
    private String methodDesc;
    private List<String> lineSpecs;
    private Set<Integer> lineNumbers;
    private String express = DEFAULT_EXPRESS;
    private String conditionExpress;
    private boolean listLines;
    private boolean isRegEx;
    private Integer expand = 1;
    private Integer sizeLimit;
    private int numberOfLimit = 100;
    private boolean stack;
    private int stackDepth = 32;

    @Option(longName = "class")
    @Description("The full qualified class name you want to watch")
    public void setClassPattern(String classPattern) {
        this.classPattern = StringUtils.normalizeClassName(classPattern);
    }

    @Option(longName = "method")
    @Description("The method name you want to watch, optional")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Option(longName = "desc")
    @Description("The JVM method descriptor, optional")
    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    @Option(longName = "line", acceptMultipleValues = true)
    @Description("Source line numbers, supports comma separated values and repeated options")
    public void setLineSpecs(List<String> lineSpecs) {
        this.lineSpecs = lineSpecs;
    }

    @Option(longName = "express")
    @DefaultValue(DEFAULT_EXPRESS)
    @Description("The content you want to watch, written by ognl. Default value is '{params, localVarMap}'")
    public void setExpress(String express) {
        this.express = express;
    }

    @Option(longName = "condition")
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Option(longName = "list-lines", flag = true)
    @Description("List available line numbers from LineNumberTable without enhancing classes")
    public void setListLines(boolean listLines) {
        this.listLines = listLines;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default), the max value is " + ObjectView.MAX_DEEP)
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (must be greater than 0, default value comes from options object-size-limit)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Option(longName = "stack", flag = true)
    @Description("Print current stack trace when line probe hits")
    public void setStack(boolean stack) {
        this.stack = stack;
    }

    @Option(longName = "stack-depth")
    @DefaultValue("32")
    @Description("Stack trace depth when --stack is enabled")
    public void setStackDepth(int stackDepth) {
        this.stackDepth = stackDepth;
    }

    @Override
    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        super.setHashCode(hashCode);
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public String getExpress() {
        return express;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public Integer getExpand() {
        return expand;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    public boolean isStack() {
        return stack;
    }

    public int getStackDepth() {
        return stackDepth;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    @Override
    public void process(CommandProcess process) {
        String validateError = validate();
        if (validateError != null) {
            process.end(-1, validateError);
            return;
        }
        if (listLines) {
            listLines(process);
            return;
        }
        super.process(process);
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new LineCommandAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    protected LineEnhanceOptions getLineEnhanceOptions() {
        return new LineEnhanceOptions(lineNumbers, methodDesc);
    }

    private String validate() {
        if (StringUtils.isEmpty(classPattern)) {
            return "The --class option is required.";
        }
        if (StringUtils.isEmpty(express)) {
            return "The --express option must not be empty.";
        }
        if (methodDesc != null && !methodDesc.startsWith("(")) {
            return "The --desc option must be a JVM method descriptor, for example '(I)V'.";
        }
        if (!listLines) {
            try {
                lineNumbers = parseLines(lineSpecs);
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
        }
        if (expand == null || expand < 0 || expand > ObjectView.MAX_DEEP) {
            return "expand must be between 0 and " + ObjectView.MAX_DEEP + ".";
        }
        String sizeLimitError = WatchCommand.validateSizeLimit(sizeLimit);
        if (sizeLimitError != null) {
            return sizeLimitError;
        }
        if (numberOfLimit <= 0) {
            return "limits must be greater than 0.";
        }
        if (stackDepth <= 0 || stackDepth > MAX_STACK_DEPTH) {
            return "stack-depth must be between 1 and " + MAX_STACK_DEPTH + ".";
        }
        return null;
    }

    static Set<Integer> parseLines(List<String> lineSpecs) {
        if (lineSpecs == null || lineSpecs.isEmpty()) {
            throw new IllegalArgumentException("The --line option is required unless --list-lines is used.");
        }
        Set<Integer> result = new LinkedHashSet<Integer>();
        for (String lineSpec : lineSpecs) {
            if (StringUtils.isEmpty(lineSpec)) {
                continue;
            }
            String[] parts = lineSpec.split(",");
            for (String part : parts) {
                String value = part.trim();
                if (value.length() == 0) {
                    continue;
                }
                if (value.indexOf('-') >= 0) {
                    throw new IllegalArgumentException("Line range syntax is not supported in the first version: "
                            + value);
                }
                final int lineNumber;
                try {
                    lineNumber = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid line number: " + value);
                }
                if (lineNumber <= 0) {
                    throw new IllegalArgumentException("Line number must be greater than 0: " + value);
                }
                result.add(lineNumber);
                if (result.size() > MAX_LINE_COUNT) {
                    throw new IllegalArgumentException("Too many line numbers, maximum is " + MAX_LINE_COUNT + ".");
                }
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("The --line option is required unless --list-lines is used.");
        }
        return result;
    }

    private void listLines(CommandProcess process) {
        try {
            Instrumentation instrumentation = process.session().getInstrumentation();
            Set<Class<?>> classes = GlobalOptions.isDisableSubClass
                    ? SearchUtils.searchClass(instrumentation, getClassNameMatcher())
                    : SearchUtils.searchSubClass(instrumentation, SearchUtils.searchClass(instrumentation,
                            getClassNameMatcher()));
            if (classes.size() > maxNumOfMatchedClass) {
                process.end(-1, "The number of matched classes is " + classes.size()
                        + ", greater than the limit value " + maxNumOfMatchedClass
                        + ". Try to change the limit with option '-m <arg>'.");
                return;
            }
            int count = 0;
            for (Class<?> clazz : classes) {
                if (!isListableClass(clazz)) {
                    continue;
                }
                count += appendLineListModels(process, clazz);
            }
            if (count == 0) {
                process.end(-1, "No available line number is found.");
                return;
            }
            process.end();
        } catch (Throwable e) {
            process.end(-1, "list line numbers failed: " + e.getMessage());
        }
    }

    private boolean isListableClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        if (hashCode != null && (clazz.getClassLoader() == null
                || !Integer.toHexString(clazz.getClassLoader().hashCode()).equalsIgnoreCase(hashCode))) {
            return false;
        }
        if (clazz.getClassLoader() == LineCommand.class.getClassLoader()) {
            return false;
        }
        if (!GlobalOptions.isUnsafe && clazz.getClassLoader() == null) {
            return false;
        }
        return getClassNameExcludeMatcher() == null || !getClassNameExcludeMatcher().matching(clazz.getName());
    }

    private int appendLineListModels(CommandProcess process, Class<?> clazz) throws Exception {
        ClassNode classNode = AsmUtils.loadClass(clazz);
        int count = 0;
        for (MethodNode methodNode : classNode.methods) {
            if (!isListLineMethodMatched(methodNode)) {
                continue;
            }
            List<Integer> lines = collectLines(methodNode);
            if (lines.isEmpty()) {
                continue;
            }
            LineListModel model = new LineListModel();
            model.setClassName(clazz.getName());
            model.setSourceFile(classNode.sourceFile);
            model.setMethodName(methodNode.name);
            model.setMethodDesc(methodNode.desc);
            model.setLines(lines);
            process.appendResult(model);
            count++;
        }
        return count;
    }

    private boolean isListLineMethodMatched(MethodNode methodNode) {
        if (methodNode == null || (methodNode.access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT
                || (methodNode.access & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE) {
            return false;
        }
        if ("<clinit>".equals(methodNode.name) || "<init>".equals(methodNode.name)) {
            return false;
        }
        if (!getMethodNameMatcher().matching(methodNode.name)) {
            return false;
        }
        return methodDesc == null || methodDesc.length() == 0 || methodDesc.equals(methodNode.desc);
    }

    private List<Integer> collectLines(MethodNode methodNode) {
        Set<Integer> lines = new LinkedHashSet<Integer>();
        for (AbstractInsnNode insnNode = methodNode.instructions.getFirst(); insnNode != null; insnNode = insnNode
                .getNext()) {
            if (insnNode instanceof LineNumberNode) {
                lines.add(((LineNumberNode) insnNode).line);
            }
        }
        List<Integer> result = new ArrayList<Integer>(lines);
        Collections.sort(result);
        return result;
    }
}
