package com.taobao.arthas.core.util.affect;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 行记录影响反馈类
 * 继承自Affect类，除了跟踪操作耗时外，还统计影响的行数
 * 用于记录数据库操作、文件操作等场景中的行数统计
 *
 * Created by vlinux on 15/5/21.
 */
public final class RowAffect extends Affect {

    /**
     * 影响的行数计数器
     * 使用AtomicInteger保证多线程环境下的线程安全性
     */
    private final AtomicInteger rCnt = new AtomicInteger();

    /**
     * 默认构造函数
     * 创建一个行影响对象，初始行数为0
     */
    public RowAffect() {
    }

    /**
     * 指定初始行数的构造函数
     *
     * @param rCnt 初始的影响行数
     */
    public RowAffect(int rCnt) {
        // 调用rCnt方法设置初始行数
        this.rCnt(rCnt);
    }

    /**
     * 影响行数统计
     * 原子性地增加或减少影响行数，并返回更新后的值
     *
     * @param mc 行影响计数的增量（可以为负数，表示减少）
     * @return 更新后的当前影响行数
     */
    public int rCnt(int mc) {
        // 原子性地将mc加到当前计数器值上，并返回新值
        return rCnt.addAndGet(mc);
    }

    /**
     * 获取当前影响行个数
     * 直接返回计数器的当前值，不修改计数器状态
     *
     * @return 当前的影响行数
     */
    public int rCnt() {
        // 获取当前计数器的值
        return rCnt.get();
    }

    /**
     * 将影响信息转换为字符串格式
     * 重写父类方法，增加了影响行数的显示
     *
     * @return 包含影响行数和耗时的格式化字符串
     */
    @Override
    public String toString() {
        // 格式化输出影响行数和操作耗时
        return String.format("Affect(row-cnt:%d) cost in %s ms.",
                rCnt(),  // 获取影响行数
                cost()); // 获取操作耗时
    }
}
