package com.taobao.arthas.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 命令结果处理工具类
 * @author gongdewei 2020/5/18
 */
public class ResultUtils {

    /**
     * 分页处理class列表，转换为className列表
     * @param classes
     * @param pageSize
     * @param handler
     */
    public static void processClassNames(Collection<Class<?>> classes, int pageSize, PaginationHandler<List<String>> handler) {
        List<String> classNames = new ArrayList<String>(pageSize);
        int segment = 0;
        for (Class aClass : classes) {
            classNames.add(aClass.getName());
            //slice segment
            if(classNames.size() >= pageSize) {
                handler.handle(classNames, segment++);
                classNames = new ArrayList<String>(pageSize);
            }
        }
        //last segment
        if (classNames.size() > 0) {
            handler.handle(classNames, segment++);
        }
    }

    /**
     * 分页数据处理回调接口
     * @param <T>
     */
    public interface PaginationHandler<T> {

        /**
         * 处理分页数据
         * @param list
         * @param segment
         * @return  true 继续处理剩余数据， false 终止处理
         */
        boolean handle(T list, int segment);
    }
}
