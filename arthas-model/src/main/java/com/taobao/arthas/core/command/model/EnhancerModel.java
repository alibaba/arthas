package com.taobao.arthas.core.command.model;

/**
 * 增强命令的数据模型
 * <p>
 * 用于封装增强命令（如 watch、trace、stack 等）执行后的结果信息，
 * 包括增强效果、执行状态和相关消息。
 * </p>
 *
 * @author gongdewei 2020/7/20
 */
public class EnhancerModel extends ResultModel {

    /**
     * 增强效果对象
     * 包含类/方法增强的统计信息，如增强的数量、影响的类等
     */
    private EnhancerAffectVO effect;

    /**
     * 执行是否成功
     * true 表示增强命令执行成功，false 表示失败
     */
    private boolean success;

    /**
     * 消息文本
     * 用于记录执行过程中的相关信息或错误消息
     */
    private String message;

    /**
     * 默认构造函数
     * 创建一个空的增强模型对象
     */
    public EnhancerModel() {
    }

    /**
     * 构造函数
     *
     * @param effect   增强效果对象
     * @param success  执行是否成功
     */
    public EnhancerModel(EnhancerAffectVO effect, boolean success) {
        this.effect = effect;
        this.success = success;
    }

    /**
     * 完整构造函数
     *
     * @param effect   增强效果对象
     * @param success  执行是否成功
     * @param message  消息文本
     */
    public EnhancerModel(EnhancerAffectVO effect, boolean success, String message) {
        this.effect = effect;
        this.success = success;
        this.message = message;
    }

    /**
     * 获取模型类型标识
     * 用于序列化时区分不同的模型类型
     *
     * @return 类型标识字符串 "enhancer"
     */
    @Override
    public String getType() {
        return "enhancer";
    }

    /**
     * 获取增强效果对象
     *
     * @return 增强效果对象
     */
    public EnhancerAffectVO getEffect() {
        return effect;
    }

    /**
     * 设置增强效果对象
     *
     * @param effect 增强效果对象
     */
    public void setEffect(EnhancerAffectVO effect) {
        this.effect = effect;
    }

    /**
     * 获取执行状态
     *
     * @return true 表示成功，false 表示失败
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 设置执行状态
     *
     * @param success 执行状态，true 表示成功，false 表示失败
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * 获取消息文本
     *
     * @return 消息文本
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置消息文本
     *
     * @param message 消息文本
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
