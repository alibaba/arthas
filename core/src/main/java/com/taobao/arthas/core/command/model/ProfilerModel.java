package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * Profiler命令结果模型类
 * 用于封装profiler命令的执行结果，profiler命令用于Java应用性能分析和火焰图生成
 *
 * @author gongdewei 2020/4/27
 */
public class ProfilerModel extends ResultModel {

    /**
     * 执行的操作类型
     * 如：start（启动）、stop（停止）、dump（导出）、status（状态）等
     */
    private String action;

    /**
     * 操作参数
     * 操作执行时需要的额外参数信息
     */
    private String actionArg;

    /**
     * profiler stop/dump 输出格式（对应命令行 --format/-o）
     * 支持的格式如：html、collapsed、flamegraph、jfr等
     */
    private String format;

    /**
     * 执行结果
     * 记录profiler命令执行后的返回信息或错误信息
     */
    private String executeResult;

    /**
     * 支持的操作集合
     * 列出profiler命令支持的所有操作类型
     */
    private Collection<String> supportedActions;

    /**
     * 输出文件路径
     * profiler dump操作导出文件的保存路径
     */
    private String outputFile;

    /**
     * 持续时间
     * profiler执行的持续时间（单位：毫秒）
     */
    private Long duration;

    /**
     * 默认构造函数
     * 创建一个空的ProfilerModel对象
     */
    public ProfilerModel() {
    }

    /**
     * 带支持操作列表的构造函数
     *
     * @param supportedActions profiler支持的操作集合
     */
    public ProfilerModel(Collection<String> supportedActions) {
        this.supportedActions = supportedActions;
    }

    /**
     * 获取结果模型的类型标识
     *
     * @return 类型标识字符串 "profiler"
     */
    @Override
    public String getType() {
        return "profiler";
    }

    /**
     * 获取执行的操作类型
     *
     * @return 操作类型字符串
     */
    public String getAction() {
        return action;
    }

    /**
     * 设置执行的操作类型
     *
     * @param action 操作类型字符串
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 获取操作参数
     *
     * @return 操作参数字符串
     */
    public String getActionArg() {
        return actionArg;
    }

    /**
     * 设置操作参数
     *
     * @param actionArg 操作参数字符串
     */
    public void setActionArg(String actionArg) {
        this.actionArg = actionArg;
    }

    /**
     * 获取输出格式
     *
     * @return 输出格式字符串
     */
    public String getFormat() {
        return format;
    }

    /**
     * 设置输出格式
     *
     * @param format 输出格式字符串
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * 获取支持的操作集合
     *
     * @return 支持的操作类型集合
     */
    public Collection<String> getSupportedActions() {
        return supportedActions;
    }

    /**
     * 设置支持的操作集合
     *
     * @param supportedActions 支持的操作类型集合
     */
    public void setSupportedActions(Collection<String> supportedActions) {
        this.supportedActions = supportedActions;
    }

    /**
     * 获取执行结果
     *
     * @return 执行结果字符串
     */
    public String getExecuteResult() {
        return executeResult;
    }

    /**
     * 设置执行结果
     *
     * @param executeResult 执行结果字符串
     */
    public void setExecuteResult(String executeResult) {
        this.executeResult = executeResult;
    }

    /**
     * 获取输出文件路径
     *
     * @return 输出文件的完整路径
     */
    public String getOutputFile() {
        return outputFile;
    }

    /**
     * 设置输出文件路径
     *
     * @param outputFile 输出文件的完整路径
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * 获取持续时间
     *
     * @return 持续时间（毫秒）
     */
    public Long getDuration() {
        return duration;
    }

    /**
     * 设置持续时间
     *
     * @param duration 持续时间（毫秒）
     */
    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
