package com.taobao.arthas.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 命令结果处理工具类
 * 提供分页处理类集合的功能，将大集合拆分为小批次进行处理
 * @author gongdewei 2020/5/18
 */
public class ResultUtils {

    /**
     * 分页处理class列表，转换为className列表
     * 将Class集合按照指定的页大小进行分批处理，每批转换为类名列表后调用处理器
     * 这种分批处理方式可以有效避免一次性加载过多数据导致的内存问题
     *
     * @param classes 要处理的类集合
     * @param pageSize 每页的大小（每批处理的类数量）
     * @param handler 分页处理器，用于处理每批数据
     */
    public static void processClassNames(Collection<Class<?>> classes, int pageSize, PaginationHandler<List<String>> handler) {
        // 创建一个ArrayList来存储当前批次的类名，初始化容量为pageSize
        List<String> classNames = new ArrayList<String>(pageSize);
        // 记录当前处理的是第几批（从0开始）
        int segment = 0;
        // 遍历所有类
        for (Class aClass : classes) {
            // 将类的全限定名添加到当前批次列表
            classNames.add(aClass.getName());
            // 如果当前批次已满，进行分片处理
            if(classNames.size() >= pageSize) {
                // 调用处理器处理当前批次的数据
                handler.handle(classNames, segment++);
                // 创建新的列表用于下一批次
                classNames = new ArrayList<String>(pageSize);
            }
        }
        // 处理最后一批（可能不满一页）
        if (classNames.size() > 0) {
            handler.handle(classNames, segment++);
        }
    }

    /**
     * 分页数据处理回调接口
     * 用于处理分页数据的回调接口，每批数据都会调用handle方法
     * @param <T> 处理的数据类型
     */
    public interface PaginationHandler<T> {

        /**
         * 处理分页数据
         * 每批数据都会调用此方法进行处理
         *
         * @param list 当前批次的数据列表
         * @param segment 当前批次的序号（从0开始）
         * @return true: 继续处理剩余数据； false: 终止处理，不再处理后续批次
         */
        boolean handle(T list, int segment);
    }
}
