package com.taobao.arthas.core.shell.future;


import com.taobao.arthas.core.shell.handlers.Handler;

public class Future<T> {
    private boolean failed;
    private boolean succeeded;
    private Handler<Future<T>> handler;
    private T result;
    private Throwable throwable;

    public Future() {
    }

    public Future(Throwable t) {
        fail(t);
    }

    public Future(String failureMessage) {
        this(new Throwable(failureMessage));
    }

    public Future(T result) {
        complete(result);
    }

    public static <T> Future<T> future() {
        return new Future<T>();
    }

    public static <T> Future<T> succeededFuture() {
        return new Future<T>((T) null);
    }

    public static <T> Future<T> succeededFuture(T result) {
        return new Future<T>(result);
    }

    public static <T> Future<T> failedFuture(Throwable t) {
        return new Future<T>(t);
    }

    public static <T> Future<T> failedFuture(String failureMessage) {
        return new Future<T>(failureMessage);
    }

    public boolean isComplete() {
        return failed || succeeded;
    }

    public Future<T> setHandler(Handler<Future<T>> handler) {
        this.handler = handler;
        checkCallHandler();
        return this;
    }


    public void complete(T result) {
        checkComplete();
        this.result = result;
        succeeded = true;
        checkCallHandler();
    }

    public void complete() {
        complete(null);
    }

    public void fail(Throwable throwable) {
        checkComplete();
        this.throwable = throwable;
        failed = true;
        checkCallHandler();
    }

    public void fail(String failureMessage) {
        fail(new Throwable(failureMessage));
    }

    public T result() {
        return result;
    }

    public Throwable cause() {
        return throwable;
    }

    public boolean succeeded() {
        return succeeded;
    }

    public boolean failed() {
        return failed;
    }

    public Handler<Future<T>> completer() {
        return new Handler<Future<T>>() {
            @Override
            public void handle(Future<T> ar) {
                if (ar.succeeded()) {
                    complete(ar.result());
                } else {
                    fail(ar.cause());
                }
            }
        };
    }

    private void checkCallHandler() {
        if (handler != null && isComplete()) {
            handler.handle(this);
        }
    }

    private void checkComplete() {
        if (succeeded || failed) {
            throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
        }
    }
}
