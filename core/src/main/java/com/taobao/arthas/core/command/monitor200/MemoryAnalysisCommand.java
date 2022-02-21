package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassHistogramModel;
import com.taobao.arthas.core.command.model.ClassHistogramVO;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.*;

import javax.management.*;
import javax.xml.transform.Source;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author:
 * @Date: 2021/7/16 10:05
 * @Copyright: 2019 www.lenovo.com Inc. All rights reserved.
 */

@Name("ma")
@Summary("memory analysis command you can get class histogram info doing gc and so on.")
@Description(Constants.EXAMPLE +
        "  ma\n" +
        "  ma --live\n" +
        "  ma -n 10\n" +
        "  ma --live -n 10\n" +
        "  ma -t cs\n" +
        "  ma -t cl\n" +
        "  ma -t cls\n" +
        "  ma -t ch\n" +
        "  ma -t hi\n" +
        "  ma -t vm\n" +
        "\nRequirements: JDK 8 or higher")
public class MemoryAnalysisCommand extends AnnotatedCommand {

    public static final String BASE_CLASS_PRE = "[B[C[D[F[I[J[Z";

    private static final Logger logger = LoggerFactory.getLogger(MemoryAnalysisCommand.class);

    private static final String DIAGNOSTIC_COMMAND_CLASS = "com.sun.management:type=DiagnosticCommand";

    private static Map<String, MemoryAnalysisUtils> typeAndMethodMaps = new HashMap<>(16);

    static {
        //class histogram info
        typeAndMethodMaps.put("ch", new MemoryAnalysisUtils(new String[]{"-all"}, "gcClassHistogram"));

        //class histogram states info
        typeAndMethodMaps.put("cs", new MemoryAnalysisUtils(new String[]{"-all"}, "gcClassStats"));

        //class loaders  info
        typeAndMethodMaps.put("cl", new MemoryAnalysisUtils(new String[]{"fold"}, "vmClassloaders"));

        //class loaders states info
        typeAndMethodMaps.put("cls", new MemoryAnalysisUtils(null, "vmClassloaderStats"));

        //class hierarchy info
        typeAndMethodMaps.put("ch", new MemoryAnalysisUtils(new String[]{"-i"}, "vmClassHierarchy"));

        //class gc heap info
        typeAndMethodMaps.put("hi", new MemoryAnalysisUtils(null, "gcHeapInfo"));

        // vm info
        typeAndMethodMaps.put("vm", new MemoryAnalysisUtils(null, "vmInfo"));

    }

    private int numberOfLimit = 100;

    private boolean live;

    private String classPattern;

    private String type;

    @Argument(index = 0, argName = "class-pattern", required = false)
    @Description("Path and classname of Pattern Matching")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Option(shortName = "t", longName = "type")
    @Description("command handler type ")
    public void setType(String type) {
        this.type = StringUtils.isEmpty(type) ? "ch" : type;
    }

    @Option(shortName = "p", longName = "params", flag = true)
    @Description("Inspect only live objects; if not specified, all objects are inspected, including unreachable objects.")
    public void setLive(boolean params) {
        this.live = params;
    }

    @Option(shortName = "n", longName = "limits")
    @Description("Maximum number of class statistics (default is 100)")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Override
    public void process(CommandProcess process) {
        try {
            processCommand(live, type, process);
            process.end();
        } catch (Throwable t) {
            String errorMsg = "Failed to do class analysis: " + t.getMessage();
            logger.error(errorMsg, t);
            process.end(-1, errorMsg);
        }
    }

    public void processCommand(boolean live, String type, CommandProcess process) throws Exception {
        final MemoryAnalysisUtils memoryAnalysisUtils = typeAndMethodMaps.get(type);
        if (memoryAnalysisUtils == null) {
            process.end(-1, "type is undefine");
            return;
        }
        doInvoke(memoryAnalysisUtils.getOperationName(), memoryAnalysisUtils.getArgs(), process);
    }


    private void doInvoke(String operationName, String[] args, CommandProcess process) throws MalformedObjectNameException, MBeanException,
            InstanceNotFoundException, ReflectionException {
        ObjectName mbeanName = new ObjectName(DIAGNOSTIC_COMMAND_CLASS);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        Object response = mBeanServer.invoke(mbeanName, operationName, new Object[]{args},
                new String[]{String[].class.getName()});
        doBuild(operationName, response, numberOfLimit, process);
    }

    private void doBuild(String operationName, Object responses, int numberOfLimit, CommandProcess process) {
        switch (operationName) {
            case "gcClassHistogram":
                buildClassHistogram(responses, numberOfLimit, process);
                break;
            case "vmClassloaders":
                buildClassLoader(responses, process);
                break;
            case "vmClassloaderStats":
                buildClassLoaderStats(responses, process);
                break;
            case "vmClassHierarchy":
                buildClassHierarchy(responses, process);
            case "gcHeapInfo":
                buildHeapInfo(responses, process);
                break;
            case "vmInfo":
                buildVMInfo(responses, process);
                break;
            default:
                break;
        }

    }

    private void buildVMInfo(Object responses, CommandProcess process) {
        process.appendResult(new MessageModel(responses.toString()));
    }

    private void buildHeapInfo(Object responses, CommandProcess process) {
        process.appendResult(new MessageModel(responses.toString()));
    }

    private void buildClassHierarchy(Object responses, CommandProcess process) {
        process.appendResult(new MessageModel(responses.toString()));
    }

    private void buildClassLoaderStats(Object response, CommandProcess process) {
        process.appendResult(new MessageModel(response.toString()));
    }

    private void buildClassLoader(Object response, CommandProcess process) {
        process.appendResult(new MessageModel(response.toString()));
    }

    private void buildClassHistogram(Object response, int numberOfLimit, CommandProcess process) {
        final String[] responses = ((String) response).split("\n");
        List<ClassHistogramVO> classHistogramVOS = new ArrayList<>(numberOfLimit);
        int count = 0;
        for (int i = 3; i < responses.length - 1; i++) {
            final String line = responses[i].trim().replaceAll(" +", "￥");
            final String[] lineFormat = line.split("￥");
            final String className = lineFormat[3];
            if (needBuildBean(className)) {
                ClassHistogramVO classHistogramVO = new ClassHistogramVO();
                classHistogramVO.setNum(count);
                classHistogramVO.setInstances(Long.parseLong(lineFormat[1]));
                classHistogramVO.setBytes(Long.parseLong(lineFormat[2]));
                classHistogramVO.setClassName(className);
                count++;
                classHistogramVOS.add(classHistogramVO);
                i = count >= numberOfLimit ? responses.length : i;
            }
        }
        final String lastLine = responses[responses.length - 1];
        final String lastLineData = lastLine.replaceAll(" +", "￥");
        final String[] lastLineDataFor = lastLineData.split("￥");
        ClassHistogramModel model = new ClassHistogramModel();
        model.setClassHistogramVOList(classHistogramVOS);
        model.setTotalInstances(Long.parseLong(lastLineDataFor[1]));
        model.setTotalBytes(Long.parseLong(lastLineDataFor[2]));
        process.write(new ObjectView(model, 2).draw());
        process.appendResult(model);
    }


    private boolean needBuildBean(String className) {
        return filterBaseClass(className) && needClassPattern(className);
    }

    private boolean needClassPattern(String className) {
        return StringUtils.isEmpty(classPattern) || doClassPattern(className);
    }

    private boolean doClassPattern(String className) {
        return className.contains(classPattern.trim().substring(0, classPattern.length() - 1));
    }

    private boolean filterBaseClass(String prefix) {
        return !BASE_CLASS_PRE.contains(prefix);
    }

}
