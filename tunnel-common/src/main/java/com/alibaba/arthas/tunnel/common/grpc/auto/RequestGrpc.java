package com.alibaba.arthas.tunnel.common.grpc.auto;

import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.14.0)",
    comments = "Source: arthas_grpc_service.proto")
public final class RequestGrpc {

  private RequestGrpc() {}

  public static final String SERVICE_NAME = "Request";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<Payload,
      Payload> getRequestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "request",
      requestType = Payload.class,
      responseType = Payload.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Payload,
      Payload> getRequestMethod() {
    io.grpc.MethodDescriptor<Payload, Payload> getRequestMethod;
    if ((getRequestMethod = RequestGrpc.getRequestMethod) == null) {
      synchronized (RequestGrpc.class) {
        if ((getRequestMethod = RequestGrpc.getRequestMethod) == null) {
          RequestGrpc.getRequestMethod = getRequestMethod =
              io.grpc.MethodDescriptor.<Payload, Payload>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(MethodDescriptor.generateFullMethodName(
                  "Request", "request"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Payload.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Payload.getDefaultInstance()))
                  .setSchemaDescriptor(new RequestMethodDescriptorSupplier("request"))
                  .build();
          }
        }
     }
     return getRequestMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RequestStub newStub(io.grpc.Channel channel) {
    return new RequestStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RequestBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new RequestBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RequestFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new RequestFutureStub(channel);
  }

  /**
   */
  public static abstract class RequestImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Sends a commonRequest
     * </pre>
     */
    public void request(Payload request,
        io.grpc.stub.StreamObserver<Payload> responseObserver) {
      ServerCalls.asyncUnimplementedUnaryCall(getRequestMethod(), responseObserver);
    }

    @Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getRequestMethod(),
            ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                Payload,
                Payload>(
                  this, METHODID_REQUEST)))
          .build();
    }
  }

  /**
   */
  public static final class RequestStub extends io.grpc.stub.AbstractStub<RequestStub> {
    private RequestStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RequestStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected RequestStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RequestStub(channel, callOptions);
    }

    /**
     * <pre>
     * Sends a commonRequest
     * </pre>
     */
    public void request(Payload request,
        io.grpc.stub.StreamObserver<Payload> responseObserver) {
      ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRequestMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class RequestBlockingStub extends io.grpc.stub.AbstractStub<RequestBlockingStub> {
    private RequestBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RequestBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected RequestBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RequestBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Sends a commonRequest
     * </pre>
     */
    public Payload request(Payload request) {
      return ClientCalls.blockingUnaryCall(
          getChannel(), getRequestMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class RequestFutureStub extends io.grpc.stub.AbstractStub<RequestFutureStub> {
    private RequestFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RequestFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected RequestFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RequestFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Sends a commonRequest
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Payload> request(
        Payload request) {
      return ClientCalls.futureUnaryCall(
          getChannel().newCall(getRequestMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REQUEST = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RequestImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RequestImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REQUEST:
          serviceImpl.request((Payload) request,
              (io.grpc.stub.StreamObserver<Payload>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class RequestBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RequestBaseDescriptorSupplier() {}

    @Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ArthasGrpcService.getDescriptor();
    }

    @Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Request");
    }
  }

  private static final class RequestFileDescriptorSupplier
      extends RequestBaseDescriptorSupplier {
    RequestFileDescriptorSupplier() {}
  }

  private static final class RequestMethodDescriptorSupplier
      extends RequestBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RequestMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RequestGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RequestFileDescriptorSupplier())
              .addMethod(getRequestMethod())
              .build();
        }
      }
    }
    return result;
  }
}
