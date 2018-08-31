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
 * The TelnetOptionHandler class is the base class to be used
 * for implementing handlers for telnet options.
 * <p>
 * TelnetOptionHandler implements basic option handling
 * functionality and defines abstract methods that must be
 * implemented to define subnegotiation behaviour.
 ***/
public abstract class TelnetOptionHandler
{
    /***
     * Option code
     ***/
    private int optionCode = -1;

    /***
     * true if the option should be activated on the local side
     ***/
    private boolean initialLocal = false;

    /***
     * true if the option should be activated on the remote side
     ***/
    private boolean initialRemote = false;

    /***
     * true if the option should be accepted on the local side
     ***/
    private boolean acceptLocal = false;

    /***
     * true if the option should be accepted on the remote side
     ***/
    private boolean acceptRemote = false;

    /***
     * true if the option is active on the local side
     ***/
    private boolean doFlag = false;

    /***
     * true if the option is active on the remote side
     ***/
    private boolean willFlag = false;

    /***
     * Constructor for the TelnetOptionHandler. Allows defining desired
     * initial setting for local/remote activation of this option and
     * behaviour in case a local/remote activation request for this
     * option is received.
     * <p>
     * @param optcode - Option code.
     * @param initlocal - if set to true, a WILL is sent upon connection.
     * @param initremote - if set to true, a DO is sent upon connection.
     * @param acceptlocal - if set to true, any DO request is accepted.
     * @param acceptremote - if set to true, any WILL request is accepted.
     ***/
    public TelnetOptionHandler(int optcode,
                                boolean initlocal,
                                boolean initremote,
                                boolean acceptlocal,
                                boolean acceptremote)
    {
        optionCode = optcode;
        initialLocal = initlocal;
        initialRemote = initremote;
        acceptLocal = acceptlocal;
        acceptRemote = acceptremote;
    }


    /***
     * Returns the option code for this option.
     * <p>
     * @return Option code.
     ***/
    public int getOptionCode()
    {
        return (optionCode);
    }

    /***
     * Returns a boolean indicating whether to accept a DO
     * request coming from the other end.
     * <p>
     * @return true if a DO request shall be accepted.
     ***/
    public boolean getAcceptLocal()
    {
        return (acceptLocal);
    }

    /***
     * Returns a boolean indicating whether to accept a WILL
     * request coming from the other end.
     * <p>
     * @return true if a WILL request shall be accepted.
     ***/
    public boolean getAcceptRemote()
    {
        return (acceptRemote);
    }

    /***
     * Set behaviour of the option for DO requests coming from
     * the other end.
     * <p>
     * @param accept - if true, subsequent DO requests will be accepted.
     ***/
    public void setAcceptLocal(boolean accept)
    {
        acceptLocal = accept;
    }

    /***
     * Set behaviour of the option for WILL requests coming from
     * the other end.
     * <p>
     * @param accept - if true, subsequent WILL requests will be accepted.
     ***/
    public void setAcceptRemote(boolean accept)
    {
        acceptRemote = accept;
    }

    /***
     * Returns a boolean indicating whether to send a WILL request
     * to the other end upon connection.
     * <p>
     * @return true if a WILL request shall be sent upon connection.
     ***/
    public boolean getInitLocal()
    {
        return (initialLocal);
    }

    /***
     * Returns a boolean indicating whether to send a DO request
     * to the other end upon connection.
     * <p>
     * @return true if a DO request shall be sent upon connection.
     ***/
    public boolean getInitRemote()
    {
        return (initialRemote);
    }

    /***
     * Tells this option whether to send a WILL request upon connection.
     * <p>
     * @param init - if true, a WILL request will be sent upon subsequent
     * connections.
     ***/
    public void setInitLocal(boolean init)
    {
        initialLocal = init;
    }

    /***
     * Tells this option whether to send a DO request upon connection.
     * <p>
     * @param init - if true, a DO request will be sent upon subsequent
     * connections.
     ***/
    public void setInitRemote(boolean init)
    {
        initialRemote = init;
    }

    /***
     * Method called upon reception of a subnegotiation for this option
     * coming from the other end.
     * <p>
     * This implementation returns null, and
     * must be overridden by the actual TelnetOptionHandler to specify
     * which response must be sent for the subnegotiation request.
     * <p>
     * @param suboptionData - the sequence received, without IAC SB &amp; IAC SE
     * @param suboptionLength - the length of data in suboption_data
     * <p>
     * @return response to be sent to the subnegotiation sequence. TelnetClient
     * will add IAC SB &amp; IAC SE. null means no response
     ***/
    public int[] answerSubnegotiation(int suboptionData[], int suboptionLength) {
        return null;
    }

    /***
     * This method is invoked whenever this option is acknowledged active on
     * the local end (TelnetClient sent a WILL, remote side sent a DO).
     * The method is used to specify a subnegotiation sequence that will be
     * sent by TelnetClient when the option is activated.
     * <p>
     * This implementation returns null, and must be overriden by
     * the actual TelnetOptionHandler to specify
     * which response must be sent for the subnegotiation request.
     * @return subnegotiation sequence to be sent by TelnetClient. TelnetClient
     * will add IAC SB &amp; IAC SE. null means no subnegotiation.
     ***/
    public int[] startSubnegotiationLocal() {
        return null;
    }

    /***
     * This method is invoked whenever this option is acknowledged active on
     * the remote end (TelnetClient sent a DO, remote side sent a WILL).
     * The method is used to specify a subnegotiation sequence that will be
     * sent by TelnetClient when the option is activated.
     * <p>
     * This implementation returns null, and must be overriden by
     * the actual TelnetOptionHandler to specify
     * which response must be sent for the subnegotiation request.
     * @return subnegotiation sequence to be sent by TelnetClient. TelnetClient
     * will add IAC SB &amp; IAC SE. null means no subnegotiation.
     ***/
    public int[] startSubnegotiationRemote() {
        return null;
    }

    /***
     * Returns a boolean indicating whether a WILL request sent to the other
     * side has been acknowledged.
     * <p>
     * @return true if a WILL sent to the other side has been acknowledged.
     ***/
    boolean getWill()
    {
        return willFlag;
    }

    /***
     * Tells this option whether a WILL request sent to the other
     * side has been acknowledged (invoked by TelnetClient).
     * <p>
     * @param state - if true, a WILL request has been acknowledged.
     ***/
    void setWill(boolean state)
    {
        willFlag = state;
    }

    /***
     * Returns a boolean indicating whether a DO request sent to the other
     * side has been acknowledged.
     * <p>
     * @return true if a DO sent to the other side has been acknowledged.
     ***/
    boolean getDo()
    {
        return doFlag;
    }


    /***
     * Tells this option whether a DO request sent to the other
     * side has been acknowledged (invoked by TelnetClient).
     * <p>
     * @param state - if true, a DO request has been acknowledged.
     ***/
    void setDo(boolean state)
    {
        doFlag = state;
    }
}
