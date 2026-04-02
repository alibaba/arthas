package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.SystemPropertyModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 系统属性命令类
 *
 * 用于查看和修改Java系统属性（System Properties）。
 * 系统属性是JVM和应用程序使用的配置参数，可以通过该命令进行查看和动态修改。
 *
 * 使用示例：
 * - sysprop: 显示所有系统属性
 * - sysprop file.encoding: 查看指定属性的值
 * - sysprop production.mode true: 修改指定属性的值
 *
 * @author ralf0131 2017-01-09 14:03.
 */
@Name("sysprop")
@Summary("Display and change the system properties.")
@Description(Constants.EXAMPLE + "  sysprop\n"+ "  sysprop file.encoding\n" + "  sysprop production.mode true\n" +
        Constants.WIKI + Constants.WIKI_HOME + "sysprop")
public class SystemPropertyCommand extends AnnotatedCommand {

    /** 属性名称 */
    private String propertyName;
    /** 属性值 */
    private String propertyValue;

    /**
     * 设置属性名称
     *
     * @param propertyName 属性名称，例如 "file.encoding"
     */
    @Argument(index = 0, argName = "property-name", required = false)
    @Description("property name")
    public void setOptionName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * 设置属性值
     *
     * @param propertyValue 属性值，如果提供则表示要修改该属性
     */
    @Argument(index = 1, argName = "property-value", required = false)
    @Description("property value")
    public void setOptionValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    /**
     * 处理命令执行逻辑
     *
     * 根据参数不同执行不同操作：
     * 1. 无参数：显示所有系统属性
     * 2. 只有属性名：显示指定属性的值
     * 3. 属性名和属性值：修改系统属性的值
     *
     * @param process 命令处理进程对象，用于输出结果和控制命令流程
     */
    @Override
    public void process(CommandProcess process) {
        try {

            // 如果既没有指定属性名，也没有指定属性值，则显示所有系统属性
            if (StringUtils.isBlank(propertyName) && StringUtils.isBlank(propertyValue)) {
                // 显示所有系统属性
                process.appendResult(new SystemPropertyModel(System.getProperties()));
            } else if (StringUtils.isBlank(propertyValue)) {
                // 只指定了属性名，查看指定的系统属性
                String value = System.getProperty(propertyName);
                if (value == null) {
                    // 属性不存在，返回错误信息
                    process.end(1, "There is no property with the key " + propertyName);
                    return;
                } else {
                    // 返回该属性的值
                    process.appendResult(new SystemPropertyModel(propertyName, value));
                }
            } else {
                // 同时指定了属性名和属性值，修改系统属性
                System.setProperty(propertyName, propertyValue);
                // 返回成功消息
                process.appendResult(new MessageModel("Successfully changed the system property."));
                // 返回修改后的属性值
                process.appendResult(new SystemPropertyModel(propertyName, System.getProperty(propertyName)));
            }
            // 结束命令处理
            process.end();
        } catch (Throwable t) {
            // 捕获异常并返回错误信息
            process.end(-1, "Error during setting system property: " + t.getMessage());
        }
    }

    /**
     * 命令自动补全功能
     *
     * 首先尝试使用sysprop命令的作用域进行补全。
     * 如果补全失败，则委托给父类处理。
     *
     * @param completion 补全对象，用于提供补全建议
     */
    @Override
    public void complete(Completion completion) {
        // 从所有系统属性名称中查找匹配的补全建议
        CompletionUtils.complete(completion, System.getProperties().stringPropertyNames());
    }
}
