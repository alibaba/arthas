package com.taobao.arthas.core.command.model;

/**
 * Data model of EnhancerCommand
 *
 * @author gongdewei 2020/7/20
 */
public class EnhancerModel extends ResultModel {

    private EnhancerAffectVO effect;
    private boolean success;
    private String message;

    public EnhancerModel() {
    }

    public EnhancerModel(EnhancerAffectVO effect, boolean success) {
        this.effect = effect;
        this.success = success;
    }

    public EnhancerModel(EnhancerAffectVO effect, boolean success, String message) {
        this.effect = effect;
        this.success = success;
        this.message = message;
    }

    @Override
    public String getType() {
        return "enhancer";
    }

    public EnhancerAffectVO getEffect() {
        return effect;
    }

    public void setEffect(EnhancerAffectVO effect) {
        this.effect = effect;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
