package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.Option;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ChangeResultVO;
import com.taobao.arthas.core.command.model.OptionVO;
import com.taobao.arthas.core.command.model.OptionsModel;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.ExitStatus;
import com.taobao.arthas.core.util.CommandUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.taobao.arthas.core.util.ArthasCheckUtils.isIn;
import static java.lang.String.format;

/**
 * 选项开关命令
 * 用于查看和修改Arthas的各种全局选项
 *
 * @author vlinux on 15/6/6.
 */
// @formatter:off
@Name("options")
@Summary("View and change various Arthas options")
@Description(Constants.EXAMPLE +
        "options       # list all options\n" +
        "options json-format true\n" +
        "options dump true\n" +
        "options unsafe true\n" +
        Constants.WIKI + Constants.WIKI_HOME + "options")
//@formatter:on
public class OptionsCommand extends AnnotatedCommand {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(OptionsCommand.class);

    // 选项名称
    private String optionName;
    // 选项值
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
            ExitStatus status = null;
            // 判断是哪种操作：显示所有选项、显示指定选项、还是修改选项值
            if (isShow()) {
                // 显示所有选项
                status = processShow(process);
            } else if (isShowName()) {
                // 显示指定名称的选项
                status = processShowName(process);
            } else {
                // 修改选项值
                status = processChangeNameValue(process);
            }

            // 结束处理
            CommandUtils.end(process, status);
        } catch (Throwable t) {
            logger.error("processing error", t);
            process.end(-1, "processing error");
        }
    }

    /**
     * 命令自动补全
     * 对第一个参数（选项名）进行补全，其他情况使用默认补全
     *
     * @param completion 补全对象
     */
    @Override
    public void complete(Completion completion) {
        int argumentIndex = CompletionUtils.detectArgumentIndex(completion);
        List<CliToken> lineTokens = completion.lineTokens();
        // 如果是第一个参数（选项名），进行前缀匹配补全
        if (argumentIndex == 1) {
            String laseToken = TokenUtils.getLast(lineTokens).value().trim();
            // 构建前缀匹配模式
            String pattern = "^" + laseToken + ".*";
            Collection<String> optionNames = findOptionNames(new RegexMatcher(pattern));
            CompletionUtils.complete(completion, optionNames);
        } else {
            // 其他情况使用默认补全
            super.complete(completion);
        }
    }

    /**
     * 处理显示所有选项的请求
     * @param process 命令处理进程
     * @return 退出状态
     * @throws IllegalAccessException 访问字段异常
     */
    private ExitStatus processShow(CommandProcess process) throws IllegalAccessException {
        // 查找所有选项字段
        Collection<Field> fields = findOptionFields(new RegexMatcher(".*"));
        process.appendResult(new OptionsModel(convertToOptionVOs(fields)));
        return ExitStatus.success();
    }

    /**
     * 处理显示指定选项的请求
     * @param process 命令处理进程
     * @return 退出状态
     * @throws IllegalAccessException 访问字段异常
     */
    private ExitStatus processShowName(CommandProcess process) throws IllegalAccessException {
        // 查找指定名称的选项字段
        Collection<Field> fields = findOptionFields(new EqualsMatcher<String>(optionName));
        process.appendResult(new OptionsModel(convertToOptionVOs(fields)));
        return ExitStatus.success();
    }

    /**
     * 处理修改选项值的请求
     * @param process 命令处理进程
     * @return 退出状态
     * @throws IllegalAccessException 访问字段异常
     */
    private ExitStatus processChangeNameValue(CommandProcess process) throws IllegalAccessException {
        // 查找指定名称的选项字段
        Collection<Field> fields = findOptionFields(new EqualsMatcher<String>(optionName));

        // 选项不存在
        if (fields.isEmpty()) {
            return ExitStatus.failure(-1, format("options[%s] not found.", optionName));
        }

        Field field = fields.iterator().next();
        Option optionAnnotation = field.getAnnotation(Option.class);
        Class<?> type = field.getType();
        // 读取修改前的值
        Object beforeValue = FieldUtils.readStaticField(field);
        Object afterValue;

        try {
            // 尝试将字符串值转换为目标类型
            if (isIn(type, int.class, Integer.class)) {
                afterValue = Integer.valueOf(optionValue);
            } else if (isIn(type, long.class, Long.class)) {
                afterValue = Long.valueOf(optionValue);
            } else if (isIn(type, boolean.class, Boolean.class)) {
                afterValue = Boolean.valueOf(optionValue);
            } else if (isIn(type, double.class, Double.class)) {
                afterValue = Double.valueOf(optionValue);
            } else if (isIn(type, float.class, Float.class)) {
                afterValue = Float.valueOf(optionValue);
            } else if (isIn(type, byte.class, Byte.class)) {
                afterValue = Byte.valueOf(optionValue);
            } else if (isIn(type, short.class, Short.class)) {
                afterValue = Short.valueOf(optionValue);
            } else if (isIn(type, short.class, String.class)) {
                afterValue = optionValue;
            } else {
                return ExitStatus.failure(-1, format("Options[%s] type[%s] was unsupported.", optionName, type.getSimpleName()));
            }
        } catch (Throwable t) {
            return ExitStatus.failure(-1, format("Cannot cast option value[%s] to type[%s].", optionValue, type.getSimpleName()));
        }

        // 验证选项值是否合法
        String validateError = validateOptionValue(optionAnnotation.name(), afterValue);
        if (validateError != null) {
            return ExitStatus.failure(-1, validateError);
        }

        // 写入新值
        FieldUtils.writeStaticField(field, afterValue);

        // FIXME hack for ongl strict
        // 特殊处理strict选项，同步更新ONGL的strict设置
        if (field.getName().equals("strict")) {
            GlobalOptions.updateOnglStrict(Boolean.valueOf(optionValue));
            logger.info("update ongl strict to: {}", optionValue);
        }

        // 创建修改结果对象
        ChangeResultVO changeResultVO = new ChangeResultVO(optionAnnotation.name(), beforeValue, afterValue);
        process.appendResult(new OptionsModel(changeResultVO));
        return ExitStatus.success();
    }

    /**
     * 验证选项值是否合法
     * @param optionName 选项名称
     * @param optionValue 选项值
     * @return 如果验证失败返回错误信息，成功返回null
     */
    static String validateOptionValue(String optionName, Object optionValue) {
        // object-size-limit必须大于0
        if ("object-size-limit".equals(optionName)
                && optionValue instanceof Integer
                && ((Integer) optionValue).intValue() <= 0) {
            return "options[object-size-limit] must be greater than 0.";
        }
        return null;
    }


    /**
     * 判断当前动作是否需要展示整个options
     * @return 如果选项名和选项值都为空，返回true
     */
    private boolean isShow() {
        return StringUtils.isBlank(optionName) && StringUtils.isBlank(optionValue);
    }


    /**
     * 判断当前动作是否需要展示某个Name的值
     * @return 如果选项名不为空但选项值为空，返回true
     */
    private boolean isShowName() {
        return !StringUtils.isBlank(optionName) && StringUtils.isBlank(optionValue);
    }

    /**
     * 查找匹配的选项字段
     * @param optionNameMatcher 选项名匹配器
     * @return 匹配的字段集合
     */
    private Collection<Field> findOptionFields(Matcher<String> optionNameMatcher) {
        final Collection<Field> matchFields = new ArrayList<Field>();
        // 遍历GlobalOptions类的所有字段
        for (final Field optionField : FieldUtils.getAllFields(GlobalOptions.class)) {
            if (isMatchOptionAnnotation(optionField, optionNameMatcher)) {
                matchFields.add(optionField);
            }
        }
        return matchFields;
    }

    /**
     * 查找匹配的选项名称
     * @param optionNameMatcher 选项名匹配器
     * @return 匹配的选项名称集合
     */
    private Collection<String> findOptionNames(Matcher<String> optionNameMatcher) {
        final Collection<String> matchOptionNames = new ArrayList<String>();
        // 遍历GlobalOptions类的所有字段
        for (final Field optionField : FieldUtils.getAllFields(GlobalOptions.class)) {
            if (isMatchOptionAnnotation(optionField, optionNameMatcher)) {
                final Option optionAnnotation = optionField.getAnnotation(Option.class);
                matchOptionNames.add(optionAnnotation.name());
            }
        }
        return matchOptionNames;
    }

    /**
     * 判断字段是否匹配选项注解
     * @param optionField 字段
     * @param optionNameMatcher 选项名匹配器
     * @return 如果匹配返回true
     */
    private boolean isMatchOptionAnnotation(Field optionField, Matcher<String> optionNameMatcher) {
        // 检查是否有Option注解
        if (!optionField.isAnnotationPresent(Option.class)) {
            return false;
        }
        final Option optionAnnotation = optionField.getAnnotation(Option.class);
        return optionAnnotation != null && optionNameMatcher.matching(optionAnnotation.name());
    }

    /**
     * 将字段集合转换为选项VO集合
     * @param fields 字段集合
     * @return 选项VO集合
     * @throws IllegalAccessException 访问字段异常
     */
    private List<OptionVO> convertToOptionVOs(Collection<Field> fields) throws IllegalAccessException {
        List<OptionVO> list = new ArrayList<OptionVO>();
        for (Field field : fields) {
            list.add(convertToOptionVO(field));
        }
        return list;
    }

    /**
     * 将字段转换为选项VO
     * @param optionField 选项字段
     * @return 选项VO对象
     * @throws IllegalAccessException 访问字段异常
     */
    private OptionVO convertToOptionVO(Field optionField) throws IllegalAccessException {
        Option optionAnnotation = optionField.getAnnotation(Option.class);
        OptionVO optionVO = new OptionVO();
        // 设置选项级别
        optionVO.setLevel(optionAnnotation.level());
        // 设置选项名称
        optionVO.setName(optionAnnotation.name());
        // 设置选项摘要
        optionVO.setSummary(optionAnnotation.summary());
        // 设置选项描述
        optionVO.setDescription(optionAnnotation.description());
        // 设置选项类型
        optionVO.setType(optionField.getType().getSimpleName());
        // 设置选项值
        optionVO.setValue(""+optionField.get(null));
        return optionVO;
    }

}
