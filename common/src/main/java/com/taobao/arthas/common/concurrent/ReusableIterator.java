/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.taobao.arthas.common.concurrent;

import java.util.Iterator;

/**
 * 可重用的迭代器接口
 *
 * <p>该接口继承自标准的 {@link Iterator} 接口，扩展了迭代器的功能，
 * 使其能够在迭代过程中重置到初始状态，从而实现迭代器的重用。
 *
 * <p>传统的 Java {@link Iterator} 一旦遍历完成就无法再次使用，
 * 而该接口通过提供 {@link #rewind()} 方法，允许将迭代器重置到起始位置，
 * 这样就可以多次遍历同一个集合或序列，而无需创建新的迭代器实例。
 *
 * <p>这种设计在以下场景中特别有用：
 * <ul>
 *   <li>需要多次遍历同一个数据集合</li>
 *   <li>希望避免频繁创建迭代器实例带来的性能开销</li>
 *   <li>需要在迭代过程中回退到起始位置重新开始</li>
 *   <li>实现某种形式的回溯或重试逻辑</li>
 * </ul>
 *
 * <p><b>使用示例：</b>
 * <pre>{@code
 * ReusableIterator<String> iterator = new SomeReusableIteratorImpl<>(list);
 *
 * // 第一次遍历
 * while (iterator.hasNext()) {
 *     String item = iterator.next();
 *     // 处理元素
 * }
 *
 * // 重置迭代器
 * iterator.rewind();
 *
 * // 第二次遍历（从头开始）
 * while (iterator.hasNext()) {
 *     String item = iterator.next();
 *     // 再次处理元素
 * }
 * }</pre>
 *
 * <p><b>实现注意事项：</b>
 * <ul>
 *   <li>实现类需要维护迭代器的当前位置状态</li>
 *   <li>{@link #rewind()} 方法应该将迭代器重置到初始状态，就像刚刚创建一样</li>
 *   <li>如果底层集合在迭代过程中被修改，行为应该与标准 Iterator 一致</li>
 *   <li>考虑线程安全问题：如果迭代器可能被多线程访问，需要额外的同步机制</li>
 * </ul>
 *
 * @param <E> 迭代器返回的元素类型
 *
 * @author The Netty Project
 * @see Iterator
 * @see java.util.ListIterator
 */
public interface ReusableIterator<E> extends Iterator<E> {

    /**
     * 将迭代器重置到起始位置
     *
     * <p>调用此方法后，迭代器将回到初始状态，就像刚刚创建一样。
     * 再次调用 {@link #hasNext()} 和 {@link #next()} 方法时，
     * 将从集合的第一个元素开始重新遍历。
     *
     * <p><b>行为规范：</b>
     * <ul>
     *   <li>重置后，第一次调用 {@link #next()} 应该返回集合的第一个元素（如果集合非空）</li>
     *   <li>重置后，{@link #hasNext()} 应该反映集合的实际状态</li>
     *   <li>如果迭代器之前通过 {@link #remove()} 删除了元素，重置后这些元素的状态取决于具体实现</li>
     *   <li>可以多次调用此方法，多次调用应该与调用一次的效果相同</li>
     * </ul>
     *
     * <p><b>实现示例：</b>
     * <pre>{@code
     * public class MyReusableIterator<E> implements ReusableIterator<E> {
     *     private final List<E> list;
     *     private int currentIndex = 0;
     *
     *     public void rewind() {
     *         this.currentIndex = 0;  // 重置索引到起始位置
     *     }
     * }
     * }</pre>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>此方法不会修改底层的数据集合，只重置迭代器的内部状态</li>
     *   <li>如果底层集合在迭代过程中被其他方式修改，重置后的行为取决于具体实现</li>
     *   <li>在迭代过程中调用此方法是安全的，会立即中断当前的迭代过程</li>
     *   <li>实现类不需要检查集合是否为空，空集合的迭代器也可以被重置</li>
     * </ul>
     *
     * @throws UnsupportedOperationException 如果实现类不支持重置操作（可选）
     */
    void rewind();
}