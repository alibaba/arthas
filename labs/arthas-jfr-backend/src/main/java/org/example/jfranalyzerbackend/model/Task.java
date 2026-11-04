
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 任务实体类
 * 表示一个执行任务的基本信息
 */
@Setter
@Getter
public class Task {

    /**
     * 任务唯一标识符
     */
    private long id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务开始时间（单位：毫秒，-1表示未知）
     */
    private long start = -1;

    /**
     * 任务结束时间（单位：毫秒，-1表示未知）
     */
    private long end = -1;
}
