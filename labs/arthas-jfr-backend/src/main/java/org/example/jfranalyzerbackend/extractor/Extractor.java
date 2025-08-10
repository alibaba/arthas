
package org.example.jfranalyzerbackend.extractor;



import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class Extractor extends EventVisitor {
    private static final Map<String, BiConsumer<Extractor, RecordedEvent>> DISPATCHER = new HashMap<>() {
        {
            put(EventConstant.GARBAGE_COLLECTION, EventVisitor::visitGarbageCollection);
            put(EventConstant.UNSIGNED_INT_FLAG, EventVisitor::visitUnsignedIntFlag);
            put(EventConstant.CPU_INFORMATION, EventVisitor::visitCPUInformation);
            put(EventConstant.CPC_RUNTIME_INFORMATION, EventVisitor::visitCPCRuntimeInformation);
            put(EventConstant.ENV_VAR, EventVisitor::visitEnvVar);
            put(EventConstant.ACTIVE_SETTING, EventVisitor::visitActiveSetting);
            put(EventConstant.THREAD_START, EventVisitor::visitThreadStart);
            put(EventConstant.THREAD_CPU_LOAD, EventVisitor::visitThreadCPULoad);
            put(EventConstant.PROCESS_CPU_LOAD, EventVisitor::visitProcessCPULoad);
            put(EventConstant.EXECUTION_SAMPLE, EventVisitor::visitExecutionSample);
            put(EventConstant.NATIVE_EXECUTION_SAMPLE, EventVisitor::visitNativeExecutionSample);
            put(EventConstant.EXECUTE_VM_OPERATION, EventVisitor::visitExecuteVMOperation);
            put(EventConstant.OBJECT_ALLOCATION_IN_NEW_TLAB, EventVisitor::visitObjectAllocationInNewTLAB);
            put(EventConstant.OBJECT_ALLOCATION_OUTSIDE_TLAB, EventVisitor::visitObjectAllocationOutsideTLAB);
            put(EventConstant.OBJECT_ALLOCATION_SAMPLE, EventVisitor::visitObjectAllocationSample);

            put(EventConstant.FILE_FORCE, EventVisitor::visitFileForce);
            put(EventConstant.FILE_READ, EventVisitor::visitFileRead);
            put(EventConstant.FILE_WRITE, EventVisitor::visitFileWrite);

            put(EventConstant.SOCKET_READ, EventVisitor::visitSocketRead);
            put(EventConstant.SOCKET_WRITE, EventVisitor::visitSocketWrite);

            put(EventConstant.JAVA_MONITOR_ENTER, EventVisitor::visitMonitorEnter);
            put(EventConstant.THREAD_PARK, EventVisitor::visitThreadPark);

            put(EventConstant.CLASS_LOAD, EventVisitor::visitClassLoad);

            put(EventConstant.THREAD_SLEEP, EventVisitor::visitThreadSleep);
        }
    };

    final JFRAnalysisContext context;

    private final List<String> interested;

    Extractor(JFRAnalysisContext context, List<String> interested) {
        this.context = context;
        this.interested = interested;
    }

    private boolean accept(RecordedEvent event) {
        return interested.contains(event.getEventType().name());
    }

    public void process(RecordedEvent event) {
        if (accept(event)) {
            DISPATCHER.get(event.getEventType().name()).accept(this, event);
        }
    }

    public abstract void fillResult(AnalysisResult result);
}
