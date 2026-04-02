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

package org.apache.commons.net;

import java.io.Serializable;
import java.util.EventListener;

import org.apache.commons.net.util.ListenerList;

/***
 * ProtocolCommandSupport是一个便利类，用于管理ProtocolCommandListener列表
 * 和触发ProtocolCommandEvents。您可以简单地将ProtocolCommandEvent触发
 * 和监听器注册/注销任务委托给此类。
 *
 *
 * @see ProtocolCommandEvent
 * @see ProtocolCommandListener
 ***/

// ProtocolCommandSupport类：协议命令支持类
// 该类实现了Serializable接口，用于管理协议命令监听器列表和触发协议命令事件
// 它提供了一个统一的接口来管理监听器和分发事件
public class ProtocolCommandSupport implements Serializable
{
    // 序列化版本UID，用于序列化和反序列化
    private static final long serialVersionUID = -8017692739988399978L;

    // 事件源对象
    // 所有生成的ProtocolCommandEvents都使用此对象作为事件源
    private final Object __source;

    // 监听器列表
    // 用于存储所有注册的ProtocolCommandListener监听器
    private final ListenerList __listeners;

    /***
     * 使用指定的源作为ProtocolCommandEvents的源创建ProtocolCommandSupport实例。
     *
     * @param source 用于所有生成的ProtocolCommandEvents的源对象
     ***/
    public ProtocolCommandSupport(Object source)
    {
        // 创建监听器列表对象
        __listeners = new ListenerList();

        // 保存事件源对象
        __source = source;
    }


    /***
     * 触发ProtocolCommandEvent，向所有已注册的监听器发送命令信号，
     * 调用它们的
     * {@link org.apache.commons.net.ProtocolCommandListener#protocolCommandSent protocolCommandSent() }
     * 方法。
     *
     * @param command 发送的命令类型的字符串表示，不包括参数
     *      （例如："STAT"或"GET"）
     * @param message 发送到服务器的完整命令字符串，包括所有参数
     ***/
    public void fireCommandSent(String command, String message)
    {
        // 声明协议命令事件对象
        ProtocolCommandEvent event;

        // 创建新的协议命令事件对象，用于表示命令已发送
        // 使用源对象、命令名称和完整消息作为参数
        event = new ProtocolCommandEvent(__source, command, message);

        // 遍历所有已注册的监听器
        for (EventListener listener : __listeners)
        {
           // 将监听器转换为ProtocolCommandListener类型
           // 调用监听器的protocolCommandSent方法，通知监听器命令已发送
           ((ProtocolCommandListener)listener).protocolCommandSent(event);
        }
    }

    /***
     * 触发ProtocolCommandEvent，向所有已注册的监听器发送接收到命令回复的信号，
     * 调用它们的
     * {@link org.apache.commons.net.ProtocolCommandListener#protocolReplyReceived protocolReplyReceived() }
     * 方法。
     *
     * @param replyCode 表示回复性质的整数代码
     *   对于使用整数回复码的协议，这是协议整数值
     *   对于使用字符串（如OK）而不是整数代码的协议（如POP3），
     *   这是对应的回复类常量（即POP3Reply.OK）
     * @param message 从服务器接收的完整回复
     ***/
    public void fireReplyReceived(int replyCode, String message)
    {
        // 声明协议命令事件对象
        ProtocolCommandEvent event;

        // 创建新的协议命令事件对象，用于表示接收到回复
        // 使用源对象、回复码和完整消息作为参数
        event = new ProtocolCommandEvent(__source, replyCode, message);

        // 遍历所有已注册的监听器
        for (EventListener listener : __listeners)
        {
           // 将监听器转换为ProtocolCommandListener类型
           // 调用监听器的protocolReplyReceived方法，通知监听器已接收到回复
           ((ProtocolCommandListener)listener).protocolReplyReceived(event);
        }
    }

    /***
     * 添加ProtocolCommandListener监听器。
     *
     * @param listener 要添加的ProtocolCommandListener监听器对象
     ***/
    public void addProtocolCommandListener(ProtocolCommandListener listener)
    {
        // 将监听器添加到监听器列表中
        __listeners.addListener(listener);
    }

    /***
     * 移除ProtocolCommandListener监听器。
     *
     * @param listener 要移除的ProtocolCommandListener监听器对象
     ***/
    public void removeProtocolCommandListener(ProtocolCommandListener listener)
    {
        // 从监听器列表中移除指定的监听器
        __listeners.removeListener(listener);
    }


    /***
     * 返回当前已注册的ProtocolCommandListener监听器数量。
     *
     * @return 当前已注册的ProtocolCommandListener监听器数量
     ***/
    public int getListenerCount()
    {
        // 返回监听器列表中的监听器数量
        return __listeners.getListenerCount();
    }

}

