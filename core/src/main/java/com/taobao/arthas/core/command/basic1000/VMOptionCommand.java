package com.taobao.arthas.core.command.basic1000;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.JMX;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ChangeResultVO;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.VMOptionModel;
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
 * vmoption command
 * 
 * @author hengyunabc 2019-09-02
 *
 */
// @formatter:off
@Name("vmoption")
@Summary("Display, and update the vm diagnostic options.")
@Description("\nExamples:\n" + 
        "  vmoption\n" + 
        "  vmoption PrintGC\n" + 
        "  vmoption PrintGC true\n" + 
        "  vmoption PrintGCDetails true\n" + 
        Constants.WIKI + Constants.WIKI_HOME + "vmoption")
//@formatter:on
public class VMOptionCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(VMOptionCommand.class);

    private static final String HOTSPOT_DIAGNOSTIC_MXBEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    private static HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() throws Exception {
        // Primary path: direct ManagementFactory lookup
        try {
            HotSpotDiagnosticMXBean bean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            if (bean != null) {
                return bean;
            }
        } catch (Throwable t) {
            // fall through to JMX fallback
        }
        // Fallback: look up via MBeanServer (better module compatibility on JDK 9+)
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        return JMX.newMXBeanProxy(server,
                new ObjectName(HOTSPOT_DIAGNOSTIC_MXBEAN_NAME),
                HotSpotDiagnosticMXBean.class);
    }

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
            HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = getHotSpotDiagnosticMXBean();

            if (StringUtils.isBlank(name) && StringUtils.isBlank(value)) {
                // show all options
                process.appendResult(new VMOptionModel(hotSpotDiagnosticMXBean.getDiagnosticOptions()));
            } else if (StringUtils.isBlank(value)) {
                // view the specified option
                VMOption option = hotSpotDiagnosticMXBean.getVMOption(name);
                if (option == null) {
                    process.end(-1, "In order to change the system properties, you must specify the property value.");
                    return;
                } else {
                    process.appendResult(new VMOptionModel(Collections.singletonList(option)));
                }
            } else {
                VMOption vmOption = hotSpotDiagnosticMXBean.getVMOption(name);
                String originValue = vmOption.getValue();

                // change vm option
                hotSpotDiagnosticMXBean.setVMOption(name, value);
                process.appendResult(new MessageModel("Successfully updated the vm option."));
                process.appendResult(new VMOptionModel(new ChangeResultVO(name, originValue,
                        hotSpotDiagnosticMXBean.getVMOption(name).getValue())));
            }
            process.end();
        } catch (Throwable t) {
            logger.error("Error during setting vm option", t);
            String msg = t.getMessage();
            String hint = "";
            if (msg != null && (msg.contains("sun.management") || msg.contains("sun/management"))) {
                hint = " (JDK module access issue: try adding"
                        + " --add-opens java.management/sun.management=ALL-UNNAMED"
                        + " to the target JVM startup arguments)";
            }
            process.end(-1, "Error during setting vm option: " + msg + hint);
        }
    }

    @Override
    public void complete(Completion completion) {
        try {
            HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = getHotSpotDiagnosticMXBean();
            List<VMOption> diagnosticOptions = hotSpotDiagnosticMXBean.getDiagnosticOptions();
            List<String> names = new ArrayList<String>(diagnosticOptions.size());
            for (VMOption option : diagnosticOptions) {
                names.add(option.getName());
            }
            CompletionUtils.complete(completion, names);
        } catch (Throwable t) {
            logger.error("Error during completing vmoption", t);
        }
    }
}
