package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;

import java.time.LocalDateTime;

/**
 * 时间碎片
 * <p>
 * 用于记录一次方法调用的完整信息，包括调用上下文（Advice）、创建时间和执行耗时。
 * 每个时间碎片代表了某个时刻的一个方法调用快照，可以用于后续的重放、查看等操作。
 * </p>
 * <p>
 * 时间碎片是TimeTunnel（时光隧道）功能的核心数据结构，通过保存方法调用的完整上下文，
 * 使得开发者可以"穿越回"某个时刻，重新查看或执行该方法。
 * </p>
 */
class TimeFragment {

    /**
     * 构造一个时间碎片对象
     *
     * @param advice    方法调用的上下文信息，包含类、方法、参数、返回值等
     * @param gmtCreate 时间碎片的创建时间
     * @param cost      方法执行的耗时（毫秒）
     */
    public TimeFragment(Advice advice, LocalDateTime gmtCreate, double cost) {
        this.advice = advice;
        this.gmtCreate = gmtCreate;
        this.cost = cost;
    }

    /**
     * 方法调用的上下文信息
     * <p>
     * Advice对象包含了该次方法调用的完整信息：
     * - 类加载器
     * - 目标类
     * - 目标方法
     * - 目标对象
     * - 方法参数
     * - 返回值或抛出的异常
     * </p>
     */
    private final Advice advice;

    /**
     * 时间碎片的创建时间
     * <p>
     * 记录了该方法调用发生的时间点，用于标识这是"何时"的调用。
     * </p>
     */
    private final LocalDateTime gmtCreate;

    /**
     * 方法执行的耗时
     * <p>
     * 记录了该方法调用从开始到结束所花费的时间（单位：毫秒）。
     * </p>
     */
    private final double cost;

    /**
     * 获取方法调用的上下文信息
     *
     * @return Advice对象，包含完整的调用上下文
     */
    public Advice getAdvice() {
        return advice;
    }

    /**
     * 获取时间碎片的创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    /**
     * 获取方法执行的耗时
     *
     * @return 耗时（毫秒）
     */
    public double getCost() {
        return cost;
    }
}
