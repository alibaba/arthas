package com.taobao.arthas.core.command.model;

/**
 * Watch command result model
 *
 * @author gongdewei 2020.03.26
 */
public class WatchModel extends ResultModel {

    private String ts;
    private double cost;
    private String result;

    public WatchModel() {
    }

    public WatchModel(String ts, double cost, String result) {
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

}
