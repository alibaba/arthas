package com.taobao.arthas.core.util.collection;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

import static java.lang.System.arraycopy;

/**
 * 线程不安全的动态扩容堆栈实现
 *
 * 该类实现了一个可动态扩容的堆栈数据结构，具有以下特点：
 * 1. 线程不安全：不提供任何同步机制，不适合在多线程环境中使用
 * 2. 动态扩容：当堆栈深度不足时自动扩容（2倍扩容策略）
 * 3. 自动缩容：当堆栈变空时，自动恢复到默认大小以释放内存
 * 4. 高性能：相比 JDK 默认的 Stack 实现，可带来约 3 倍的性能提升
 *
 * 性能优化原理：
 * - 使用数组存储元素，减少对象创建开销
 * - 2倍扩容策略平衡了内存使用和性能
 * - 空堆栈时自动缩容避免长期占用过多内存
 *
 * 使用场景：
 * - 适用于单线程环境下的堆栈操作
 * - 适用于堆栈大小波动较大的场景
 * - 不适合需要线程安全的场景
 *
 * Created by vlinux on 15/6/21.
 *
 * @param <E> 堆栈中存储的元素类型
 */
public class ThreadUnsafeGaStack<E> implements GaStack<E> {
    /**
     * 日志记录器
     * 用于记录堆栈扩容和缩容的调试信息
     */
    private static final Logger logger = LoggerFactory.getLogger(ThreadUnsafeGaStack.class);

    /**
     * 空堆栈的索引值
     * 当 current 等于此值时，表示堆栈为空
     */
    private final static int EMPTY_INDEX = -1;

    /**
     * 默认堆栈深度
     * 当创建新实例或堆栈变空恢复时使用的默认大小
     * 值为 12，是基于性能和内存平衡的选择
     */
    private final static int DEFAULT_STACK_DEEP = 12;

    /**
     * 元素存储数组
     * 使用数组来存储堆栈中的元素，类型为 Object 以支持泛型
     * 大小可以根据需要动态调整
     */
    private Object[] elementArray;

    /**
     * 当前栈顶元素的索引
     * 初始值为 EMPTY_INDEX，表示堆栈为空
     * 每次压栈时递增，每次弹栈时递减
     */
    private int current = EMPTY_INDEX;

    /**
     * 默认构造函数
     *
     * 创建一个使用默认深度（12）的堆栈实例
     */
    public ThreadUnsafeGaStack() {
        this(DEFAULT_STACK_DEEP);
    }

    /**
     * 私有构造函数
     *
     * 创建一个指定初始深度的堆栈实例。
     * 该构造函数为私有，仅内部使用。
     *
     * @param stackSize 堆栈的初始深度
     */
    private ThreadUnsafeGaStack(int stackSize) {
        this.elementArray = new Object[stackSize];
    }

    /**
     * 确保堆栈容量足够
     *
     * 当当前堆栈的最大深度不满足期望深度时，自动进行扩容。
     * 扩容策略：将当前容量扩大为原来的 2 倍。
     *
     * 扩容时机：在执行 push 操作前调用，确保有足够的空间存储新元素。
     *
     * @param expectDeep 期望的堆栈深度（即当前元素数量 + 1）
     */
    private void ensureCapacityInternal(int expectDeep) {
        final int currentStackSize = elementArray.length;
        // 如果当前数组长度小于等于期望深度，需要扩容
        if (elementArray.length <= expectDeep) {
            // 记录扩容调试信息
            if (logger.isDebugEnabled()) {
                logger.debug("resize GaStack to double length: " + currentStackSize * 2 + " for thread: "
                        + Thread.currentThread().getName());
            }
            // 创建新数组，大小为当前大小的 2 倍
            final Object[] newElementArray = new Object[currentStackSize * 2];
            // 将原数组中的所有元素复制到新数组
            arraycopy(elementArray, 0, newElementArray, 0, currentStackSize);
            // 将新数组赋值给当前数组引用
            this.elementArray = newElementArray;
        }
    }

    /**
     * 检查是否可以执行弹栈或查看栈顶操作
     *
     * 如果堆栈为空（current 等于 EMPTY_INDEX），则抛出异常。
     * 该方法在执行 pop 或 peek 操作前调用，用于确保堆栈不为空。
     *
     * @throws NoSuchElementException 如果堆栈为空
     */
    private void checkForPopOrPeek() {
        // 检查堆栈是否为空
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    /**
     * 弹出栈顶元素
     *
     * 移除并返回栈顶的元素。如果堆栈为空，则抛出异常。
     * 该操作会使堆栈大小减 1。
     *
     * 特殊行为：
     * - 在弹出元素后，会将原位置置为 null，帮助垃圾回收
     * - 如果堆栈变为空且当前大小大于默认大小，会自动缩容到默认大小
     *
     * @return 栈顶的元素
     * @throws NoSuchElementException 如果堆栈为空
     */
    @Override
    public E pop() {
        try {
            // 检查堆栈是否为空
            checkForPopOrPeek();
            // 获取栈顶元素
            E res = (E) elementArray[current];
            // 将栈顶位置置为 null，帮助垃圾回收
            elementArray[current] = null;
            // 栈顶索引减 1
            current--;
            return res;
        } finally {
            // 如果堆栈变为空且当前数组大小大于默认大小，则缩容到默认大小
            // 这样可以避免堆栈在长时间使用后占用过多内存
            if (current == EMPTY_INDEX && elementArray.length > DEFAULT_STACK_DEEP) {
                elementArray = new Object[DEFAULT_STACK_DEEP];
                // 记录缩容调试信息
                if (logger.isDebugEnabled()) {
                    logger.debug("resize GaStack to default length for thread: " + Thread.currentThread().getName());
                }
            }
        }
    }

    /**
     * 将元素压入堆栈
     *
     * 将指定元素压入堆栈顶部。
     * 该操作会使堆栈大小加 1。
     *
     * 如果当前堆栈已满，会自动扩容为原来的 2 倍。
     *
     * @param e 要压入堆栈的元素
     */
    @Override
    public void push(E e) {
        // 确保堆栈有足够的容量
        ensureCapacityInternal(current + 1);
        // 先将栈顶索引加 1，然后将元素存入该位置
        elementArray[++current] = e;
    }

    /**
     * 查看栈顶元素但不移除
     *
     * 返回栈顶的元素但不将其从堆栈中移除。
     * 如果堆栈为空，则抛出异常。
     *
     * @return 栈顶的元素
     * @throws NoSuchElementException 如果堆栈为空
     */
    @Override
    public E peek() {
        // 检查堆栈是否为空
        checkForPopOrPeek();
        // 返回当前栈顶元素，不修改索引
        return (E) elementArray[current];
    }

    /**
     * 判断堆栈是否为空
     *
     * @return 如果堆栈为空返回 true，否则返回 false
     */
    @Override
    public boolean isEmpty() {
        return current == EMPTY_INDEX;
    }

}
