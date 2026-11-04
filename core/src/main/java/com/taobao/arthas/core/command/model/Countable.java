package com.taobao.arthas.core.command.model;

/**
 * Item countable for ResultModel
 * @author gongdewei 2020/6/8
 */
public interface Countable {

    /**
     * Get item size of this result model, the value of size is greater than or equal to 1
     * @return item size of this result model
     */
    int size();

}
