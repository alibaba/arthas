
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 维度结果泛型类
 * 用于封装特定维度的分析结果列表
 * @param <T> 结果类型
 */
@Setter
@Getter
public class DimensionResult<T> {

    /**
     * 结果列表
     */
    private List<T> list;

    /**
     * 添加结果项
     * @param item 要添加的结果项
     */
    public void addResultItem(T item) {
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(item);
    }
}
