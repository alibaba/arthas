package com.taobao.arthas.grpcweb.grpc.observer.impl;

import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Process;
import com.taobao.arthas.core.shell.term.Tty;

import java.util.Date;

public class GrpcProcess implements Process {

    private ExecStatus processStatus;

    public void setProcessStatus(ExecStatus processStatus) {
        this.processStatus = processStatus;
    }

    @Override
    public ExecStatus status() {
        return processStatus;
    }

    @Override
    public Integer exitCode() {
        return null;
    }

    @Override
    public Process setTty(Tty tty) {
        return null;
    }

    @Override
    public Tty getTty() {
        return null;
    }

    @Override
    public Process setSession(Session session) {
        return null;
    }

    @Override
    public Session getSession() {
        return null;
    }

    @Override
    public Process terminatedHandler(Handler<Integer> handler) {
        return null;
    }

    @Override
    public void run() {

    }

    @Override
    public void run(boolean foreground) {

    }

    @Override
    public boolean interrupt() {
        return false;
    }

    @Override
    public boolean interrupt(Handler<Void> completionHandler) {
        return false;
    }

    @Override
    public void resume() {

    }

    @Override
    public void resume(boolean foreground) {

    }

    @Override
    public void resume(Handler<Void> completionHandler) {

    }

    @Override
    public void resume(boolean foreground, Handler<Void> completionHandler) {

    }

    @Override
    public void suspend() {

    }

    @Override
    public void suspend(Handler<Void> completionHandler) {

    }

    @Override
    public void terminate() {

    }

    @Override
    public void terminate(Handler<Void> completionHandler) {

    }

    @Override
    public void toBackground() {

    }

    @Override
    public void toBackground(Handler<Void> completionHandler) {

    }

    @Override
    public void toForeground() {

    }

    @Override
    public void toForeground(Handler<Void> completionHandler) {

    }

    @Override
    public int times() {
        return 0;
    }

    @Override
    public Date startTime() {
        return null;
    }

    @Override
    public String cacheLocation() {
        return null;
    }

    @Override
    public void setJobId(int jobId) {

    }
}
