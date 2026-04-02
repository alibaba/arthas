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
 * DatagramSocketFactory 接口为程序员提供了一种控制数据报套接字创建的方式，
 * 并允许程序员提供自己的 DatagramSocket 实现，供所有派生自
 * {@link org.apache.commons.net.DatagramSocketClient}
 * 的类使用。
 * 这允许你提供自己的 DatagramSocket 实现，并在创建 DatagramSocket 之前
 * 执行安全检查或浏览器能力请求。
 *
 *
 ***/

public interface DatagramSocketFactory
{

    /**
     * 在本地主机的第一个可用端口上创建一个 DatagramSocket。
     * @return 创建的套接字。
     *
     * @exception SocketException 如果无法创建套接字。
     ***/
    public DatagramSocket createDatagramSocket() throws SocketException;

    /**
     * 在本地主机的指定端口上创建一个 DatagramSocket。
     *
     * @param port 套接字使用的端口号。
     * @return 创建的套接字。
     * @exception SocketException 如果无法创建套接字。
     ***/
    public DatagramSocket createDatagramSocket(int port) throws SocketException;

    /**
     * 在本地主机的指定地址和端口上创建一个 DatagramSocket。
     *
     * @param port 套接字使用的端口号。
     * @param laddr 使用的本地地址。
     * @return 创建的套接字。
     * @exception SocketException 如果无法创建套接字。
     ***/
    public DatagramSocket createDatagramSocket(int port, InetAddress laddr)
    throws SocketException;
}
