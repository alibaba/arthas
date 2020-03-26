package com.taobao.arthas.core.command.result;

import com.taobao.arthas.core.shell.command.CommandProcess;

public class WatchResult extends ExecResult {

    private String ts;
    private double cost;
    private String result;

    public WatchResult(String ts, double cost, String result) {
        this.ts = ts;
        this.cost = cost;
        this.result = result;
    }

    @Override
    public String getType() {
        return "watch";
    }

    public String getTs() {
        return ts;
    }

    public double getCost() {
        return cost;
    }

    public String getResult() {
        return result;
    }

    @Override
    protected void write(CommandProcess process) {
        process.write("ts=" + ts + "; [cost=" + cost + "ms] result=" + result + "\n");
    }
}
