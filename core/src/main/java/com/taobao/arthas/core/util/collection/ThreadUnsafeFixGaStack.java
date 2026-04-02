package com.taobao.arthas.core.util.collection;

import java.util.NoSuchElementException;

/**
 * 线程不安全的固定深度堆栈实现
 *
 * 该类实现了一个固定最大深度的堆栈数据结构，具有以下特点：
 * 1. 线程不安全：不提供任何同步机制，不适合在多线程环境中使用
 * 2. 固定深度：堆栈在创建时指定最大深度，不会动态扩容
 * 3. 高性能：相比 JDK 自带的 Stack 实现，固定堆栈深度的实现能提高约 10 倍的性能
 *
 * 性能优化原理：
 * - 使用数组而非链表存储元素，减少对象创建和垃圾回收开销
 * - 固定大小避免了动态扩容的性能损耗
 * - 简化了同步和边界检查逻辑
 *
 * Created by vlinux on 15/6/21.
 *
 * @param <E> 堆栈中存储的元素类型
 */
public class ThreadUnsafeFixGaStack<E> implements GaStack<E> {

    /**
     * 空堆栈的索引值
     * 当 current 等于此值时，表示堆栈为空
     */
    private final static int EMPTY_INDEX = -1;

    /**
     * 元素存储数组
     * 使用数组来存储堆栈中的元素，类型为 Object 以支持泛型
     */
    private final Object[] elementArray;

    /**
     * 堆栈的最大深度
     * 堆栈能容纳的最大元素数量，在构造时指定且不可更改
     */
    private final int max;

    /**
     * 当前栈顶元素的索引
     * 初始值为 EMPTY_INDEX，表示堆栈为空
     * 每次压栈时递增，每次弹栈时递减
     */
    private int current = EMPTY_INDEX;

    /**
     * 构造函数
     *
     * 创建一个具有指定最大深度的固定深度堆栈。
     *
     * @param max 堆栈的最大深度，必须大于 0
     */
    public ThreadUnsafeFixGaStack(int max) {
        this.max = max;
        // 初始化元素数组，大小为指定的最大深度
        this.elementArray = new Object[max];
    }

    /**
     * 检查是否可以执行压栈操作
     *
     * 如果堆栈已满（current 等于 max），则抛出异常。
     * 该方法在执行 push 操作前调用，用于确保堆栈不会溢出。
     *
     * @throws ArrayIndexOutOfBoundsException 如果堆栈已满
     */
    private void checkForPush() {
        // 检查堆栈是否已满
        if (current == max) {
            throw new ArrayIndexOutOfBoundsException();
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
     * @return 栈顶的元素
     * @throws NoSuchElementException 如果堆栈为空
     */
    @Override
    public E pop() {
        // 检查堆栈是否为空
        checkForPopOrPeek();
        // 返回当前栈顶元素，并将栈顶索引减 1
        return (E) elementArray[current--];
    }

    /**
     * 将元素压入堆栈
     *
     * 将指定元素压入堆栈顶部。如果堆栈已满，则抛出异常。
     * 该操作会使堆栈大小加 1。
     *
     * @param e 要压入堆栈的元素
     * @throws ArrayIndexOutOfBoundsException 如果堆栈已满
     */
    @Override
    public void push(E e) {
        // 检查堆栈是否已满
        checkForPush();
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
