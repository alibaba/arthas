package com.taobao.arthas.core.util.collection;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

import static java.lang.System.arraycopy;

/**
 * 线程不安全不固定栈深的堆栈实现<br/>
 * 比默认的实现带来3倍的性能提升
 * Created by vlinux on 15/6/21.
 *
 * @param <E>
 */
public class ThreadUnsafeGaStack<E> implements GaStack<E> {
    private static final Logger logger = LoggerFactory.getLogger(ThreadUnsafeGaStack.class);
    private final static int EMPTY_INDEX = -1;
    private final static int DEFAULT_STACK_DEEP = 12;

    private Object[] elementArray;
    private int current = EMPTY_INDEX;

    public ThreadUnsafeGaStack() {
        this(DEFAULT_STACK_DEEP);
    }

    private ThreadUnsafeGaStack(int stackSize) {
        this.elementArray = new Object[stackSize];
    }

    /**
     * 自动扩容<br/>
     * 当前堆栈最大深度不满足期望时会自动扩容(2倍扩容)
     *
     * @param expectDeep 期望堆栈深度
     */
    private void ensureCapacityInternal(int expectDeep) {
        final int currentStackSize = elementArray.length;
        if (elementArray.length <= expectDeep) {
            if (logger.isDebugEnabled()) {
                logger.debug("resize GaStack to double length: " + currentStackSize * 2 + " for thread: "
                        + Thread.currentThread().getName());
            }
            final Object[] newElementArray = new Object[currentStackSize * 2];
            arraycopy(elementArray, 0, newElementArray, 0, currentStackSize);
            this.elementArray = newElementArray;
        }
    }

    private void checkForPopOrPeek() {
        // stack is empty
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E pop() {
        try {
            checkForPopOrPeek();
            E res = (E) elementArray[current];
            elementArray[current] = null;
            current--;
            return res;
        } finally {
            if (current == EMPTY_INDEX && elementArray.length > DEFAULT_STACK_DEEP) {
                elementArray = new Object[DEFAULT_STACK_DEEP];
                if (logger.isDebugEnabled()) {
                    logger.debug("resize GaStack to default length for thread: " + Thread.currentThread().getName());
                }
            }
        }
    }

    @Override
    public void push(E e) {
        ensureCapacityInternal(current + 1);
        elementArray[++current] = e;
    }

    @Override
    public E peek() {
        checkForPopOrPeek();
        return (E) elementArray[current];
    }

    @Override
    public boolean isEmpty() {
        return current == EMPTY_INDEX;
    }

}
