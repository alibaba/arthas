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

package org.apache.commons.net.telnet;

/***
 * 简单Telnet选项处理器
 *
 * 这是一个简单的选项处理器，可以用于不需要子协商（subnegotiation）的Telnet选项。
 * 该类继承自TelnetOptionHandler，提供了基本的Telnet选项处理功能。
 *
 * Telnet协议允许客户端和服务器协商各种选项，这些选项可以控制会话的各个方面。
 * 该处理器用于处理那些不需要复杂数据交换的简单选项。
 ***/
public class SimpleOptionHandler extends TelnetOptionHandler
{
    /***
     * SimpleOptionHandler构造函数（完整参数版本）
     *
     * 允许定义该选项在本地/远程激活时的初始设置，
     * 以及接收到本地/远程激活请求时的处理行为。
     *
     * <p>
     * @param optcode - 选项码，标识要处理的Telnet选项
     * @param initlocal - 如果设置为true，建立连接时会发送WILL（表示本地希望启用该选项）
     * @param initremote - 如果设置为true，建立连接时会发送DO（表示希望远程启用该选项）
     * @param acceptlocal - 如果设置为true，接受任何DO请求（远程要求本地启用该选项）
     * @param acceptremote - 如果设置为true，接受任何WILL请求（本地希望启用该选项）
     ***/
    public SimpleOptionHandler(int optcode,
                                boolean initlocal,
                                boolean initremote,
                                boolean acceptlocal,
                                boolean acceptremote)
    {
        // 调用父类TelnetOptionHandler的构造函数，传递所有参数
        super(optcode, initlocal, initremote,
                                      acceptlocal, acceptremote);
    }

    /***
     * SimpleOptionHandler构造函数（简化版本）
     *
     * 使用默认值创建选项处理器，所有初始和接受行为标志都设置为false。
     * 这个构造函数用于创建一个被动的选项处理器，不会主动发起协商。
     *
     * <p>
     * @param optcode - 选项码，标识要处理的Telnet选项
     ***/
    public SimpleOptionHandler(int optcode)
    {
        // 调用父类构造函数，所有行为标志都设置为false
        super(optcode, false, false, false, false);
    }

}
