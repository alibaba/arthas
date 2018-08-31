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
 * Implements the telnet suppress go ahead option RFC 858.
 ***/
public class SuppressGAOptionHandler extends TelnetOptionHandler
{
    /***
     * Constructor for the SuppressGAOptionHandler. Allows defining desired
     * initial setting for local/remote activation of this option and
     * behaviour in case a local/remote activation request for this
     * option is received.
     * <p>
     * @param initlocal - if set to true, a WILL is sent upon connection.
     * @param initremote - if set to true, a DO is sent upon connection.
     * @param acceptlocal - if set to true, any DO request is accepted.
     * @param acceptremote - if set to true, any WILL request is accepted.
     ***/
    public SuppressGAOptionHandler(boolean initlocal, boolean initremote,
                                boolean acceptlocal, boolean acceptremote)
    {
        super(TelnetOption.SUPPRESS_GO_AHEAD, initlocal, initremote,
                                      acceptlocal, acceptremote);
    }

    /***
     * Constructor for the SuppressGAOptionHandler. Initial and accept
     * behaviour flags are set to false
     ***/
    public SuppressGAOptionHandler()
    {
        super(TelnetOption.SUPPRESS_GO_AHEAD, false, false, false, false);
    }

}
