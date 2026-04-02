package com.taobao.arthas.core.util.collection;

/**
 * 堆栈接口
 * 定义了基本的堆栈操作，包括入栈、出栈、查看栈顶元素和判断堆栈是否为空
 *
 * Created by vlinux on 15/6/21.
 * @param <E> 堆栈中元素的类型
 */
public interface GaStack<E> {

    /**
     * 出栈操作
     * 移除并返回堆栈顶部的元素
     *
     * @return 堆栈顶部的元素
     */
    E pop();

    /**
     * 入栈操作
     * 将元素压入堆栈顶部
     *
     * @param e 要压入堆栈的元素
     */
    void push(E e);

    /**
     * 查看栈顶元素
     * 返回堆栈顶部的元素，但不移除它
     *
     * @return 堆栈顶部的元素
     */
    E peek();

    /**
     * 判断堆栈是否为空
     *
     * @return 如果堆栈为空返回true，否则返回false
     */
    boolean isEmpty();

}
