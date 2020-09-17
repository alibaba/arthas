package com.taobao.arthas.core.command.monitor200;


import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.JavaVersionUtils;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.PerfCounterModel;
import com.taobao.arthas.core.command.model.PerfCounterVO;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import sun.management.counter.Counter;
import sun.management.counter.perf.PerfInstrumentation;

/**
 * @see sun.misc.Perf
 * @see sun.management.counter.perf.PerfInstrumentation
 * @author hengyunabc 2020-02-16
 */
@Name("perfcounter")
@Summary("Display the perf counter information.")
@Description("\nExamples:\n" +
        "  perfcounter\n" +
        "  perfcounter -d\n" +
        Constants.WIKI + Constants.WIKI_HOME + "perfcounter")
public class PerfCounterCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(PerfCounterCommand.class);
    private static Object perfObject;
    private static Method attachMethod;

    private boolean details;

    @Option(shortName = "d", longName = "details", flag = true)
    @Description("print all perf counter details")
    public void setDetails(boolean details) {
        this.details = details;
    }

    @Override
    public void process(CommandProcess process) {
        List<Counter> perfCounters = getPerfCounters();
        if (perfCounters.isEmpty()) {
            process.end(1,
                    "please check arthas log. if java version >=9 , try to add jvm options when start your process: "
                            + "--add-opens java.base/jdk.internal.perf=ALL-UNNAMED "
                            + "--add-exports java.base/jdk.internal.perf=ALL-UNNAMED");
            return;
        }

        List<PerfCounterVO> perfCounterVOs = new ArrayList<PerfCounterVO>();
        for (Counter counter : perfCounters) {
            PerfCounterVO perfCounterVO = new PerfCounterVO(counter.getName(), counter.getValue());
            if (details) {
                perfCounterVO.setUnits(counter.getUnits().toString());
                perfCounterVO.setVariability(counter.getVariability().toString());
            }
            perfCounterVOs.add(perfCounterVO);
        }
        process.appendResult(new PerfCounterModel(perfCounterVOs, details));
        process.end();
    }

    private static List<Counter> getPerfCounters() {

        /**
         * <pre>
         * Perf p = Perf.getPerf();
         * ByteBuffer buffer = p.attach(pid, "r");
         * </pre>
         */
        try {
            if (perfObject == null) {
                // jdk8
                String perfClassName = "sun.misc.Perf";
                // jdk 11
                if (!JavaVersionUtils.isLessThanJava9()) {
                    perfClassName = "jdk.internal.perf.Perf";
                }

                Class<?> perfClass = ClassLoader.getSystemClassLoader().loadClass(perfClassName);
                Method getPerfMethod = perfClass.getDeclaredMethod("getPerf");
                perfObject = getPerfMethod.invoke(null);
            }

            if (attachMethod == null) {
                attachMethod = perfObject.getClass().getDeclaredMethod("attach",
                        new Class<?>[] { int.class, String.class });
            }

            ByteBuffer buffer = (ByteBuffer) attachMethod.invoke(perfObject,
                    new Object[] { (int) PidUtils.currentLongPid(), "r" });

            PerfInstrumentation perfInstrumentation = new PerfInstrumentation(buffer);
            return perfInstrumentation.getAllCounters();
        } catch (Throwable e) {
            logger.error("get perf counter error", e);
        }
        return Collections.emptyList();
    }
}
