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
import java.util.EventListener;

/***
 * There exists a large class of IETF protocols that work by sending an
 * ASCII text command and arguments to a server, and then receiving an
 * ASCII text reply.  For debugging and other purposes, it is extremely
 * useful to log or keep track of the contents of the protocol messages.
 * The ProtocolCommandListener interface coupled with the
 * {@link ProtocolCommandEvent} class facilitate this process.
 * <p>
 * To receive ProtocolCommandEvents, you merely implement the
 * ProtocolCommandListener interface and register the class as a listener
 * with a ProtocolCommandEvent source such as
 * {@link org.apache.commons.net.ftp.FTPClient}.
 *
 *
 * @see ProtocolCommandEvent
 * @see ProtocolCommandSupport
 ***/

public interface ProtocolCommandListener extends EventListener
{

    /***
     * This method is invoked by a ProtocolCommandEvent source after
     * sending a protocol command to a server.
     *
     * @param event The ProtocolCommandEvent fired.
     ***/
    public void protocolCommandSent(ProtocolCommandEvent event);

    /***
     * This method is invoked by a ProtocolCommandEvent source after
     * receiving a reply from a server.
     *
     * @param event The ProtocolCommandEvent fired.
     ***/
    public void protocolReplyReceived(ProtocolCommandEvent event);

}
