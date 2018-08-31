package com.taobao.arthas.core.util.collection;

/**
 * 堆栈
 * Created by vlinux on 15/6/21.
 * @param <E>
 */
public interface GaStack<E> {

    E pop();

    void push(E e);

    E peek();

    boolean isEmpty();

}
