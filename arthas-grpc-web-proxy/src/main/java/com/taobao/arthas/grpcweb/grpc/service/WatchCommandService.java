package com.taobao.arthas.grpcweb.grpc.service;

import io.arthas.api.ArthasServices.ResponseBody;
import io.arthas.api.ArthasServices.WatchRequest;
import io.arthas.api.WatchGrpc;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.command.model.MessageModel;

import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.grpcweb.grpc.DemoBootstrap;
import com.taobao.arthas.grpcweb.grpc.model.WatchRequestModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.grpcweb.grpc.observer.impl.ArthasStreamObserverImpl;
import com.taobao.arthas.grpcweb.grpc.service.advisor.WatchRpcAdviceListener;
import io.grpc.stub.StreamObserver;

public class WatchCommandService extends WatchGrpc.WatchImplBase {

    private static final Logger logger = LoggerFactory.getLogger(WatchCommandService.class);

    private WatchRequestModel watchRequestModel;

    private ArthasStreamObserver arthasStreamObserver;


    private GrpcJobController grpcJobController;

    public WatchCommandService(GrpcJobController grpcJobController) {
        this.grpcJobController = grpcJobController;
    }

    @Override
    public void watch(WatchRequest watchRequest, StreamObserver<ResponseBody> responseObserver){
        // 解析watchRequest 参数
        watchRequestModel = new WatchRequestModel(watchRequest);
        ArthasStreamObserverImpl<ResponseBody> newArthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver, watchRequestModel, grpcJobController);
        // arthasStreamObserver 传入到advisor中，实现异步传输数据
        if(grpcJobController.containsJob(watchRequestModel.getJobId())){
            arthasStreamObserver = grpcJobController.getGrpcJob(watchRequest.getJobId());
            if(arthasStreamObserver != null && arthasStreamObserver.getPorcessStatus() == ExecStatus.RUNNING){
                WatchRpcAdviceListener listener = (WatchRpcAdviceListener) AdviceWeaver.listener(arthasStreamObserver.getListener().id());
                watchRequestModel.setListenerId(listener.id());
                arthasStreamObserver.setRequestModel(watchRequestModel);
                listener.setArthasStreamObserver(arthasStreamObserver);
                arthasStreamObserver.appendResult(new MessageModel("SUCCESS CHANGE!!!!!!!!!!!"));
                newArthasStreamObserver.setProcessStatus(ExecStatus.RUNNING);
                newArthasStreamObserver.end(0,"修改成功!!!");
                return;
            }else {
                arthasStreamObserver = newArthasStreamObserver;
            }
        }else {
            arthasStreamObserver = newArthasStreamObserver;
        }
        // 创建watch任务
        WatchTask watchTask = new WatchTask();
        // 执行watch任务
        DemoBootstrap.getRunningInstance().execute(watchTask);
    }

    private class WatchTask implements Runnable{
        @Override
        public void run() {
            try {
                watchRequestModel.enhance(arthasStreamObserver);
            } catch (Throwable t) {
                logger.error("Error during processing the command:", t);
                arthasStreamObserver.end(-1, "Error during processing the command: " + t.getClass().getName() + ", message:" + t.getMessage()
                        + ", please check $HOME/logs/arthas/arthas.log for more details." );
            }
        }
    }
}
