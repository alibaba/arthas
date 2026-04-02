package com.taobao.arthas.core.shell.future;


import com.taobao.arthas.core.shell.handlers.Handler;

/**
 * 异步结果Future类
 * 用于表示异步操作的结果，支持成功和失败两种状态
 * 可以设置处理器在操作完成时执行回调
 *
 * @param <T> 结果的类型
 */
public class Future<T> {
    /**
     * 是否失败标志
     */
    private boolean failed;

    /**
     * 是否成功标志
     */
    private boolean succeeded;

    /**
     * 完成时的处理器
     */
    private Handler<Future<T>> handler;

    /**
     * 操作成功时的结果
     */
    private T result;

    /**
     * 操作失败时的异常信息
     */
    private Throwable throwable;

    /**
     * 默认构造函数，创建一个未完成的Future
     */
    public Future() {
    }

    /**
     * 创建一个已失败的Future
     *
     * @param t 导致失败的异常
     */
    public Future(Throwable t) {
        fail(t);
    }

    /**
     * 创建一个已失败的Future，使用失败消息作为异常
     *
     * @param failureMessage 失败消息
     */
    public Future(String failureMessage) {
        this(new Throwable(failureMessage));
    }

    /**
     * 创建一个已成功的Future
     *
     * @param result 操作的结果
     */
    public Future(T result) {
        complete(result);
    }

    /**
     * 静态工厂方法，创建一个未完成的Future
     *
     * @param <T> 结果的类型
     * @return 新的Future实例
     */
    public static <T> Future<T> future() {
        return new Future<T>();
    }

    /**
     * 静态工厂方法，创建一个已成功的Future，结果为null
     *
     * @param <T> 结果的类型
     * @return 已完成的Future实例
     */
    public static <T> Future<T> succeededFuture() {
        return new Future<T>((T) null);
    }

    /**
     * 静态工厂方法，创建一个已成功的Future
     *
     * @param <T> 结果的类型
     * @param result 操作的结果
     * @return 已完成的Future实例
     */
    public static <T> Future<T> succeededFuture(T result) {
        return new Future<T>(result);
    }

    /**
     * 静态工厂方法，创建一个已失败的Future
     *
     * @param <T> 结果的类型
     * @param t 导致失败的异常
     * @return 已失败的Future实例
     */
    public static <T> Future<T> failedFuture(Throwable t) {
        return new Future<T>(t);
    }

    /**
     * 静态工厂方法，创建一个已失败的Future，使用失败消息作为异常
     *
     * @param <T> 结果的类型
     * @param failureMessage 失败消息
     * @return 已失败的Future实例
     */
    public static <T> Future<T> failedFuture(String failureMessage) {
        return new Future<T>(failureMessage);
    }

    /**
     * 判断Future是否已完成（成功或失败）
     *
     * @return 如果已完成返回true，否则返回false
     */
    public boolean isComplete() {
        return failed || succeeded;
    }

    /**
     * 设置完成处理器
     * 如果Future已经完成，会立即调用处理器；否则在完成时调用
     *
     * @param handler 要设置的处理器
     * @return 当前Future实例，支持链式调用
     */
    public Future<T> setHandler(Handler<Future<T>> handler) {
        this.handler = handler;
        checkCallHandler();
        return this;
    }


    /**
     * 标记Future为成功完成状态，并设置结果
     *
     * @param result 操作的结果
     * @throws IllegalStateException 如果Future已经完成
     */
    public void complete(T result) {
        checkComplete();
        this.result = result;
        succeeded = true;
        checkCallHandler();
    }

    /**
     * 标记Future为成功完成状态，结果为null
     *
     * @throws IllegalStateException 如果Future已经完成
     */
    public void complete() {
        complete(null);
    }

    /**
     * 标记Future为失败状态，并设置失败原因
     *
     * @param throwable 导致失败的异常
     * @throws IllegalStateException 如果Future已经完成
     */
    public void fail(Throwable throwable) {
        checkComplete();
        this.throwable = throwable;
        failed = true;
        checkCallHandler();
    }

    /**
     * 标记Future为失败状态，使用失败消息作为异常
     *
     * @param failureMessage 失败消息
     * @throws IllegalStateException 如果Future已经完成
     */
    public void fail(String failureMessage) {
        fail(new Throwable(failureMessage));
    }

    /**
     * 获取操作的结果
     *
     * @return 操作的结果，可能为null
     */
    public T result() {
        return result;
    }

    /**
     * 获取导致失败的异常
     *
     * @return 导致失败的Throwable，可能为null
     */
    public Throwable cause() {
        return throwable;
    }

    /**
     * 判断Future是否成功完成
     *
     * @return 如果成功完成返回true，否则返回false
     */
    public boolean succeeded() {
        return succeeded;
    }

    /**
     * 判断Future是否失败
     *
     * @return 如果失败返回true，否则返回false
     */
    public boolean failed() {
        return failed;
    }

    /**
     * 创建一个补全器（completer）处理器
     * 当传入的Future完成时，将结果传递给当前Future
     * 这通常用于将一个Future的结果传递给另一个Future
     *
     * @return 一个新的Handler，用于处理其他Future的完成事件
     */
    public Handler<Future<T>> completer() {
        return new Handler<Future<T>>() {
            @Override
            public void handle(Future<T> ar) {
                if (ar.succeeded()) {
                    // 如果传入的Future成功，则将结果传递给当前Future
                    complete(ar.result());
                } else {
                    // 如果传入的Future失败，则将失败原因传递给当前Future
                    fail(ar.cause());
                }
            }
        };
    }

    /**
     * 检查是否需要调用处理器
     * 如果处理器已设置且Future已完成，则立即调用处理器
     */
    private void checkCallHandler() {
        if (handler != null && isComplete()) {
            handler.handle(this);
        }
    }

    /**
     * 检查Future是否已经完成
     * 如果已完成，则抛出异常，防止重复完成
     *
     * @throws IllegalStateException 如果Future已经完成（成功或失败）
     */
    private void checkComplete() {
        if (succeeded || failed) {
            throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
        }
    }
}
