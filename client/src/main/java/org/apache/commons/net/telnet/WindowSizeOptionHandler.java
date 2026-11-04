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
 * Implements the telnet window size option RFC 1073.
 * @version $Id: WindowSizeOptionHandler.java 1697293 2015-08-24 01:01:00Z sebb $
 * @since 2.0
 ***/
public class WindowSizeOptionHandler extends TelnetOptionHandler
{
    /***
     * Horizontal Size
     ***/
    private int m_nWidth = 80;

    /***
     * Vertical Size
     ***/
    private int m_nHeight = 24;

    /***
     * Window size option
     ***/
    protected static final int WINDOW_SIZE = 31;

    /***
     * Constructor for the WindowSizeOptionHandler. Allows defining desired
     * initial setting for local/remote activation of this option and
     * behaviour in case a local/remote activation request for this
     * option is received.
     * <p>
     * @param nWidth - Window width.
     * @param nHeight - Window Height
     * @param initlocal - if set to true, a WILL is sent upon connection.
     * @param initremote - if set to true, a DO is sent upon connection.
     * @param acceptlocal - if set to true, any DO request is accepted.
     * @param acceptremote - if set to true, any WILL request is accepted.
     ***/
    public WindowSizeOptionHandler(
        int nWidth,
        int nHeight,
        boolean initlocal,
        boolean initremote,
        boolean acceptlocal,
        boolean acceptremote
    ) {
        super (
            TelnetOption.WINDOW_SIZE,
            initlocal,
            initremote,
            acceptlocal,
            acceptremote
        );

        m_nWidth = nWidth;
        m_nHeight = nHeight;
    }

    /***
     * Constructor for the WindowSizeOptionHandler. Initial and accept
     * behaviour flags are set to false
     * <p>
     * @param nWidth - Window width.
     * @param nHeight - Window Height
     ***/
    public WindowSizeOptionHandler(
        int nWidth,
        int nHeight
    ) {
        super (
            TelnetOption.WINDOW_SIZE,
            false,
            false,
            false,
            false
        );

        m_nWidth = nWidth;
        m_nHeight = nHeight;
    }

    /***
     * Implements the abstract method of TelnetOptionHandler.
     * This will send the client Height and Width to the server.
     * <p>
     * @return array to send to remote system
     ***/
    @Override
    public int[] startSubnegotiationLocal()
    {
        int nCompoundWindowSize = m_nWidth * 0x10000 + m_nHeight;
        int nResponseSize = 5;
        int nIndex;
        int nShift;
        int nTurnedOnBits;

        if ((m_nWidth % 0x100) == 0xFF) {
            nResponseSize += 1;
        }

        if ((m_nWidth / 0x100) == 0xFF) {
            nResponseSize += 1;
        }

        if ((m_nHeight % 0x100) == 0xFF) {
            nResponseSize += 1;
        }

        if ((m_nHeight / 0x100) == 0xFF) {
            nResponseSize += 1;
        }

        //
        // allocate response array
        //
        int response[] = new int[nResponseSize];

        //
        // Build response array.
        // ---------------------
        // 1. put option name.
        // 2. loop through Window size and fill the values,
        // 3.    duplicate 'ff' if needed.
        //

        response[0] = WINDOW_SIZE;                          // 1 //

        for (                                               // 2 //
            nIndex=1, nShift = 24;
            nIndex < nResponseSize;
            nIndex++, nShift -=8
        ) {
            nTurnedOnBits = 0xFF;
            nTurnedOnBits <<= nShift;
            response[nIndex] = (nCompoundWindowSize & nTurnedOnBits) >>> nShift;

            if (response[nIndex] == 0xff) {                 // 3 //
                nIndex++;
                response[nIndex] = 0xff;
            }
        }

        return response;
    }

}
