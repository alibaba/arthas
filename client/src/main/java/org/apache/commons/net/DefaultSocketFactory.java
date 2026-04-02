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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

/**
 * DefaultSocketFactory 通过简单地包装 java.net.Socket 和 java.net.ServerSocket
 * 构造函数来实现 SocketFactory 接口。
 * 它是 {@link org.apache.commons.net.SocketClient}
 * 实现使用的默认 SocketFactory。
 *
 *
 * @see SocketFactory
 * @see SocketClient
 * @see SocketClient#setSocketFactory
 ***/

public class DefaultSocketFactory extends SocketFactory
{
    /**
     * 创建新套接字时使用的代理。
     */
    private final Proxy connProxy;

    /**
     * 默认构造函数。
     */
    public DefaultSocketFactory()
    {
        // 调用带参数的构造函数，传入 null 表示不使用代理
        this(null);
    }

    /**
     * 支持代理的套接字构造函数。
     *
     * @param proxy 创建新套接字时使用的代理。
     * @since 3.2
     */
    public DefaultSocketFactory(Proxy proxy)
    {
        // 保存代理配置
        connProxy = proxy;
    }

    /**
     * 创建一个未连接的套接字。
     *
     * @return 一个新的未连接的套接字。
     * @exception IOException 如果创建套接字时发生 I/O 错误。
     * @since 3.2
     */
    @Override
    public Socket createSocket() throws IOException
    {
        // 如果配置了代理，则使用代理创建套接字
        if (connProxy != null)
        {
            return new Socket(connProxy);
        }
        // 否则创建普通的套接字
        return new Socket();
    }

    /**
     * 创建一个连接到给定主机和端口的套接字。
     *
     * @param host 要连接的主机名。
     * @param port 要连接的端口。
     * @return 连接到给定主机和端口的套接字。
     * @exception UnknownHostException  如果无法解析主机名。
     * @exception IOException 如果创建套接字时发生 I/O 错误。
     ***/
    @Override
    public Socket createSocket(String host, int port)
    throws UnknownHostException, IOException
    {
        // 如果配置了代理
        if (connProxy != null)
        {
            // 使用代理创建套接字
            Socket s = new Socket(connProxy);
            // 连接到指定的主机和端口
            s.connect(new InetSocketAddress(host, port));
            return s;
        }
        // 否则直接创建并连接套接字
        return new Socket(host, port);
    }

    /**
     * 创建一个连接到给定主机和端口的套接字。
     *
     * @param address 要连接的主机地址。
     * @param port 要连接的端口。
     * @return 连接到给定主机和端口的套接字。
     * @exception IOException 如果创建套接字时发生 I/O 错误。
     ***/
    @Override
    public Socket createSocket(InetAddress address, int port)
    throws IOException
    {
        // 如果配置了代理
        if (connProxy != null)
        {
            // 使用代理创建套接字
            Socket s = new Socket(connProxy);
            // 连接到指定的地址和端口
            s.connect(new InetSocketAddress(address, port));
            return s;
        }
        // 否则直接创建并连接套接字
        return new Socket(address, port);
    }

    /**
     * 创建一个连接到给定主机和端口的套接字，并从指定的本地地址和端口发起。
     *
     * @param host 要连接的主机名。
     * @param port 要连接的端口。
     * @param localAddr  使用的本地地址。
     * @param localPort  使用的本地端口。
     * @return 连接到给定主机和端口的套接字。
     * @exception UnknownHostException  如果无法解析主机名。
     * @exception IOException 如果创建套接字时发生 I/O 错误。
     ***/
    @Override
    public Socket createSocket(String host, int port,
                               InetAddress localAddr, int localPort)
    throws UnknownHostException, IOException
    {
        // 如果配置了代理
        if (connProxy != null)
        {
            // 使用代理创建套接字
            Socket s = new Socket(connProxy);
            // 绑定到指定的本地地址和端口
            s.bind(new InetSocketAddress(localAddr, localPort));
            // 连接到指定的主机和端口
            s.connect(new InetSocketAddress(host, port));
            return s;
        }
        // 否则直接创建并连接套接字，同时指定本地地址和端口
        return new Socket(host, port, localAddr, localPort);
    }

    /**
     * 创建一个连接到给定主机和端口的套接字，并从指定的本地地址和端口发起。
     *
     * @param address 要连接的主机地址。
     * @param port 要连接的端口。
     * @param localAddr  使用的本地地址。
     * @param localPort  使用的本地端口。
     * @return 连接到给定主机和端口的套接字。
     * @exception IOException 如果创建套接字时发生 I/O 错误。
     ***/
    @Override
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddr, int localPort)
    throws IOException
    {
        // 如果配置了代理
        if (connProxy != null)
        {
            // 使用代理创建套接字
            Socket s = new Socket(connProxy);
            // 绑定到指定的本地地址和端口
            s.bind(new InetSocketAddress(localAddr, localPort));
            // 连接到指定的地址和端口
            s.connect(new InetSocketAddress(address, port));
            return s;
        }
        // 否则直接创建并连接套接字，同时指定本地地址和端口
        return new Socket(address, port, localAddr, localPort);
    }

    /**
     * 创建绑定到指定端口的服务器套接字。
     * 端口为 0 将在系统确定的空闲端口上创建服务器套接字。
     *
     * @param port  要监听的端口号，或 0 表示使用任何空闲端口。
     * @return 在指定端口上监听的服务器套接字。
     * @exception IOException 如果创建服务器套接字时发生 I/O 错误。
     ***/
    public ServerSocket createServerSocket(int port) throws IOException
    {
        return new ServerSocket(port);
    }

    /**
     * 创建绑定到指定端口的服务器套接字，并给定传入连接的最大队列长度。
     * 端口为 0 将在系统确定的空闲端口上创建服务器套接字。
     *
     * @param port  要监听的端口号，或 0 表示使用任何空闲端口。
     * @param backlog  传入连接队列的最大长度。
     * @return 在指定端口上监听的服务器套接字。
     * @exception IOException 如果创建服务器套接字时发生 I/O 错误。
     ***/
    public ServerSocket createServerSocket(int port, int backlog)
    throws IOException
    {
        return new ServerSocket(port, backlog);
    }

    /**
     * 创建绑定到给定本地地址的指定端口上的服务器套接字，
     * 并给定传入连接的最大队列长度。
     * 端口为 0 将在系统确定的空闲端口上创建服务器套接字。
     *
     * @param port  要监听的端口号，或 0 表示使用任何空闲端口。
     * @param backlog  传入连接队列的最大长度。
     * @param bindAddr  服务器套接字应该绑定的本地地址。
     * @return 在指定端口上监听的服务器套接字。
     * @exception IOException 如果创建服务器套接字时发生 I/O 错误。
     ***/
    public ServerSocket createServerSocket(int port, int backlog,
                                           InetAddress bindAddr)
    throws IOException
    {
        return new ServerSocket(port, backlog, bindAddr);
    }
}
