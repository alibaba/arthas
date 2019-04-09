package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.lang.management.*;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import static com.taobao.text.ui.Element.label;

/**
 * JVM info command
 *
 * @author vlinux on 15/6/6.
 */
@Name("jvm")
@Summary("Display the target JVM information")
@Description(Constants.WIKI + Constants.WIKI_HOME + "jvm")
public class JvmCommand extends AnnotatedCommand {

    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
    private final CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
    private final Collection<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final Collection<MemoryManagerMXBean> memoryManagerMXBeans = ManagementFactory.getMemoryManagerMXBeans();
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    //    private final Collection<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Override
    public void process(CommandProcess process) {
        RowAffect affect = new RowAffect();
        TableElement table = new TableElement(2, 5).leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("RUNTIME").style(Decoration.bold.bold()));
        drawRuntimeTable(table);
        table.row("", "");
        table.row(true, label("CLASS-LOADING").style(Decoration.bold.bold()));
        drawClassLoadingTable(table);
        table.row("", "");
        table.row(true, label("COMPILATION").style(Decoration.bold.bold()));
        drawCompilationTable(table);

        if (!garbageCollectorMXBeans.isEmpty()) {
            table.row("", "");
            table.row(true, label("GARBAGE-COLLECTORS").style(Decoration.bold.bold()));
            drawGarbageCollectorsTable(table);
        }

        if (!memoryManagerMXBeans.isEmpty()) {
            table.row("", "");
            table.row(true, label("MEMORY-MANAGERS").style(Decoration.bold.bold()));
            drawMemoryManagersTable(table);
        }

        table.row("", "");
        table.row(true, label("MEMORY").style(Decoration.bold.bold()));
        drawMemoryTable(table);
        table.row("", "");
        table.row(true, label("OPERATING-SYSTEM").style(Decoration.bold.bold()));
        drawOperatingSystemMXBeanTable(table);
        table.row("", "");
        table.row(true, label("THREAD").style(Decoration.bold.bold()));
        drawThreadTable(table);
        table.row("", "");
        table.row(true, label("FILE-DESCRIPTOR").style(Decoration.bold.bold()));
        drawFileDescriptorTable(table);
        process.write(RenderUtil.render(table, process.width()));
        process.write(affect.toString()).write("\n");
        process.end();
    }

    private void drawFileDescriptorTable(TableElement table) {
        table.row("MAX-FILE-DESCRIPTOR-COUNT", "" + invokeFileDescriptor(operatingSystemMXBean, "getMaxFileDescriptorCount"))
                .row("OPEN-FILE-DESCRIPTOR-COUNT", "" + invokeFileDescriptor(operatingSystemMXBean, "getOpenFileDescriptorCount"));
    }
    private long invokeFileDescriptor(OperatingSystemMXBean os, String name) {
        try {
            final Method method = os.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return (Long) method.invoke(os);
        } catch (Exception e) {
            return -1;
        }
    }
    private String toCol(Collection<String> strings) {
        final StringBuilder colSB = new StringBuilder();
        if (strings.isEmpty()) {
            colSB.append("[]");
        } else {
            for (String str : strings) {
                colSB.append(str).append("\n");
            }
        }
        return colSB.toString();
    }

    private String toCol(String... stringArray) {
        final StringBuilder colSB = new StringBuilder();
        if (null == stringArray
                || stringArray.length == 0) {
            colSB.append("[]");
        } else {
            for (String str : stringArray) {
                colSB.append(str).append("\n");
            }
        }
        return colSB.toString();
    }

    private Element drawRuntimeTable(TableElement table) {
        String bootClassPath = "";
        try {
            bootClassPath = runtimeMXBean.getBootClassPath();
        } catch (Exception e) {
            // under jdk9 will throw UnsupportedOperationException, ignore
        }
        table.row("MACHINE-NAME", runtimeMXBean.getName())
                .row("JVM-START-TIME", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(runtimeMXBean.getStartTime())))
                .row("MANAGEMENT-SPEC-VERSION", runtimeMXBean.getManagementSpecVersion())
                .row("SPEC-NAME", runtimeMXBean.getSpecName())
                .row("SPEC-VENDOR", runtimeMXBean.getSpecVendor())
                .row("SPEC-VERSION", runtimeMXBean.getSpecVersion())
                .row("VM-NAME", runtimeMXBean.getVmName())
                .row("VM-VENDOR", runtimeMXBean.getVmVendor())
                .row("VM-VERSION", runtimeMXBean.getVmVersion())
                .row("INPUT-ARGUMENTS", toCol(runtimeMXBean.getInputArguments()))
                .row("CLASS-PATH", runtimeMXBean.getClassPath())
                .row("BOOT-CLASS-PATH", bootClassPath)
                .row("LIBRARY-PATH", runtimeMXBean.getLibraryPath());

        return table;
    }

    private Element drawClassLoadingTable(TableElement table) {
        table.row("LOADED-CLASS-COUNT", "" + classLoadingMXBean.getLoadedClassCount())
                .row("TOTAL-LOADED-CLASS-COUNT", "" + classLoadingMXBean.getTotalLoadedClassCount())
                .row("UNLOADED-CLASS-COUNT", "" + classLoadingMXBean.getUnloadedClassCount())
                .row("IS-VERBOSE", "" + classLoadingMXBean.isVerbose());

        return table;
    }

    private Element drawCompilationTable(TableElement table) {
        table.row("NAME", compilationMXBean.getName());

        if (compilationMXBean.isCompilationTimeMonitoringSupported()) {
            table.row("TOTAL-COMPILE-TIME", compilationMXBean.getTotalCompilationTime() + "(ms)");

        }
        return table;
    }

    private Element drawGarbageCollectorsTable(TableElement table) {
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            table.row(garbageCollectorMXBean.getName() + "\n[count/time]",
                    garbageCollectorMXBean.getCollectionCount() + "/" + garbageCollectorMXBean.getCollectionTime() + "(ms)");
        }

        return table;
    }

    private Element drawMemoryManagersTable(TableElement table) {
        for (final MemoryManagerMXBean memoryManagerMXBean : memoryManagerMXBeans) {
            if (memoryManagerMXBean.isValid()) {
                final String name = memoryManagerMXBean.isValid()
                        ? memoryManagerMXBean.getName()
                        : memoryManagerMXBean.getName() + "(Invalid)";


                table.row(name, toCol(memoryManagerMXBean.getMemoryPoolNames()));
            }
        }

        return table;
    }

    private Element drawMemoryTable(TableElement table) {
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        table.row("HEAP-MEMORY-USAGE\n[committed/init/max/used]",
                formatMemoryByte(heapMemoryUsage.getCommitted())
                        + "/" + formatMemoryByte(heapMemoryUsage.getInit())
                        + "/" + formatMemoryByte(heapMemoryUsage.getMax())
                        + "/" + formatMemoryByte(heapMemoryUsage.getUsed())
        );
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        table.row("NO-HEAP-MEMORY-USAGE\n[committed/init/max/used]",
                formatMemoryByte(nonHeapMemoryUsage.getCommitted())
                        + "/" + formatMemoryByte(nonHeapMemoryUsage.getInit())
                        + "/" + formatMemoryByte(nonHeapMemoryUsage.getMax())
                        + "/" + formatMemoryByte(nonHeapMemoryUsage.getUsed())
        );

        table.row("PENDING-FINALIZE-COUNT", "" + memoryMXBean.getObjectPendingFinalizationCount());
        return table;
    }
    private String formatMemoryByte(long bytes){
        return String.format("%s(%s)",bytes, StringUtils.humanReadableByteCount(bytes));
    }

    private Element drawOperatingSystemMXBeanTable(TableElement table) {
        table.row("OS", operatingSystemMXBean.getName()).row("ARCH", operatingSystemMXBean.getArch())
                .row("PROCESSORS-COUNT", "" + operatingSystemMXBean.getAvailableProcessors())
                .row("LOAD-AVERAGE", "" + operatingSystemMXBean.getSystemLoadAverage())
                .row("VERSION", operatingSystemMXBean.getVersion());
        return table;
    }

    private Element drawThreadTable(TableElement table) {
        table.row("COUNT", "" + threadMXBean.getThreadCount())
                .row("DAEMON-COUNT", "" + threadMXBean.getDaemonThreadCount())
                .row("PEAK-COUNT", "" + threadMXBean.getPeakThreadCount())
                .row("STARTED-COUNT", "" + threadMXBean.getTotalStartedThreadCount())
                .row("DEADLOCK-COUNT","" + getDeadlockedThreadsCount(threadMXBean));
        return table;
    }
    private int getDeadlockedThreadsCount(ThreadMXBean threads) {
        final long[] ids = threads.findDeadlockedThreads();
        if (ids == null) {
            return 0;
        } else {
            return ids.length;
        }
    }
}
