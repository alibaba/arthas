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
 * Implements the telnet terminal type option RFC 1091.
 ***/
public class TerminalTypeOptionHandler extends TelnetOptionHandler
{
    /***
     * Terminal type
     ***/
    private final String termType;

    /***
     * Terminal type option
     ***/
    protected static final int TERMINAL_TYPE = 24;

    /***
     * Send (for subnegotiation)
     ***/
    protected static final int TERMINAL_TYPE_SEND =  1;

    /***
     * Is (for subnegotiation)
     ***/
    protected static final int TERMINAL_TYPE_IS =  0;

    /***
     * Constructor for the TerminalTypeOptionHandler. Allows defining desired
     * initial setting for local/remote activation of this option and
     * behaviour in case a local/remote activation request for this
     * option is received.
     * <p>
     * @param termtype - terminal type that will be negotiated.
     * @param initlocal - if set to true, a WILL is sent upon connection.
     * @param initremote - if set to true, a DO is sent upon connection.
     * @param acceptlocal - if set to true, any DO request is accepted.
     * @param acceptremote - if set to true, any WILL request is accepted.
     ***/
    public TerminalTypeOptionHandler(String termtype,
                                boolean initlocal,
                                boolean initremote,
                                boolean acceptlocal,
                                boolean acceptremote)
    {
        super(TelnetOption.TERMINAL_TYPE, initlocal, initremote,
                                      acceptlocal, acceptremote);
        termType = termtype;
    }

    /***
     * Constructor for the TerminalTypeOptionHandler. Initial and accept
     * behaviour flags are set to false
     * <p>
     * @param termtype - terminal type that will be negotiated.
     ***/
    public TerminalTypeOptionHandler(String termtype)
    {
        super(TelnetOption.TERMINAL_TYPE, false, false, false, false);
        termType = termtype;
    }

    /***
     * Implements the abstract method of TelnetOptionHandler.
     * <p>
     * @param suboptionData - the sequence received, without IAC SB &amp; IAC SE
     * @param suboptionLength - the length of data in suboption_data
     * <p>
     * @return terminal type information
     ***/
    @Override
    public int[] answerSubnegotiation(int suboptionData[], int suboptionLength)
    {
        if ((suboptionData != null) && (suboptionLength > 1)
            && (termType != null))
        {
            if ((suboptionData[0] == TERMINAL_TYPE)
                && (suboptionData[1] == TERMINAL_TYPE_SEND))
            {
                int response[] = new int[termType.length() + 2];

                response[0] = TERMINAL_TYPE;
                response[1] = TERMINAL_TYPE_IS;

                for (int ii = 0; ii < termType.length(); ii++)
                {
                    response[ii + 2] = termType.charAt(ii);
                }

                return response;
            }
        }
        return null;
    }
}
