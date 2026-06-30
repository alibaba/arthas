package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.util.affect.EnhancerAffect;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating EnhancerModel and EnhancerAffectVO from EnhancerAffect.
 * The base EnhancerModel and EnhancerAffectVO are defined in arthas-model module.
 *
 * @author gongdewei 2020/7/20
 */
public class EnhancerModelFactory {

    public static EnhancerModel create(EnhancerAffect affect, boolean success) {
        return new EnhancerModel(createEnhancerAffectVO(affect), success);
    }

    public static EnhancerModel create(EnhancerAffect affect, boolean success, String message) {
        return new EnhancerModel(createEnhancerAffectVO(affect), success, message);
    }

    /**
     * Create EnhancerAffectVO from EnhancerAffect.
     * This method is public so other classes like ResetModel can use it.
     */
    public static EnhancerAffectVO createEnhancerAffectVO(EnhancerAffect affect) {
        if (affect == null) {
            return new EnhancerAffectVO(-1, 0, 0, -1);
        }
        
        EnhancerAffectVO vo = new EnhancerAffectVO(
            affect.cost(),
            affect.mCnt(),
            affect.cCnt(),
            affect.getListenerId()
        );
        vo.setThrowable(affect.getThrowable());
        vo.setOverLimitMsg(affect.getOverLimitMsg());
        
        if (GlobalOptions.isDump) {
            List<String> classDumpFiles = new ArrayList<String>();
            for (File classDumpFile : affect.getClassDumpFiles()) {
                classDumpFiles.add(classDumpFile.getAbsolutePath());
            }
            vo.setClassDumpFiles(classDumpFiles);
        }

        if (GlobalOptions.verbose) {
            List<String> methods = new ArrayList<String>();
            methods.addAll(affect.getMethods());
            vo.setMethods(methods);
        }
        
        return vo;
    }
}
