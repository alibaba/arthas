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
 * @author ralf0131 2017-01-09 14:03.
 */
@Name("sysprop")
@Summary("Display, and change the system properties.")
@Description(Constants.EXAMPLE + "  sysprop\n"+ "  sysprop file.encoding\n" + "  sysprop production.mode true\n" +
        Constants.WIKI + Constants.WIKI_HOME + "sysprop")
public class SystemPropertyCommand extends AnnotatedCommand {

    private String propertyName;
    private String propertyValue;

    @Argument(index = 0, argName = "property-name", required = false)
    @Description("property name")
    public void setOptionName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Argument(index = 1, argName = "property-value", required = false)
    @Description("property value")
    public void setOptionValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    @Override
    public void process(CommandProcess process) {
        try {

            if (StringUtils.isBlank(propertyName) && StringUtils.isBlank(propertyValue)) {
                // show all system properties
                process.appendResult(new SystemPropertyModel(System.getProperties()));
            } else if (StringUtils.isBlank(propertyValue)) {
                // view the specified system property
                String value = System.getProperty(propertyName);
                if (value == null) {
                    process.end(1, "There is no property with the key " + propertyName);
                    return;
                } else {
                    process.appendResult(new SystemPropertyModel(propertyName, value));
                }
            } else {
                // change system property
                System.setProperty(propertyName, propertyValue);
                process.appendResult(new MessageModel("Successfully changed the system property."));
                process.appendResult(new SystemPropertyModel(propertyName, System.getProperty(propertyName)));
            }
            process.end();
        } catch (Throwable t) {
            process.end(-1, "Error during setting system property: " + t.getMessage());
        }
    }

    /**
     * First, try to complete with the sysprop command scope.
     * If completion is failed, delegates to super class.
     * @param completion the completion object
     */
    @Override
    public void complete(Completion completion) {
        CompletionUtils.complete(completion, System.getProperties().stringPropertyNames());
    }
}
