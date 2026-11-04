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

/***
 * DefaultSocketFactory implements the SocketFactory interface by
 * simply wrapping the java.net.Socket and java.net.ServerSocket
 * constructors.  It is the default SocketFactory used by
 * {@link org.apache.commons.net.SocketClient}
 * implementations.
 *
 *
 * @see SocketFactory
 * @see SocketClient
 * @see SocketClient#setSocketFactory
 ***/

public class DefaultSocketFactory extends SocketFactory
{
    /** The proxy to use when creating new sockets. */
    private final Proxy connProxy;

    /**
     * The default constructor.
     */
    public DefaultSocketFactory()
    {
        this(null);
    }

    /**
     * A constructor for sockets with proxy support.
     *
     * @param proxy The Proxy to use when creating new Sockets.
     * @since 3.2
     */
    public DefaultSocketFactory(Proxy proxy)
    {
        connProxy = proxy;
    }

    /**
     * Creates an unconnected Socket.
     *
     * @return A new unconnected Socket.
     * @exception IOException If an I/O error occurs while creating the Socket.
     * @since 3.2
     */
    @Override
    public Socket createSocket() throws IOException
    {
        if (connProxy != null)
        {
            return new Socket(connProxy);
        }
        return new Socket();
    }

    /***
     * Creates a Socket connected to the given host and port.
     *
     * @param host The hostname to connect to.
     * @param port The port to connect to.
     * @return A Socket connected to the given host and port.
     * @exception UnknownHostException  If the hostname cannot be resolved.
     * @exception IOException If an I/O error occurs while creating the Socket.
     ***/
    @Override
    public Socket createSocket(String host, int port)
    throws UnknownHostException, IOException
    {
        if (connProxy != null)
        {
            Socket s = new Socket(connProxy);
            s.connect(new InetSocketAddress(host, port));
            return s;
        }
        return new Socket(host, port);
    }

    /***
     * Creates a Socket connected to the given host and port.
     *
     * @param address The address of the host to connect to.
     * @param port The port to connect to.
     * @return A Socket connected to the given host and port.
     * @exception IOException If an I/O error occurs while creating the Socket.
     ***/
    @Override
    public Socket createSocket(InetAddress address, int port)
    throws IOException
    {
        if (connProxy != null)
        {
            Socket s = new Socket(connProxy);
            s.connect(new InetSocketAddress(address, port));
            return s;
        }
        return new Socket(address, port);
    }

    /***
     * Creates a Socket connected to the given host and port and
     * originating from the specified local address and port.
     *
     * @param host The hostname to connect to.
     * @param port The port to connect to.
     * @param localAddr  The local address to use.
     * @param localPort  The local port to use.
     * @return A Socket connected to the given host and port.
     * @exception UnknownHostException  If the hostname cannot be resolved.
     * @exception IOException If an I/O error occurs while creating the Socket.
     ***/
    @Override
    public Socket createSocket(String host, int port,
                               InetAddress localAddr, int localPort)
    throws UnknownHostException, IOException
    {
        if (connProxy != null)
        {
            Socket s = new Socket(connProxy);
            s.bind(new InetSocketAddress(localAddr, localPort));
            s.connect(new InetSocketAddress(host, port));
            return s;
        }
        return new Socket(host, port, localAddr, localPort);
    }

    /***
     * Creates a Socket connected to the given host and port and
     * originating from the specified local address and port.
     *
     * @param address The address of the host to connect to.
     * @param port The port to connect to.
     * @param localAddr  The local address to use.
     * @param localPort  The local port to use.
     * @return A Socket connected to the given host and port.
     * @exception IOException If an I/O error occurs while creating the Socket.
     ***/
    @Override
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddr, int localPort)
    throws IOException
    {
        if (connProxy != null)
        {
            Socket s = new Socket(connProxy);
            s.bind(new InetSocketAddress(localAddr, localPort));
            s.connect(new InetSocketAddress(address, port));
            return s;
        }
        return new Socket(address, port, localAddr, localPort);
    }

    /***
     * Creates a ServerSocket bound to a specified port.  A port
     * of 0 will create the ServerSocket on a system-determined free port.
     *
     * @param port  The port on which to listen, or 0 to use any free port.
     * @return A ServerSocket that will listen on a specified port.
     * @exception IOException If an I/O error occurs while creating
     *                        the ServerSocket.
     ***/
    public ServerSocket createServerSocket(int port) throws IOException
    {
        return new ServerSocket(port);
    }

    /***
     * Creates a ServerSocket bound to a specified port with a given
     * maximum queue length for incoming connections.  A port of 0 will
     * create the ServerSocket on a system-determined free port.
     *
     * @param port  The port on which to listen, or 0 to use any free port.
     * @param backlog  The maximum length of the queue for incoming connections.
     * @return A ServerSocket that will listen on a specified port.
     * @exception IOException If an I/O error occurs while creating
     *                        the ServerSocket.
     ***/
    public ServerSocket createServerSocket(int port, int backlog)
    throws IOException
    {
        return new ServerSocket(port, backlog);
    }

    /***
     * Creates a ServerSocket bound to a specified port on a given local
     * address with a given maximum queue length for incoming connections.
     * A port of 0 will
     * create the ServerSocket on a system-determined free port.
     *
     * @param port  The port on which to listen, or 0 to use any free port.
     * @param backlog  The maximum length of the queue for incoming connections.
     * @param bindAddr  The local address to which the ServerSocket should bind.
     * @return A ServerSocket that will listen on a specified port.
     * @exception IOException If an I/O error occurs while creating
     *                        the ServerSocket.
     ***/
    public ServerSocket createServerSocket(int port, int backlog,
                                           InetAddress bindAddr)
    throws IOException
    {
        return new ServerSocket(port, backlog, bindAddr);
    }
}
