package com.taobao.arthas.core.command.hidden;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.Option;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.TokenUtils;
import com.taobao.arthas.core.util.matcher.EqualsMatcher;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.reflect.FieldUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.taobao.arthas.core.util.ArthasCheckUtils.isIn;
import static com.taobao.text.ui.Element.label;
import static java.lang.String.format;

/**
 * 选项开关命令
 *
 * @author vlinux on 15/6/6.
 */
@Name("options")
@Summary("View and change various Arthas options")
@Description(Constants.EXAMPLE + "options dump true\n"+ "options unsafe true\n" +
        Constants.WIKI + Constants.WIKI_HOME + "options")
public class OptionsCommand extends AnnotatedCommand {
    private String optionName;
    private String optionValue;

    @Argument(index = 0, argName = "options-name", required = false)
    @Description("Option name")
    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    @Argument(index = 1, argName = "options-value", required = false)
    @Description("Option value")
    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    @Override
    public void process(CommandProcess process) {
        try {
            if (isShow()) {
                processShow(process);
            } else if (isShowName()) {
                processShowName(process);
            } else {
                processChangeNameValue(process);
            }
        } catch (Throwable t) {
            // ignore
        } finally {
            process.end();
        }
    }

    /**
     * complete first argument(options-name), other case use default complete
     *
     * @param completion the completion object
     */
    @Override
    public void complete(Completion completion) {
        int argumentIndex = CompletionUtils.detectArgumentIndex(completion);
        List<CliToken> lineTokens = completion.lineTokens();
        if (argumentIndex == 1) {
            String laseToken = TokenUtils.getLast(lineTokens).value().trim();
            //prefix match options-name
            String pattern = "^" + laseToken + ".*";
            Collection<String> optionNames = findOptionNames(new RegexMatcher(pattern));
            CompletionUtils.complete(completion, optionNames);
        } else {
            super.complete(completion);
        }
    }

    private void processShow(CommandProcess process) throws IllegalAccessException {
        Collection<Field> fields = findOptionFields(new RegexMatcher(".*"));
        process.write(RenderUtil.render(drawShowTable(fields), process.width()));
    }

    private void processShowName(CommandProcess process) throws IllegalAccessException {
        Collection<Field> fields = findOptionFields(new EqualsMatcher<String>(optionName));
        process.write(RenderUtil.render(drawShowTable(fields), process.width()));
    }

    private void processChangeNameValue(CommandProcess process) throws IllegalAccessException {
        Collection<Field> fields = findOptionFields(new EqualsMatcher<String>(optionName));

        // name not exists
        if (fields.isEmpty()) {
            process.write(format("options[%s] not found.\n", optionName));
            return;
        }

        Field field = fields.iterator().next();
        Option optionAnnotation = field.getAnnotation(Option.class);
        Class<?> type = field.getType();
        Object beforeValue = FieldUtils.readStaticField(field);
        Object afterValue;

        try {
            // try to case string to type
            if (isIn(type, int.class, Integer.class)) {
                FieldUtils.writeStaticField(field, afterValue = Integer.valueOf(optionValue));
            } else if (isIn(type, long.class, Long.class)) {
                FieldUtils.writeStaticField(field, afterValue = Long.valueOf(optionValue));
            } else if (isIn(type, boolean.class, Boolean.class)) {
                FieldUtils.writeStaticField(field, afterValue = Boolean.valueOf(optionValue));
            } else if (isIn(type, double.class, Double.class)) {
                FieldUtils.writeStaticField(field, afterValue = Double.valueOf(optionValue));
            } else if (isIn(type, float.class, Float.class)) {
                FieldUtils.writeStaticField(field, afterValue = Float.valueOf(optionValue));
            } else if (isIn(type, byte.class, Byte.class)) {
                FieldUtils.writeStaticField(field, afterValue = Byte.valueOf(optionValue));
            } else if (isIn(type, short.class, Short.class)) {
                FieldUtils.writeStaticField(field, afterValue = Short.valueOf(optionValue));
            } else if (isIn(type, short.class, String.class)) {
                FieldUtils.writeStaticField(field, afterValue = optionValue);
            } else {
                process.write(format("Options[%s] type[%s] desupported.\n", optionName, type.getSimpleName()));
                return;
            }

        } catch (Throwable t) {
            process.write(format("Cannot cast option value[%s] to type[%s].\n", optionValue, type.getSimpleName()));
            return;
        }

        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("NAME").style(Decoration.bold.bold()),
                label("BEFORE-VALUE").style(Decoration.bold.bold()),
                label("AFTER-VALUE").style(Decoration.bold.bold()));
        table.row(optionAnnotation.name(), StringUtils.objectToString(beforeValue),
                StringUtils.objectToString(afterValue));
        process.write(RenderUtil.render(table, process.width()));
    }


    /**
     * 判断当前动作是否需要展示整个options
     */
    private boolean isShow() {
        return StringUtils.isBlank(optionName) && StringUtils.isBlank(optionValue);
    }


    /**
     * 判断当前动作是否需要展示某个Name的值
     */
    private boolean isShowName() {
        return !StringUtils.isBlank(optionName) && StringUtils.isBlank(optionValue);
    }

    private Collection<Field> findOptionFields(Matcher<String> optionNameMatcher) {
        final Collection<Field> matchFields = new ArrayList<Field>();
        for (final Field optionField : FieldUtils.getAllFields(GlobalOptions.class)) {
            if (isMatchOptionAnnotation(optionField, optionNameMatcher)) {
                matchFields.add(optionField);
            }
        }
        return matchFields;
    }

    private Collection<String> findOptionNames(Matcher<String> optionNameMatcher) {
        final Collection<String> matchOptionNames = new ArrayList<String>();
        for (final Field optionField : FieldUtils.getAllFields(GlobalOptions.class)) {
            if (isMatchOptionAnnotation(optionField, optionNameMatcher)) {
                final Option optionAnnotation = optionField.getAnnotation(Option.class);
                matchOptionNames.add(optionAnnotation.name());
            }
        }
        return matchOptionNames;
    }

    private boolean isMatchOptionAnnotation(Field optionField, Matcher<String> optionNameMatcher) {
        if (!optionField.isAnnotationPresent(Option.class)) {
            return false;
        }
        final Option optionAnnotation = optionField.getAnnotation(Option.class);
        return optionAnnotation != null && optionNameMatcher.matching(optionAnnotation.name());
    }

    private Element drawShowTable(Collection<Field> optionFields) throws IllegalAccessException {
        TableElement table = new TableElement(1, 1, 2, 1, 3, 6)
                .leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("LEVEL").style(Decoration.bold.bold()),
                label("TYPE").style(Decoration.bold.bold()),
                label("NAME").style(Decoration.bold.bold()),
                label("VALUE").style(Decoration.bold.bold()),
                label("SUMMARY").style(Decoration.bold.bold()),
                label("DESCRIPTION").style(Decoration.bold.bold()));

        for (final Field optionField : optionFields) {
            final Option optionAnnotation = optionField.getAnnotation(Option.class);
            table.row("" + optionAnnotation.level(),
                    optionField.getType().getSimpleName(),
                    optionAnnotation.name(),
                    "" + optionField.get(null),
                    optionAnnotation.summary(),
                    optionAnnotation.description());
        }
        return table;
    }
}
