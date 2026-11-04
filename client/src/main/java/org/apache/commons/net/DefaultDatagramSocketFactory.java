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

/***
 * DefaultDatagramSocketFactory implements the DatagramSocketFactory
 * interface by simply wrapping the java.net.DatagramSocket
 * constructors.  It is the default DatagramSocketFactory used by
 * {@link org.apache.commons.net.DatagramSocketClient}
 *  implementations.
 *
 *
 * @see DatagramSocketFactory
 * @see DatagramSocketClient
 * @see DatagramSocketClient#setDatagramSocketFactory
 ***/

public class DefaultDatagramSocketFactory implements DatagramSocketFactory
{

    /***
     * Creates a DatagramSocket on the local host at the first available port.
     * @return a new DatagramSocket
     * @exception SocketException If the socket could not be created.
     ***/
    @Override
    public DatagramSocket createDatagramSocket() throws SocketException
    {
        return new DatagramSocket();
    }

    /***
     * Creates a DatagramSocket on the local host at a specified port.
     *
     * @param port The port to use for the socket.
     * @return a new DatagramSocket
     * @exception SocketException If the socket could not be created.
     ***/
    @Override
    public DatagramSocket createDatagramSocket(int port) throws SocketException
    {
        return new DatagramSocket(port);
    }

    /***
     * Creates a DatagramSocket at the specified address on the local host
     * at a specified port.
     *
     * @param port The port to use for the socket.
     * @param laddr  The local address to use.
     * @return a new DatagramSocket
     * @exception SocketException If the socket could not be created.
     ***/
    @Override
    public DatagramSocket createDatagramSocket(int port, InetAddress laddr)
    throws SocketException
    {
        return new DatagramSocket(port, laddr);
    }
}
