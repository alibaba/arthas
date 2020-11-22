package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.affect.EnhancerAffect;

/**
 * Data model of EnhancerCommand
 *
 * @author gongdewei 2020/7/20
 */
public class EnhancerModel extends ResultModel {

    private final EnhancerAffectVO effect;
    private boolean success;
    private String message;

    public EnhancerModel(EnhancerAffect effect, boolean success) {
        if (effect != null) {
            this.effect = new EnhancerAffectVO(effect);
            this.success = success;
        } else {
            this.effect = new EnhancerAffectVO(-1, 0, 0, -1);
            this.success = false;
        }
    }

    public EnhancerModel(EnhancerAffect effect, boolean success, String message) {
        this(effect, success);
        this.message = message;
    }

    @Override
    public String getType() {
        return "enhancer";
    }

    public EnhancerAffectVO getEffect() {
        return effect;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
