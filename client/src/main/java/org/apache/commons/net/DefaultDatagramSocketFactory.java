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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * DefaultDatagramSocketFactory 通过简单地包装 java.net.DatagramSocket
 * 构造函数来实现 DatagramSocketFactory 接口。
 * 它是 {@link org.apache.commons.net.DatagramSocketClient}
 * 实现使用的默认 DatagramSocketFactory。
 *
 *
 * @see DatagramSocketFactory
 * @see DatagramSocketClient
 * @see DatagramSocketClient#setDatagramSocketFactory
 ***/

public class DefaultDatagramSocketFactory implements DatagramSocketFactory
{

    /**
     * 在本地主机的第一个可用端口上创建一个 DatagramSocket。
     * @return 一个新的 DatagramSocket 实例。
     * @exception SocketException 如果无法创建套接字。
     ***/
    @Override
    public DatagramSocket createDatagramSocket() throws SocketException
    {
        // 调用无参构造函数创建数据报套接字，系统会自动分配可用端口
        return new DatagramSocket();
    }

    /**
     * 在本地主机的指定端口上创建一个 DatagramSocket。
     *
     * @param port 套接字使用的端口号。
     * @return 一个新的 DatagramSocket 实例。
     * @exception SocketException 如果无法创建套接字。
     ***/
    @Override
    public DatagramSocket createDatagramSocket(int port) throws SocketException
    {
        // 调用带端口号的构造函数创建数据报套接字
        return new DatagramSocket(port);
    }

    /**
     * 在本地主机的指定地址和端口上创建一个 DatagramSocket。
     *
     * @param port 套接字使用的端口号。
     * @param laddr 使用的本地地址。
     * @return 一个新的 DatagramSocket 实例。
     * @exception SocketException 如果无法创建套接字。
     ***/
    @Override
    public DatagramSocket createDatagramSocket(int port, InetAddress laddr)
    throws SocketException
    {
        // 调用带端口号和本地地址的构造函数创建数据报套接字
        return new DatagramSocket(port, laddr);
    }
}
