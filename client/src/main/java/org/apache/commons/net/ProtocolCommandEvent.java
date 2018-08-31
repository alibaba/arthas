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
 * There exists a large class of IETF protocols that work by sending an
 * ASCII text command and arguments to a server, and then receiving an
 * ASCII text reply.  For debugging and other purposes, it is extremely
 * useful to log or keep track of the contents of the protocol messages.
 * The ProtocolCommandEvent class coupled with the
 * {@link org.apache.commons.net.ProtocolCommandListener}
 *  interface facilitate this process.
 *
 *
 * @see ProtocolCommandListener
 * @see ProtocolCommandSupport
 ***/

public class ProtocolCommandEvent extends EventObject
{
    private static final long serialVersionUID = 403743538418947240L;

    private final int __replyCode;
    private final boolean __isCommand;
    private final String __message, __command;

    /***
     * Creates a ProtocolCommandEvent signalling a command was sent to
     * the server.  ProtocolCommandEvents created with this constructor
     * should only be sent after a command has been sent, but before the
     * reply has been received.
     *
     * @param source  The source of the event.
     * @param command The string representation of the command type sent, not
     *      including the arguments (e.g., "STAT" or "GET").
     * @param message The entire command string verbatim as sent to the server,
     *        including all arguments.
     ***/
    public ProtocolCommandEvent(Object source, String command, String message)
    {
        super(source);
        __replyCode = 0;
        __message = message;
        __isCommand = true;
        __command = command;
    }


    /***
     * Creates a ProtocolCommandEvent signalling a reply to a command was
     * received.  ProtocolCommandEvents created with this constructor
     * should only be sent after a complete command reply has been received
     * fromt a server.
     *
     * @param source  The source of the event.
     * @param replyCode The integer code indicating the natureof the reply.
     *   This will be the protocol integer value for protocols
     *   that use integer reply codes, or the reply class constant
     *   corresponding to the reply for protocols like POP3 that use
     *   strings like OK rather than integer codes (i.e., POP3Repy.OK).
     * @param message The entire reply as received from the server.
     ***/
    public ProtocolCommandEvent(Object source, int replyCode, String message)
    {
        super(source);
        __replyCode = replyCode;
        __message = message;
        __isCommand = false;
        __command = null;
    }

    /***
     * Returns the string representation of the command type sent (e.g., "STAT"
     * or "GET").  If the ProtocolCommandEvent is a reply event, then null
     * is returned.
     *
     * @return The string representation of the command type sent, or null
     *         if this is a reply event.
     ***/
    public String getCommand()
    {
        return __command;
    }


    /***
     * Returns the reply code of the received server reply.  Undefined if
     * this is not a reply event.
     *
     * @return The reply code of the received server reply.  Undefined if
     *         not a reply event.
     ***/
    public int getReplyCode()
    {
        return __replyCode;
    }

    /***
     * Returns true if the ProtocolCommandEvent was generated as a result
     * of sending a command.
     *
     * @return true If the ProtocolCommandEvent was generated as a result
     * of sending a command.  False otherwise.
     ***/
    public boolean isCommand()
    {
        return __isCommand;
    }

    /***
     * Returns true if the ProtocolCommandEvent was generated as a result
     * of receiving a reply.
     *
     * @return true If the ProtocolCommandEvent was generated as a result
     * of receiving a reply.  False otherwise.
     ***/
    public boolean isReply()
    {
        return !isCommand();
    }

    /***
     * Returns the entire message sent to or received from the server.
     * Includes the line terminator.
     *
     * @return The entire message sent to or received from the server.
     ***/
    public String getMessage()
    {
        return __message;
    }
}
