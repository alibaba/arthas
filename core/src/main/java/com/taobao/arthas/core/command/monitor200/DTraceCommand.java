package com.taobao.arthas.core.command.monitor200;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.tools.jdi.SocketAttachingConnector;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.logger.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Date: 2019/5/19
 *
 * @author xuzhiyi
 */
@Name("dtrace")
@Summary("Dtrace the execution time of specified method invocation.")
@Description(value = "\nExamples:\n" +
                     "  dtrace demo.MathGame run -p demo.*\n" +
                     "  dtrace demo.MathGame run -p java.* demo.*\n" +
                     "  dtrace --host 127.0.0.1 --port 8000 demo.MathGame run -p java.*\n" +
                     "  dtrace --timeout 30000 demo.MathGame run -p java.* demo.*\n" +
                     Constants.WIKI + Constants.WIKI_HOME + "dtrace")
public class DTraceCommand extends AnnotatedCommand {

    private static final Logger logger = LogUtil.getArthasLogger();

    private String className;
    private String methodName;
    private List<String> pathPatterns;
    private int port = 8000;
    private String host = "127.0.0.1";
    private int timeout = 10000;
    private int stepTimeout = 3000;
    private int connectTimeout = 5000;

    @Argument(argName = "class-name", index = 0)
    @Description("The class name")
    public void setClassName(String className) {
        this.className = className;
    }

    @Argument(argName = "method-name", index = 1)
    @Description("The method name")
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Option(shortName = "p", longName = "path", acceptMultipleValues = true)
    @Description("path tracing pattern")
    public void setPathPatterns(List<String> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    @Option(longName = "timeout")
    @Description("Timeout wait for the breakpoint.")
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Option(longName = "connect-timeout")
    @Description("Timeout connect to the target vm.")
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Option(longName = "step-timeout")
    @Description("Timeout connect for every step.")
    public void setStepTimeout(int stepTimeout) {
        this.stepTimeout = stepTimeout;
    }

    @Option(longName = "host")
    @Description("The debug host, default 127.0.0.1")
    public void setHost(String host) {
        this.host = host;
    }

    @Option(longName = "port")
    @Description("The debug port.")
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void process(CommandProcess process) {
        VirtualMachine vm;
        try {
            vm = connect();
        } catch (Exception e) {
            logger.warn("Connect to the target vm fail, host:" + host + " port:" + port, e);
            process.write("Connect to the target vm fail, host:" + host + " port:" + port + "\n");
            process.end();
            return;
        }

        try {
            EventRequestManager requestManager = vm.eventRequestManager();
            List<ReferenceType> referenceTypes = vm.classesByName(className);
            if (referenceTypes == null || referenceTypes.size() == 0) {
                process.write("Class + " + className + " not found in target vm.\n");
                process.end();
                return;
            }
            Method targetMethod = null;
            for (Method m : referenceTypes.get(0).methods()) {
                //todo args match
                if (methodName.equals(m.name())) {
                    targetMethod = m;
                    break;
                }
            }
            if (targetMethod == null) {
                process.write("Can not find target method.\n");
                return;
            }
            BreakpointRequest breakpointRequest = requestManager.createBreakpointRequest(targetMethod.location());
            breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            breakpointRequest.enable();

            boolean traced = false;
            while (!traced) {
                EventSet eventSet = vm.eventQueue().remove(timeout);
                if (eventSet == null) {
                    process.write("Wait for breakpoint timeout.\n");
                    return;
                }
                EventIterator eventIterator = eventSet.eventIterator();
                while (eventIterator.hasNext()) {
                    Event event = eventIterator.nextEvent();
                    if (event instanceof BreakpointEvent) {
                        traced = true;
                        BreakpointEvent breakpointEvent = (BreakpointEvent) event;
                        ThreadReference threadReference = breakpointEvent.thread();
                        process.write("Reach method: " + targetMethod.toString() + "\n");
                        process.write("Thread: " + threadReference.name() + "\n");
                        breakpointRequest.disable();
                        trace(vm, threadReference, eventSet, process);
                        break;
                    }
                    // some other method event may comes during the interval, ignore.
                }
            }
        } catch (Throwable e) {
            logger.warn("Dtrace error", e);
            process.write("Dtrace error, please check arthas.log\n");
        } finally {
            if (vm != null) {
                vm.resume();
                vm.dispose();
            }
            process.end();
        }
    }

    private VirtualMachine connect()
        throws IOException, IllegalConnectorArgumentsException {

        List<AttachingConnector> allConnectors = Bootstrap.virtualMachineManager().attachingConnectors();

        SocketAttachingConnector socketAttachingConnector = null;
        for (Connector connector : allConnectors) {
            logger.warn("Find connector: " + connector.getClass());
            if (connector instanceof SocketAttachingConnector) {
                socketAttachingConnector = (SocketAttachingConnector) connector;
                break;
            }
        }
        if (socketAttachingConnector == null) {
            throw new IllegalArgumentException("Can not find SocketAttachingConnector");
        }

        Map<String, Connector.Argument> arguments1 = socketAttachingConnector.defaultArguments();
        Connector.Argument hostArg = arguments1.get("hostname");
        Connector.Argument portArg = arguments1.get("port");
        Connector.Argument timeArg = arguments1.get("timeout");
        hostArg.setValue(host);
        portArg.setValue(String.valueOf(port));
        timeArg.setValue(String.valueOf(connectTimeout));

        VirtualMachine vm = socketAttachingConnector.attach(arguments1);
        logger.info("Connect success.");
        logger.info(vm.description());
        return vm;
    }

    private void trace(VirtualMachine vm, ThreadReference threadReference,
                       EventSet beakPointEventSet, CommandProcess process) {

        List<EventRequest> eventRequests = createEvent(vm.eventRequestManager(), threadReference);

        enableAll(eventRequests);

        beakPointEventSet.resume();

        TraceEntity traceEntity = new TraceEntity();
        boolean isBreak = false;
        try {
            while (!isBreak) {
                //todo interrupt
                EventSet eventSet = vm.eventQueue().remove(stepTimeout);
                if (eventSet == null) {
                    logger.warn("step timeout---->>>>>");
                    break;
                }
                EventIterator eventIterator = eventSet.eventIterator();
                while (eventIterator.hasNext() && !isBreak) {
                    Event event = eventIterator.nextEvent();
                    if (event instanceof MethodEntryEvent) {
                        MethodEntryEvent methodEntryEvent = (MethodEntryEvent) event;
                        Method method = methodEntryEvent.method();
                        String methodName = method.name();
                        String className = method.declaringType().name();
                        if (ignore(className, methodName)) {
                            continue;
                        }
                        traceEntity.view.begin(className + ":" + methodName + "() #" + method.location().lineNumber());
                        traceEntity.deep++;
                    } else if (event instanceof MethodExitEvent) {
                        MethodExitEvent methodExitEvent = (MethodExitEvent) event;
                        Method method = methodExitEvent.method();
                        String methodName = method.name();
                        String className = method.declaringType().name();
                        if (ignore(className, methodName)) {
                            continue;
                        }
                        //todo Fix recursive method
                        if (this.methodName.equals(methodName) && this.className.equals(className)) {
                            isBreak = true;
                            logger.warn("break by method exit ---->>>>>");
                        } else {
                            if (traceEntity.view.isRoot()) {
                                isBreak = true;
                                logger.warn("break by root tree ---->>>>>");
                            } else {
                                traceEntity.view.end();
                            }
                        }
                    } else if (event instanceof ExceptionEvent) {
                        isBreak = true;
                        traceEntity.view.end("throws Exception");
                        logger.warn("break by exception ---->>>>>");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("trace error", e);
        } finally {
            disableAll(eventRequests);
        }
        process.write(traceEntity.view.draw() + "\n");
    }

    private List<EventRequest> createEvent(EventRequestManager eventRequestManager, ThreadReference threadReference) {
        List<EventRequest> eventRequests = new ArrayList<EventRequest>();
        for (String path : pathPatterns) {
            MethodEntryRequest methodEntryRequest = eventRequestManager.createMethodEntryRequest();
            methodEntryRequest.addThreadFilter(threadReference);
            methodEntryRequest.addClassFilter(path);
            methodEntryRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            eventRequests.add(methodEntryRequest);

            MethodExitRequest methodExitRequest = eventRequestManager.createMethodExitRequest();
            methodExitRequest.addThreadFilter(threadReference);
            methodExitRequest.addClassFilter(path);
            methodExitRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            eventRequests.add(methodExitRequest);

            //todo config exception strategy
            ExceptionRequest exceptionRequest = eventRequestManager.createExceptionRequest(null, true, true);
            exceptionRequest.addThreadFilter(threadReference);
            exceptionRequest.addClassFilter(path);
            exceptionRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            eventRequests.add(exceptionRequest);
        }
        return eventRequests;
    }

    private void enableAll(List<EventRequest> eventRequests) {
        for (EventRequest eventRequest : eventRequests) {
            eventRequest.enable();
        }
    }

    private void disableAll(List<EventRequest> eventRequests) {
        for (EventRequest eventRequest : eventRequests) {
            eventRequest.disable();
        }
    }

    private boolean ignore(String className, String methodName) {
        return methodName.contains("$") || className.contains("$");
    }

    @Override
    public void complete(Completion completion) {
        int argumentIndex = CompletionUtils.detectArgumentIndex(completion);

        if (argumentIndex == 1) {
            if (!CompletionUtils.completeClassName(completion)) {
                super.complete(completion);
            }
            return;
        } else if (argumentIndex == 2) {
            if (!CompletionUtils.completeMethodName(completion)) {
                super.complete(completion);
            }
            return;
        }

        super.complete(completion);
    }
}
