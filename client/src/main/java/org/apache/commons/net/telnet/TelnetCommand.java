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

/**
 * The TelnetCommand class cannot be instantiated and only serves as a
 * storehouse for telnet command constants.
 * @see org.apache.commons.net.telnet.Telnet
 * @see org.apache.commons.net.telnet.TelnetClient
 */

public final class TelnetCommand
{
    /*** The maximum value a command code can have.  This value is 255. ***/
    public static final int MAX_COMMAND_VALUE = 255;

    /*** Interpret As Command code.  Value is 255 according to RFC 854. ***/
    public static final int IAC = 255;

    /*** Don't use option code.  Value is 254 according to RFC 854. ***/
    public static final int DONT = 254;

    /*** Request to use option code.  Value is 253 according to RFC 854. ***/
    public static final int DO = 253;

    /*** Refuse to use option code.  Value is 252 according to RFC 854. ***/
    public static final int WONT = 252;

    /*** Agree to use option code.  Value is 251 according to RFC 854. ***/
    public static final int WILL = 251;

    /*** Start subnegotiation code.  Value is 250 according to RFC 854. ***/
    public static final int SB = 250;

    /*** Go Ahead code.  Value is 249 according to RFC 854. ***/
    public static final int GA = 249;

    /*** Erase Line code.  Value is 248 according to RFC 854. ***/
    public static final int EL = 248;

    /*** Erase Character code.  Value is 247 according to RFC 854. ***/
    public static final int EC = 247;

    /*** Are You There code.  Value is 246 according to RFC 854. ***/
    public static final int AYT = 246;

    /*** Abort Output code.  Value is 245 according to RFC 854. ***/
    public static final int AO = 245;

    /*** Interrupt Process code.  Value is 244 according to RFC 854. ***/
    public static final int IP = 244;

    /*** Break code.  Value is 243 according to RFC 854. ***/
    public static final int BREAK = 243;

    /*** Data mark code.  Value is 242 according to RFC 854. ***/
    public static final int DM = 242;

    /*** No Operation code.  Value is 241 according to RFC 854. ***/
    public static final int NOP = 241;

    /*** End subnegotiation code.  Value is 240 according to RFC 854. ***/
    public static final int SE = 240;

    /*** End of record code.  Value is 239. ***/
    public static final int EOR = 239;

    /*** Abort code.  Value is 238. ***/
    public static final int ABORT = 238;

    /*** Suspend process code.  Value is 237. ***/
    public static final int SUSP = 237;

    /*** End of file code.  Value is 236. ***/
    public static final int EOF = 236;

    /*** Synchronize code.  Value is 242. ***/
    public static final int SYNCH = 242;

    /*** String representations of commands. ***/
    private static final String __commandString[] = {
                "IAC", "DONT", "DO", "WONT", "WILL", "SB", "GA", "EL", "EC", "AYT",
                "AO", "IP", "BRK", "DMARK", "NOP", "SE", "EOR", "ABORT", "SUSP", "EOF"
            };

    private static final int __FIRST_COMMAND = IAC;
    private static final int __LAST_COMMAND = EOF;

    /***
     * Returns the string representation of the telnet protocol command
     * corresponding to the given command code.
     * <p>
     * @param code The command code of the telnet protocol command.
     * @return The string representation of the telnet protocol command.
     ***/
    public static final String getCommand(int code)
    {
        return __commandString[__FIRST_COMMAND - code];
    }

    /***
     * Determines if a given command code is valid.  Returns true if valid,
     * false if not.
     * <p>
     * @param code  The command code to test.
     * @return True if the command code is valid, false if not.
     **/
    public static final boolean isValidCommand(int code)
    {
        return (code <= __FIRST_COMMAND && code >= __LAST_COMMAND);
    }

    // Cannot be instantiated
    private TelnetCommand()
    { }
}
