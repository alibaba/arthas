package com.taobao.arthas.grpcweb.grpc.service;

import com.taobao.arthas.core.shell.system.ExecStatus;
import io.arthas.api.ArthasServices.ResponseBody;
import io.arthas.api.ArthasServices.StringKey;
import io.arthas.api.ArthasServices.StringStringMapValue;
import io.arthas.api.SystemPropertyGrpc;
import com.google.protobuf.Empty;
import com.taobao.arthas.core.command.model.SystemPropertyModel;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.grpcweb.grpc.observer.impl.ArthasStreamObserverImpl;
import io.grpc.stub.StreamObserver;

import java.util.Map;

public class SystemPropertyCommandService extends SystemPropertyGrpc.SystemPropertyImplBase{

    private GrpcJobController grpcJobController;

    public SystemPropertyCommandService(GrpcJobController grpcJobController) {
        this.grpcJobController = grpcJobController;
    }

    @Override
    public void get(Empty empty, StreamObserver<ResponseBody> responseObserver){
        ArthasStreamObserver<ResponseBody> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver, null, grpcJobController);
        arthasStreamObserver.setProcessStatus(ExecStatus.RUNNING);
        arthasStreamObserver.appendResult(new SystemPropertyModel(System.getProperties()));
        arthasStreamObserver.end();
    }

    @Override
    public void getByKey(StringKey request, StreamObserver<ResponseBody> responseObserver){
        String propertyName = request.getKey();
        ArthasStreamObserver<ResponseBody> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver,null, grpcJobController);
        arthasStreamObserver.setProcessStatus(ExecStatus.RUNNING);
        // view the specified system property
        String value = System.getProperty(propertyName);
        if (value == null) {
            arthasStreamObserver.end(-1, "There is no property with the key " + propertyName);
            return;
        } else {
            arthasStreamObserver.appendResult(new SystemPropertyModel(propertyName, value));
            arthasStreamObserver.end();
        }
    }

    @Override
    public void update(StringStringMapValue request, StreamObserver<ResponseBody> responseObserver){
        // get properties from client
        Map<String, String> properties = request.getStringStringMapMap();
        String propertyName = "";
        String propertyValue = "";
        // change system property
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            propertyName = entry.getKey();
            propertyValue = entry.getValue();
        }
        ArthasStreamObserver<ResponseBody> arthasStreamObserver = new ArthasStreamObserverImpl<>(responseObserver,null, grpcJobController);
        arthasStreamObserver.setProcessStatus(ExecStatus.RUNNING);
        try {
            System.setProperty(propertyName, propertyValue);
            arthasStreamObserver.appendResult(new SystemPropertyModel(propertyName, System.getProperty(propertyName)));
            arthasStreamObserver.onCompleted();
        }catch (Throwable t) {
            arthasStreamObserver.end(-1, "Error during setting system property: " + t.getMessage());
        }
    }
}
