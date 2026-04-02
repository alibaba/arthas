/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.net.util;

import java.io.Serializable;
import java.util.EventListener;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 监听器列表类
 *
 * 该类用于管理一组事件监听器（EventListener），提供了添加、删除和遍历监听器的功能。
 * 使用线程安全的 CopyOnWriteArrayList 作为底层数据结构，确保在多线程环境下的安全性。
 * 实现了 Serializable 接口，支持序列化操作。
 * 实现了 Iterable<EventListener> 接口，支持使用 for-each 循环遍历监听器。
 */
public class ListenerList implements Serializable, Iterable<EventListener>
{
    /**
     * 序列化版本号
     * 用于在序列化和反序列化过程中验证类的版本兼容性
     */
    private static final long serialVersionUID = -1934227607974228213L;

    /**
     * 监听器列表
     * 使用 CopyOnWriteArrayList 存储所有的事件监听器
     * 该数据结构是线程安全的，适合在多线程环境下使用
     * 写操作（添加、删除）会创建底层数组的新副本，读操作无需加锁
     */
    private final CopyOnWriteArrayList<EventListener> __listeners;

    /**
     * 构造函数
     *
     * 初始化一个空的监听器列表
     * 创建一个新的 CopyOnWriteArrayList 实例用于存储监听器
     */
    public ListenerList()
    {
        __listeners = new CopyOnWriteArrayList<EventListener>();
    }

    /**
     * 添加监听器
     *
     * 将指定的事件监听器添加到监听器列表中
     * 如果监听器已经存在于列表中，仍会再次添加（允许重复）
     *
     * @param listener 要添加的事件监听器，不能为 null
     */
    public void addListener(EventListener listener)
    {
            __listeners.add(listener);
    }

    /**
     * 移除监听器
     *
     * 从监听器列表中移除指定的事件监听器
     * 如果监听器在列表中存在多次，只会移除第一个匹配的实例
     *
     * @param listener 要移除的事件监听器
     */
    public  void removeListener(EventListener listener)
    {
            __listeners.remove(listener);
    }

    /**
     * 获取监听器数量
     *
     * 返回当前监听器列表中包含的监听器总数
     *
     * @return 监听器的数量
     */
    public int getListenerCount()
    {
        return __listeners.size();
    }

    /**
     * 返回监听器列表的迭代器
     *
     * 返回一个用于遍历监听器列表的迭代器
     * 该迭代器基于 CopyOnWriteArrayList 的快照创建，是线程安全的
     * 在迭代过程中，即使其他线程修改了监听器列表，也不会抛出 ConcurrentModificationException
     *
     * @return 用于遍历 EventListener 实例的迭代器
     * @since 2.0
     * TODO 需要检查这是否是一个好的防御策略
     */
    @Override
    public Iterator<EventListener> iterator() {
            return __listeners.iterator();
    }

}
