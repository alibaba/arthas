package com.taobao.arthas.core.command.model;

import java.util.List;
import java.util.Map;

/**
 * TimeTunnel命令的数据模型
 * 用于TimeTunnel（tt）命令的输出，该命令可以记录方法调用、重放方法调用、查看方法调用记录等
 * TimeTunnel是Arthas中用于记录方法调用历史并提供重放功能的强大工具
 *
 * @author gongdewei 2020/4/27
 */
public class TimeTunnelModel extends ResultModel {

    /** 时间片段列表，用于存储多个方法调用记录 */
    private List<TimeFragmentVO> timeFragmentList;

    /** 是否为第一次输出，用于判断是否需要添加表头信息 */
    private Boolean isFirst;

    /** 单条时间片段记录，用于查看单个方法调用的详细信息 */
    private TimeFragmentVO timeFragment;

    /** 重放执行的结果，记录方法重放后的执行结果 */
    private TimeFragmentVO replayResult;

    /** 重放执行的次数，记录当前是第几次重放 */
    private Integer replayNo;

    /** 监视值对象，用于存储通过-w参数指定的表达式求值结果 */
    private ObjectVO watchValue;

    /** 监视结果映射，用于批量查看多个时间片段的监视结果，对应 tt -s {} -w {} 命令 */
    private Map<Integer, ObjectVO> watchResults;

    /** 展开层级，控制对象展开的深度，用于限制对象属性的展开层级 */
    private Integer expand;

    /** 大小限制，限制输出内容的最大长度，防止输出过大内容 */
    private Integer sizeLimit;


    /**
     * 获取模型类型
     * 用于标识这是TimeTunnel命令的输出模型
     *
     * @return 模型类型标识字符串 "tt"
     */
    @Override
    public String getType() {
        return "tt";
    }

    /**
     * 获取时间片段列表
     *
     * @return 时间片段列表，包含多个方法调用记录
     */
    public List<TimeFragmentVO> getTimeFragmentList() {
        return timeFragmentList;
    }

    /**
     * 设置时间片段列表
     * 支持链式调用
     *
     * @param timeFragmentList 时间片段列表
     * @return 当前TimeTunnelModel实例，支持链式调用
     */
    public TimeTunnelModel setTimeFragmentList(List<TimeFragmentVO> timeFragmentList) {
        this.timeFragmentList = timeFragmentList;
        return this;
    }

    /**
     * 获取单条时间片段记录
     *
     * @return 单条时间片段对象
     */
    public TimeFragmentVO getTimeFragment() {
        return timeFragment;
    }

    /**
     * 设置单条时间片段记录
     * 支持链式调用
     *
     * @param timeFragment 单条时间片段对象
     * @return 当前TimeTunnelModel实例，支持链式调用
     */
    public TimeTunnelModel setTimeFragment(TimeFragmentVO timeFragment) {
        this.timeFragment = timeFragment;
        return this;
    }

    /**
     * 获取对象展开层级
     *
     * @return 展开层级数值
     */
    public Integer getExpand() {
        return expand;
    }

    /**
     * 设置对象展开层级
     * 支持链式调用
     *
     * @param expand 展开层级数值
     * @return 当前TimeTunnelModel实例，支持链式调用
     */
    public TimeTunnelModel setExpand(Integer expand) {
        this.expand = expand;
        return this;
    }

    /**
     * 获取大小限制
     *
     * @return 输出内容的最大长度限制
     */
    public Integer getSizeLimit() {
        return sizeLimit;
    }

    /**
     * 设置大小限制
     * 支持链式调用
     *
     * @param sizeLimit 输出内容的最大长度限制
     * @return 当前TimeTunnelModel实例，支持链式调用
     */
    public TimeTunnelModel setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
        return this;
    }

    /**
     * 获取监视值对象
     *
     * @return 监视值的ObjectVO对象
     */
    public ObjectVO getWatchValue() {
        return watchValue;
    }

    /**
     * 设置监视值对象
     * 支持链式调用
     *
     * @param watchValue 监视值的ObjectVO对象
     * @return 当前TimeTunnelModel实例，支持链式调用
     */
    public TimeTunnelModel setWatchValue(ObjectVO watchValue) {
        this.watchValue = watchValue;
        return this;
    }

    /**
     * 获取监视结果映射
     *
     * @return 监视结果映射，key为索引，value为监视结果对象
     */
    public Map<Integer, ObjectVO> getWatchResults() {
        return watchResults;
    }

    /**
     * 设置监视结果映射
     * 支持链式调用
     *
     * @param watchResults 监视结果映射
     * @return 当前TimeTunnelModel实例，支持链式调用
     */
    public TimeTunnelModel setWatchResults(Map<Integer, ObjectVO> watchResults) {
        this.watchResults = watchResults;
        return this;
    }

    /**
     * 获取重放执行的结果
     *
     * @return 重放执行后的时间片段对象
     */
    public TimeFragmentVO getReplayResult() {
        return replayResult;
    }

    /**
     * 设置重放执行的结果
     * 支持链式调用
     *
     * @param replayResult 重放执行后的时间片段对象
     * @return 当前TimeTunnelModel实例，支持链式调用
     */
    public TimeTunnelModel setReplayResult(TimeFragmentVO replayResult) {
        this.replayResult = replayResult;
        return this;
    }

    /**
     * 获取重放执行的次数
     *
     * @return 重放执行的次数
     */
    public Integer getReplayNo() {
        return replayNo;
    }

    /**
     * 设置重放执行的次数
     * 支持链式调用
     *
     * @param replayNo 重放执行的次数
     * @return 当前TimeTunnelModel实例，支持链式调用
     */
    public TimeTunnelModel setReplayNo(Integer replayNo) {
        this.replayNo = replayNo;
        return this;
    }

    /**
     * 判断是否为第一次输出
     *
     * @return 如果是第一次输出返回true，否则返回false
     */
    public Boolean getFirst() {
        return isFirst;
    }

    /**
     * 设置是否为第一次输出
     * 支持链式调用
     *
     * @param first true表示第一次输出，false表示非第一次输出
     * @return 当前TimeTunnelModel实例，支持链式调用
     */
    public TimeTunnelModel setFirst(Boolean first) {
        isFirst = first;
        return this;
    }
}
