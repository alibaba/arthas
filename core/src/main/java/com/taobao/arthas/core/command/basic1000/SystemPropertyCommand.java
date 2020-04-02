package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.result.MessageResult;
import com.taobao.arthas.core.command.result.StatusResult;
import com.taobao.arthas.core.command.result.PropertyResult;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.Properties;

import static com.taobao.text.ui.Element.label;

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
        int status = 0;
        try {

            if (StringUtils.isBlank(propertyName) && StringUtils.isBlank(propertyValue)) {
                // show all system properties
                //process.write(renderSystemProperties(System.getProperties(), process.width()));
                process.appendResult(new PropertyResult(System.getProperties()));
            } else if (StringUtils.isBlank(propertyValue)) {
                // view the specified system property
                String value = System.getProperty(propertyName);
                if (value == null) {
                    //process.write("There is no property with the key " + propertyName + ".\n");
                    process.appendResult(new StatusResult(status, "There is no property with the key " + propertyName));
                } else {
                    //process.write(propertyName + "=" + value + "\n");
                    process.appendResult(new PropertyResult(propertyName, value));
                }
            } else {
                // change system property
                System.setProperty(propertyName, propertyValue);
                //process.write("Successfully changed the system property.\n");
                //process.write(propertyName + "=" + System.getProperty(propertyName) + "\n");
                process.appendResult(new MessageResult("Successfully changed the system property."));
                process.appendResult(new PropertyResult(propertyName, System.getProperty(propertyName)));
            }
        } catch (Throwable t) {
            //process.write("Error during setting system property: " + t.getMessage() + "\n");
            process.appendResult(new StatusResult(status, "Error during setting system property: " + t.getMessage()));
            status = 1;
        } finally {
            process.end(status);
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
