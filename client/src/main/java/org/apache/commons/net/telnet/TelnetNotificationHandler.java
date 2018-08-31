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
 * The TelnetNotificationHandler interface can be used to handle
 * notification of options negotiation commands received on a telnet
 * session.
 * <p>
 * The user can implement this interface and register a
 * TelnetNotificationHandler by using the registerNotificationHandler()
 * of TelnetClient to be notified of option negotiation commands.
 ***/

public interface TelnetNotificationHandler
{
    /***
     * The remote party sent a DO command.
     ***/
    public static final int RECEIVED_DO =   1;

    /***
     * The remote party sent a DONT command.
     ***/
    public static final int RECEIVED_DONT = 2;

    /***
     * The remote party sent a WILL command.
     ***/
    public static final int RECEIVED_WILL = 3;

    /***
     * The remote party sent a WONT command.
     ***/
    public static final int RECEIVED_WONT = 4;

    /***
     * The remote party sent a COMMAND.
     * @since 2.2
     ***/
    public static final int RECEIVED_COMMAND = 5;

    /***
     * Callback method called when TelnetClient receives an
     * command or option negotiation command
     *
     * @param negotiation_code - type of (negotiation) command received
     * (RECEIVED_DO, RECEIVED_DONT, RECEIVED_WILL, RECEIVED_WONT, RECEIVED_COMMAND)
     *
     * @param option_code - code of the option negotiated, or the command code itself (e.g. NOP).
     ***/
    public void receivedNegotiation(int negotiation_code, int option_code);
}
