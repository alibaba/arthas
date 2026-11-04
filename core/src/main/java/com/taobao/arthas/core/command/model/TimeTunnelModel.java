package com.taobao.arthas.core.command.model;

import java.util.List;
import java.util.Map;

/**
 * Data model of TimeTunnelCommand
 * @author gongdewei 2020/4/27
 */
public class TimeTunnelModel extends ResultModel {

    //查看列表
    private List<TimeFragmentVO> timeFragmentList;

    //是否为第一次输出（需要加表头）
    private Boolean isFirst;

    //查看单条记录
    private TimeFragmentVO timeFragment;

    //重放执行的结果
    private TimeFragmentVO replayResult;

    //重放执行的次数
    private Integer replayNo;

    private ObjectVO watchValue;

    //search: tt -s {} -w {}
    private Map<Integer, ObjectVO> watchResults;

    private Integer expand;

    private Integer sizeLimit;


    @Override
    public String getType() {
        return "tt";
    }

    public List<TimeFragmentVO> getTimeFragmentList() {
        return timeFragmentList;
    }

    public TimeTunnelModel setTimeFragmentList(List<TimeFragmentVO> timeFragmentList) {
        this.timeFragmentList = timeFragmentList;
        return this;
    }

    public TimeFragmentVO getTimeFragment() {
        return timeFragment;
    }

    public TimeTunnelModel setTimeFragment(TimeFragmentVO timeFragment) {
        this.timeFragment = timeFragment;
        return this;
    }

    public Integer getExpand() {
        return expand;
    }

    public TimeTunnelModel setExpand(Integer expand) {
        this.expand = expand;
        return this;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public TimeTunnelModel setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
        return this;
    }

    public ObjectVO getWatchValue() {
        return watchValue;
    }

    public TimeTunnelModel setWatchValue(ObjectVO watchValue) {
        this.watchValue = watchValue;
        return this;
    }

    public Map<Integer, ObjectVO> getWatchResults() {
        return watchResults;
    }

    public TimeTunnelModel setWatchResults(Map<Integer, ObjectVO> watchResults) {
        this.watchResults = watchResults;
        return this;
    }

    public TimeFragmentVO getReplayResult() {
        return replayResult;
    }

    public TimeTunnelModel setReplayResult(TimeFragmentVO replayResult) {
        this.replayResult = replayResult;
        return this;
    }

    public Integer getReplayNo() {
        return replayNo;
    }

    public TimeTunnelModel setReplayNo(Integer replayNo) {
        this.replayNo = replayNo;
        return this;
    }

    public Boolean getFirst() {
        return isFirst;
    }

    public TimeTunnelModel setFirst(Boolean first) {
        isFirst = first;
        return this;
    }
}
