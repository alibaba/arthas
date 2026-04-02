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
import java.util.EventObject;

/***
 * 存在一大类IETF协议，它们通过向服务器发送ASCII文本命令和参数，
 * 然后接收ASCII文本回复来工作。为了调试和其他目的，
 * 记录或跟踪协议消息的内容非常有用。
 * ProtocolCommandEvent类与
 * {@link org.apache.commons.net.ProtocolCommandListener}
 * 接口配合使用，可简化此过程。
 *
 *
 * @see ProtocolCommandListener
 * @see ProtocolCommandSupport
 ***/

// ProtocolCommandEvent类：协议命令事件
// 该类继承EventObject，用于表示协议命令事件，包含命令发送或回复接收的相关信息
public class ProtocolCommandEvent extends EventObject
{
    // 序列化版本UID，用于序列化和反序列化
    private static final long serialVersionUID = 403743538418947240L;

    // 服务器回复的回复码
    // 对于使用整数回复码的协议，这是协议整数值
    // 对于使用字符串（如OK）的协议（如POP3），这是对应的回复类常量
    private final int __replyCode;

    // 标识是否为命令事件
    // 如果为true，表示这是发送命令的事件；如果为false，表示这是接收回复的事件
    private final boolean __isCommand;

    // 完整的消息内容（发送到服务器或从服务器接收的消息，包含行终止符）
    private final String __message, __command;

    /***
     * 创建一个ProtocolCommandEvent，表示已向服务器发送命令。
     * 使用此构造函数创建的ProtocolCommandEvents应该仅在命令发送后、
     * 接收到回复之前发送。
     *
     * @param source 事件的源对象
     * @param command 发送的命令类型的字符串表示，不包括参数
     *      （例如："STAT"或"GET"）
     * @param message 发送到服务器的完整命令字符串，包括所有参数
     ***/
    public ProtocolCommandEvent(Object source, String command, String message)
    {
        // 调用父类EventObject的构造函数，设置事件源
        super(source);

        // 初始化成员变量
        __replyCode = 0;       // 命令事件没有回复码，设为0
        __message = message;   // 保存完整的命令消息
        __isCommand = true;    // 标识这是一个命令事件
        __command = command;   // 保存命令名称
    }


    /***
     * 创建一个ProtocolCommandEvent，表示已接收到命令的回复。
     * 使用此构造函数创建的ProtocolCommandEvents应该仅在从服务器
     * 接收到完整的命令回复之后发送。
     *
     * @param source 事件的源对象
     * @param replyCode 表示回复性质的整数代码
     *   对于使用整数回复码的协议，这是协议整数值
     *   对于使用字符串（如OK）而不是整数代码的协议（如POP3），
     *   这是对应的回复类常量（即POP3Reply.OK）
     * @param message 从服务器接收的完整回复
     ***/
    public ProtocolCommandEvent(Object source, int replyCode, String message)
    {
        // 调用父类EventObject的构造函数，设置事件源
        super(source);

        // 初始化成员变量
        __replyCode = replyCode; // 保存回复码
        __message = message;      // 保存完整的回复消息
        __isCommand = false;      // 标识这是一个回复事件
        __command = null;         // 回复事件没有命令名称
    }

    /***
     * 返回已发送的命令类型的字符串表示（例如："STAT"或"GET"）。
     * 如果ProtocolCommandEvent是回复事件，则返回null。
     *
     * @return 已发送的命令类型的字符串表示，如果是回复事件则返回null
     ***/
    public String getCommand()
    {
        // 返回命令名称
        return __command;
    }


    /***
     * 返回接收到的服务器回复的回复码。
     * 如果这不是回复事件，则返回值未定义。
     *
     * @return 接收到的服务器回复的回复码。如果不是回复事件，则返回值未定义
     ***/
    public int getReplyCode()
    {
        // 返回回复码
        return __replyCode;
    }

    /***
     * 如果ProtocolCommandEvent是由于发送命令而生成的，则返回true。
     *
     * @return 如果ProtocolCommandEvent是由于发送命令而生成的，则返回true
     *         否则返回false
     ***/
    public boolean isCommand()
    {
        // 返回是否为命令事件
        return __isCommand;
    }

    /***
     * 如果ProtocolCommandEvent是由于接收回复而生成的，则返回true。
     *
     * @return 如果ProtocolCommandEvent是由于接收回复而生成的，则返回true
     *         否则返回false
     ***/
    public boolean isReply()
    {
        // 如果不是命令事件，就是回复事件
        return !isCommand();
    }

    /***
     * 返回发送到服务器或从服务器接收的完整消息。
     * 包含行终止符。
     *
     * @return 发送到服务器或从服务器接收的完整消息
     ***/
    public String getMessage()
    {
        // 返回完整的消息内容
        return __message;
    }
}

