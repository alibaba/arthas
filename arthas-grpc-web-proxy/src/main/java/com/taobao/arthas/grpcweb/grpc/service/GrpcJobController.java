package com.taobao.arthas.grpcweb.grpc.service;


import com.taobao.arthas.core.advisor.TransformerManager;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.grpcweb.grpc.view.GrpcResultViewResolver;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GrpcJobController{

    private Map<Long/*JOB_ID*/, ArthasStreamObserver> jobs
            = new ConcurrentHashMap<Long, ArthasStreamObserver>();
//    private Map<Long/*JOB_ID*/, ArthasStreamObserver> jobs
//            = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    private GrpcResultViewResolver resultViewResolver;

    private Instrumentation instrumentation;

    private TransformerManager transformerManager;

    public GrpcJobController(Instrumentation instrumentation, TransformerManager transformerManager, GrpcResultViewResolver resultViewResolver){
        this.instrumentation = instrumentation;
        this.transformerManager = transformerManager;
        this.resultViewResolver = resultViewResolver;
    }


    public Set<Long> getJobIds(){
        return jobs.keySet();
    }

    public void registerGrpcJob(long jobId,ArthasStreamObserver arthasStreamObserver){
        jobs.put(jobId, arthasStreamObserver);
    }

    public void unRegisterGrpcJob(long jobId){
        if(jobs.containsKey(jobId)){
            jobs.remove(jobId);
        }
    }
    public boolean containsJob(long jobId){
        return jobs.containsKey(jobId);
    }

    public ArthasStreamObserver getGrpcJob(long jobId){
        if(this.containsJob(jobId)){
            return jobs.get(jobId);
        }else {
            return null;
        }
    }

    public int generateGrpcJobId(){
        int jobId = idGenerator.incrementAndGet();
        return jobId;
    }

    public GrpcResultViewResolver getResultViewResolver() {
        return resultViewResolver;
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public TransformerManager getTransformerManager() {
        return transformerManager;
    }

    public void setTransformerManager(TransformerManager transformerManager) {
        this.transformerManager = transformerManager;
    }
}
