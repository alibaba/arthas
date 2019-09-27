package com.taobao.arthas.core.command.basic1000;

import static com.taobao.text.ui.Element.label;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.logger.Logger;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

/**
 * vmoption command
 * 
 * @author hengyunabc 2019-09-02
 *
 */
@Name("vmoption")
@Summary("Display, and update the vm diagnostic options.")
@Description("\nExamples:\n" + "  vmoption\n" + "  vmoption PrintGCDetails\n" + "  vmoption PrintGCDetails true\n"
                + Constants.WIKI + Constants.WIKI_HOME + "vmoption")
public class VMOptionCommand extends AnnotatedCommand {
    private static final Logger logger = LogUtil.getArthasLogger();

    private String name;
    private String value;

    @Argument(index = 0, argName = "name", required = false)
    @Description("VMOption name")
    public void setOptionName(String name) {
        this.name = name;
    }

    @Argument(index = 1, argName = "value", required = false)
    @Description("VMOption value")
    public void setOptionValue(String value) {
        this.value = value;
    }

    @Override
    public void process(CommandProcess process) {
        run(process, name, value);
    }

    private static void run(CommandProcess process, String name, String value) {
        try {
            HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory
                            .getPlatformMXBean(HotSpotDiagnosticMXBean.class);

            if (StringUtils.isBlank(name) && StringUtils.isBlank(value)) {
                // show all options
                process.write(renderVMOptions(hotSpotDiagnosticMXBean.getDiagnosticOptions(), process.width()));
            } else if (StringUtils.isBlank(value)) {
                // view the specified option
                VMOption option = hotSpotDiagnosticMXBean.getVMOption(name);
                if (option == null) {
                    process.write("In order to change the system properties, you must specify the property value.\n");
                } else {
                    process.write(renderVMOptions(Arrays.asList(option), process.width()));
                }
            } else {
                // change vm option
                hotSpotDiagnosticMXBean.setVMOption(name, value);
                process.write("Successfully updated the vm option.\n");
                process.write(name + "=" + hotSpotDiagnosticMXBean.getVMOption(name).getValue() + "\n");
            }
        } catch (Throwable t) {
            process.write("Error during setting vm option: " + t.getMessage() + "\n");
            logger.error("arthas", "Error during setting vm option", t);
        } finally {
            process.end();
        }

    }

    private static String renderVMOptions(List<VMOption> diagnosticOptions, int width) {
        TableElement table = new TableElement(1, 1, 1, 1).leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("KEY").style(Decoration.bold.bold()), label("VALUE").style(Decoration.bold.bold()),
                        label("ORIGIN").style(Decoration.bold.bold()),
                        label("WRITEABLE").style(Decoration.bold.bold()));

        for (VMOption option : diagnosticOptions) {
            table.row(option.getName(), option.getValue(), "" + option.getOrigin(), "" + option.isWriteable());
        }

        return RenderUtil.render(table, width);
    }

    @Override
    public void complete(Completion completion) {
        HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory
                        .getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        List<VMOption> diagnosticOptions = hotSpotDiagnosticMXBean.getDiagnosticOptions();
        List<String> names = new ArrayList<String>(diagnosticOptions.size());
        for (VMOption option : diagnosticOptions) {
            names.add(option.getName());
        }
        CompletionUtils.complete(completion, names);
    }
}
