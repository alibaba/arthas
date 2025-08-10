
package org.example.jfranalyzerbackend.extractor;


import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

public abstract class EventVisitor {
    void visitUnsignedIntFlag(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitGarbageCollection(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitCPUInformation(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitEnvVar(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitCPCRuntimeInformation(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitActiveSetting(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitThreadStart(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitProcessCPULoad(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitThreadCPULoad(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitExecutionSample(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitNativeExecutionSample(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitExecuteVMOperation(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitObjectAllocationInNewTLAB(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitObjectAllocationOutsideTLAB(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitObjectAllocationSample(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitFileRead(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitFileWrite(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitFileForce(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitSocketRead(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitSocketWrite(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitMonitorEnter(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitThreadPark(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitClassLoad(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }

    void visitThreadSleep(RecordedEvent event) {
        throw new UnsupportedOperationException();
    }
}
