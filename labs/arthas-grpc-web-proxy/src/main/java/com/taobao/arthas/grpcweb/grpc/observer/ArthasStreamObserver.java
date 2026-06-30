package com.taobao.arthas.grpcweb.grpc.observer;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.shell.system.ExecStatus;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

public interface ArthasStreamObserver<T>  {

    void onNext(T value);

    void onError(Throwable t);

    void onCompleted();

    Instrumentation getInstrumentation();

    ArthasStreamObserver write(String msg);

    void appendResult(ResultModel result);

    AtomicInteger times();

    void register(AdviceListener listener, ClassFileTransformer transformer);

    void unregister();

    void end();

    ExecStatus getPorcessStatus();

    void setProcessStatus(ExecStatus execStatus);
    /**
     * End the process.
     *
     * @param status the exit status.
     */
    void end(int status);
    /**
     * End the process.
     *
     * @param status the exit status.
     */
    void end(int status, String message);

    int getJobId();

    Object getRequestModel();

    void setRequestModel(Object requestModel);

    AdviceListener getListener();
}
