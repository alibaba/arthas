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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.net.SocketClient;

class Telnet extends SocketClient
{
    static final boolean debug =  /*true;*/ false;

    static final boolean debugoptions =  /*true;*/ false;

    static final byte[] _COMMAND_DO = {
                                          (byte)TelnetCommand.IAC, (byte)TelnetCommand.DO
                                      };

    static final byte[] _COMMAND_DONT = {
                                            (byte)TelnetCommand.IAC, (byte)TelnetCommand.DONT
                                        };

    static final byte[] _COMMAND_WILL = {
                                            (byte)TelnetCommand.IAC, (byte)TelnetCommand.WILL
                                        };

    static final byte[] _COMMAND_WONT = {
                                            (byte)TelnetCommand.IAC, (byte)TelnetCommand.WONT
                                        };

    static final byte[] _COMMAND_SB = {
                                          (byte)TelnetCommand.IAC, (byte)TelnetCommand.SB
                                      };

    static final byte[] _COMMAND_SE = {
                                          (byte)TelnetCommand.IAC, (byte)TelnetCommand.SE
                                      };

    static final int _WILL_MASK = 0x01, _DO_MASK = 0x02,
                                  _REQUESTED_WILL_MASK = 0x04, _REQUESTED_DO_MASK = 0x08;

    /* public */
    static final int DEFAULT_PORT =  23;

    int[] _doResponse, _willResponse, _options;

    /* TERMINAL-TYPE option (start)*/
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
     * Is sequence (for subnegotiation)
     ***/
    static final byte[] _COMMAND_IS = {
                                          (byte) TERMINAL_TYPE, (byte) TERMINAL_TYPE_IS
                                      };

    /***
     * Terminal type
     ***/
    private String terminalType = null;
    /* TERMINAL-TYPE option (end)*/

    /* open TelnetOptionHandler functionality (start)*/
    /***
     * Array of option handlers
     ***/
    private final TelnetOptionHandler optionHandlers[];

    /* open TelnetOptionHandler functionality (end)*/

    /* Code Section added for supporting AYT (start)*/
    /***
     * AYT sequence
     ***/
    static final byte[] _COMMAND_AYT = {
                                          (byte) TelnetCommand.IAC, (byte) TelnetCommand.AYT
                                       };

    /***
     * monitor to wait for AYT
     ***/
    private final Object aytMonitor = new Object();

    /***
     * flag for AYT
     ***/
    private volatile boolean aytFlag = true;
    /* Code Section added for supporting AYT (end)*/

    /***
     * The stream on which to spy
     ***/
    private volatile OutputStream spyStream = null;

    /***
     * The notification handler
     ***/
    private TelnetNotificationHandler __notifhand = null;
    /***
     * Empty Constructor
     ***/
    Telnet()
    {
        setDefaultPort(DEFAULT_PORT);
        _doResponse = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        _willResponse = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        _options = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        optionHandlers =
            new TelnetOptionHandler[TelnetOption.MAX_OPTION_VALUE + 1];
    }

    /* TERMINAL-TYPE option (start)*/
    /***
     * This constructor lets you specify the terminal type.
     *
     * @param termtype - terminal type to be negotiated (ej. VT100)
     ***/
    Telnet(String termtype)
    {
        setDefaultPort(DEFAULT_PORT);
        _doResponse = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        _willResponse = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        _options = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        terminalType = termtype;
        optionHandlers =
            new TelnetOptionHandler[TelnetOption.MAX_OPTION_VALUE + 1];
    }
    /* TERMINAL-TYPE option (end)*/

    /***
     * Looks for the state of the option.
     *
     * @return returns true if a will has been acknowledged
     *
     * @param option - option code to be looked up.
     ***/
    boolean _stateIsWill(int option)
    {
        return ((_options[option] & _WILL_MASK) != 0);
    }

    /***
     * Looks for the state of the option.
     *
     * @return returns true if a wont has been acknowledged
     *
     * @param option - option code to be looked up.
     ***/
    boolean _stateIsWont(int option)
    {
        return !_stateIsWill(option);
    }

    /***
     * Looks for the state of the option.
     *
     * @return returns true if a do has been acknowledged
     *
     * @param option - option code to be looked up.
     ***/
    boolean _stateIsDo(int option)
    {
        return ((_options[option] & _DO_MASK) != 0);
    }

    /***
     * Looks for the state of the option.
     *
     * @return returns true if a dont has been acknowledged
     *
     * @param option - option code to be looked up.
     ***/
    boolean _stateIsDont(int option)
    {
        return !_stateIsDo(option);
    }

    /***
     * Looks for the state of the option.
     *
     * @return returns true if a will has been reuqested
     *
     * @param option - option code to be looked up.
     ***/
    boolean _requestedWill(int option)
    {
        return ((_options[option] & _REQUESTED_WILL_MASK) != 0);
    }

    /***
     * Looks for the state of the option.
     *
     * @return returns true if a wont has been reuqested
     *
     * @param option - option code to be looked up.
     ***/
    boolean _requestedWont(int option)
    {
        return !_requestedWill(option);
    }

    /***
     * Looks for the state of the option.
     *
     * @return returns true if a do has been reuqested
     *
     * @param option - option code to be looked up.
     ***/
    boolean _requestedDo(int option)
    {
        return ((_options[option] & _REQUESTED_DO_MASK) != 0);
    }

    /***
     * Looks for the state of the option.
     *
     * @return returns true if a dont has been reuqested
     *
     * @param option - option code to be looked up.
     ***/
    boolean _requestedDont(int option)
    {
        return !_requestedDo(option);
    }

    /***
     * Sets the state of the option.
     *
     * @param option - option code to be set.
     * @throws IOException
     ***/
    void _setWill(int option) throws IOException
    {
        _options[option] |= _WILL_MASK;

        /* open TelnetOptionHandler functionality (start)*/
        if (_requestedWill(option))
        {
            if (optionHandlers[option] != null)
            {
                optionHandlers[option].setWill(true);

                int subneg[] =
                    optionHandlers[option].startSubnegotiationLocal();

                if (subneg != null)
                {
                    _sendSubnegotiation(subneg);
                }
            }
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /***
     * Sets the state of the option.
     *
     * @param option - option code to be set.
     * @throws IOException
     ***/
    void _setDo(int option) throws IOException
    {
        _options[option] |= _DO_MASK;

        /* open TelnetOptionHandler functionality (start)*/
        if (_requestedDo(option))
        {
            if (optionHandlers[option] != null)
            {
                optionHandlers[option].setDo(true);

                int subneg[] =
                    optionHandlers[option].startSubnegotiationRemote();

                if (subneg != null)
                {
                    _sendSubnegotiation(subneg);
                }
            }
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /***
     * Sets the state of the option.
     *
     * @param option - option code to be set.
     ***/
    void _setWantWill(int option)
    {
        _options[option] |= _REQUESTED_WILL_MASK;
    }

    /***
     * Sets the state of the option.
     *
     * @param option - option code to be set.
     ***/
    void _setWantDo(int option)
    {
        _options[option] |= _REQUESTED_DO_MASK;
    }

    /***
     * Sets the state of the option.
     *
     * @param option - option code to be set.
     ***/
    void _setWont(int option)
    {
        _options[option] &= ~_WILL_MASK;

        /* open TelnetOptionHandler functionality (start)*/
        if (optionHandlers[option] != null)
        {
            optionHandlers[option].setWill(false);
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /***
     * Sets the state of the option.
     *
     * @param option - option code to be set.
     ***/
    void _setDont(int option)
    {
        _options[option] &= ~_DO_MASK;

        /* open TelnetOptionHandler functionality (start)*/
        if (optionHandlers[option] != null)
        {
            optionHandlers[option].setDo(false);
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /***
     * Sets the state of the option.
     *
     * @param option - option code to be set.
     ***/
    void _setWantWont(int option)
    {
        _options[option] &= ~_REQUESTED_WILL_MASK;
    }

    /***
     * Sets the state of the option.
     *
     * @param option - option code to be set.
     ***/
    void _setWantDont(int option)
    {
        _options[option] &= ~_REQUESTED_DO_MASK;
    }

    /**
     * Processes a COMMAND.
     *
     * @param command - option code to be set.
     **/
    void _processCommand(int command)
    {
        if (debugoptions)
        {
            System.err.println("RECEIVED COMMAND: " + command);
        }

        if (__notifhand != null)
        {
            __notifhand.receivedNegotiation(
                TelnetNotificationHandler.RECEIVED_COMMAND, command);
        }
    }

    /**
     * Processes a DO request.
     *
     * @param option - option code to be set.
     * @throws IOException - Exception in I/O.
     **/
    void _processDo(int option) throws IOException
    {
        if (debugoptions)
        {
            System.err.println("RECEIVED DO: "
                + TelnetOption.getOption(option));
        }

        if (__notifhand != null)
        {
            __notifhand.receivedNegotiation(
                TelnetNotificationHandler.RECEIVED_DO,
                option);
        }

        boolean acceptNewState = false;


        /* open TelnetOptionHandler functionality (start)*/
        if (optionHandlers[option] != null)
        {
            acceptNewState = optionHandlers[option].getAcceptLocal();
        }
        else
        {
        /* open TelnetOptionHandler functionality (end)*/
            /* TERMINAL-TYPE option (start)*/
            if (option == TERMINAL_TYPE)
            {
                if ((terminalType != null) && (terminalType.length() > 0))
                {
                    acceptNewState = true;
                }
            }
            /* TERMINAL-TYPE option (end)*/
        /* open TelnetOptionHandler functionality (start)*/
        }
        /* open TelnetOptionHandler functionality (end)*/

        if (_willResponse[option] > 0)
        {
            --_willResponse[option];
            if (_willResponse[option] > 0 && _stateIsWill(option))
            {
                --_willResponse[option];
            }
        }

        if (_willResponse[option] == 0)
        {
            if (_requestedWont(option))
            {

                switch (option)
                {

                default:
                    break;

                }


                if (acceptNewState)
                {
                    _setWantWill(option);
                    _sendWill(option);
                }
                else
                {
                    ++_willResponse[option];
                    _sendWont(option);
                }
            }
            else
            {
                // Other end has acknowledged option.

                switch (option)
                {

                default:
                    break;

                }

            }
        }

        _setWill(option);
    }

    /**
     * Processes a DONT request.
     *
     * @param option - option code to be set.
     * @throws IOException - Exception in I/O.
     **/
    void _processDont(int option) throws IOException
    {
        if (debugoptions)
        {
            System.err.println("RECEIVED DONT: "
                + TelnetOption.getOption(option));
        }
        if (__notifhand != null)
        {
            __notifhand.receivedNegotiation(
                TelnetNotificationHandler.RECEIVED_DONT,
                option);
        }
        if (_willResponse[option] > 0)
        {
            --_willResponse[option];
            if (_willResponse[option] > 0 && _stateIsWont(option))
            {
                --_willResponse[option];
            }
        }

        if (_willResponse[option] == 0 && _requestedWill(option))
        {

            switch (option)
            {

            default:
                break;

            }

            /* FIX for a BUG in the negotiation (start)*/
            if ((_stateIsWill(option)) || (_requestedWill(option)))
            {
                _sendWont(option);
            }

            _setWantWont(option);
            /* FIX for a BUG in the negotiation (end)*/
        }

        _setWont(option);
    }


    /**
     * Processes a WILL request.
     *
     * @param option - option code to be set.
     * @throws IOException - Exception in I/O.
     **/
    void _processWill(int option) throws IOException
    {
        if (debugoptions)
        {
            System.err.println("RECEIVED WILL: "
                + TelnetOption.getOption(option));
        }

        if (__notifhand != null)
        {
            __notifhand.receivedNegotiation(
                TelnetNotificationHandler.RECEIVED_WILL,
                option);
        }

        boolean acceptNewState = false;

        /* open TelnetOptionHandler functionality (start)*/
        if (optionHandlers[option] != null)
        {
            acceptNewState = optionHandlers[option].getAcceptRemote();
        }
        /* open TelnetOptionHandler functionality (end)*/

        if (_doResponse[option] > 0)
        {
            --_doResponse[option];
            if (_doResponse[option] > 0 && _stateIsDo(option))
            {
                --_doResponse[option];
            }
        }

        if (_doResponse[option] == 0 && _requestedDont(option))
        {

            switch (option)
            {

            default:
                break;

            }


            if (acceptNewState)
            {
                _setWantDo(option);
                _sendDo(option);
            }
            else
            {
                ++_doResponse[option];
                _sendDont(option);
            }
        }

        _setDo(option);
    }

    /**
     * Processes a WONT request.
     *
     * @param option - option code to be set.
     * @throws IOException - Exception in I/O.
     **/
    void _processWont(int option) throws IOException
    {
        if (debugoptions)
        {
            System.err.println("RECEIVED WONT: "
                + TelnetOption.getOption(option));
        }

        if (__notifhand != null)
        {
            __notifhand.receivedNegotiation(
                TelnetNotificationHandler.RECEIVED_WONT,
                option);
        }

        if (_doResponse[option] > 0)
        {
            --_doResponse[option];
            if (_doResponse[option] > 0 && _stateIsDont(option))
            {
                --_doResponse[option];
            }
        }

        if (_doResponse[option] == 0 && _requestedDo(option))
        {

            switch (option)
            {

            default:
                break;

            }

            /* FIX for a BUG in the negotiation (start)*/
            if ((_stateIsDo(option)) || (_requestedDo(option)))
            {
                _sendDont(option);
            }

            _setWantDont(option);
            /* FIX for a BUG in the negotiation (end)*/
        }

        _setDont(option);
    }

    /* TERMINAL-TYPE option (start)*/
    /**
     * Processes a suboption negotiation.
     *
     * @param suboption - subnegotiation data received
     * @param suboptionLength - length of data received
     * @throws IOException - Exception in I/O.
     **/
    void _processSuboption(int suboption[], int suboptionLength)
    throws IOException
    {
        if (debug)
        {
            System.err.println("PROCESS SUBOPTION.");
        }

        /* open TelnetOptionHandler functionality (start)*/
        if (suboptionLength > 0)
        {
            if (optionHandlers[suboption[0]] != null)
            {
                int responseSuboption[] =
                  optionHandlers[suboption[0]].answerSubnegotiation(suboption,
                  suboptionLength);
                _sendSubnegotiation(responseSuboption);
            }
            else
            {
                if (suboptionLength > 1)
                {
                    if (debug)
                    {
                        for (int ii = 0; ii < suboptionLength; ii++)
                        {
                            System.err.println("SUB[" + ii + "]: "
                                + suboption[ii]);
                        }
                    }
                    if ((suboption[0] == TERMINAL_TYPE)
                        && (suboption[1] == TERMINAL_TYPE_SEND))
                    {
                        _sendTerminalType();
                    }
                }
            }
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /***
     * Sends terminal type information.
     *
     * @throws IOException - Exception in I/O.
     ***/
    final synchronized void _sendTerminalType()
    throws IOException
    {
        if (debug)
        {
            System.err.println("SEND TERMINAL-TYPE: " + terminalType);
        }
        if (terminalType != null)
        {
            _output_.write(_COMMAND_SB);
            _output_.write(_COMMAND_IS);
            _output_.write(terminalType.getBytes(getCharset()));
            _output_.write(_COMMAND_SE);
            _output_.flush();
        }
    }

    /* TERMINAL-TYPE option (end)*/

    /* open TelnetOptionHandler functionality (start)*/
    /**
     * Manages subnegotiation for Terminal Type.
     *
     * @param subn - subnegotiation data to be sent
     * @throws IOException - Exception in I/O.
     **/
    final synchronized void _sendSubnegotiation(int subn[])
    throws IOException
    {
        if (debug)
        {
            System.err.println("SEND SUBNEGOTIATION: ");
            if (subn != null)
            {
                System.err.println(Arrays.toString(subn));
            }
        }
        if (subn != null)
        {
            _output_.write(_COMMAND_SB);
            // Note _output_ is buffered, so might as well simplify by writing single bytes
            for (int element : subn)
            {
                byte b = (byte) element;
                if (b == (byte) TelnetCommand.IAC) { // cast is necessary because IAC is outside the signed byte range
                    _output_.write(b); // double any IAC bytes
                }
                _output_.write(b);
            }
            _output_.write(_COMMAND_SE);

            /* Code Section added for sending the negotiation ASAP (start)*/
            _output_.flush();
            /* Code Section added for sending the negotiation ASAP (end)*/
        }
    }
    /* open TelnetOptionHandler functionality (end)*/

    /**
     * Sends a command, automatically adds IAC prefix and flushes the output.
     *
     * @param cmd - command data to be sent
     * @throws IOException - Exception in I/O.
     * @since 3.0
     */
    final synchronized void _sendCommand(byte cmd) throws IOException
    {
            _output_.write(TelnetCommand.IAC);
            _output_.write(cmd);
            _output_.flush();
    }

    /* Code Section added for supporting AYT (start)*/
    /***
     * Processes the response of an AYT
     ***/
    final synchronized void _processAYTResponse()
    {
        if (!aytFlag)
        {
            synchronized (aytMonitor)
            {
                aytFlag = true;
                aytMonitor.notifyAll();
            }
        }
    }
    /* Code Section added for supporting AYT (end)*/

    /***
     * Called upon connection.
     *
     * @throws IOException - Exception in I/O.
     ***/
    @Override
    protected void _connectAction_() throws IOException
    {
        /* (start). BUGFIX: clean the option info for each connection*/
        for (int ii = 0; ii < TelnetOption.MAX_OPTION_VALUE + 1; ii++)
        {
            _doResponse[ii] = 0;
            _willResponse[ii] = 0;
            _options[ii] = 0;
            if (optionHandlers[ii] != null)
            {
                optionHandlers[ii].setDo(false);
                optionHandlers[ii].setWill(false);
            }
        }
        /* (end). BUGFIX: clean the option info for each connection*/

        super._connectAction_();
        _input_ = new BufferedInputStream(_input_);
        _output_ = new BufferedOutputStream(_output_);

        /* open TelnetOptionHandler functionality (start)*/
        for (int ii = 0; ii < TelnetOption.MAX_OPTION_VALUE + 1; ii++)
        {
            if (optionHandlers[ii] != null)
            {
                if (optionHandlers[ii].getInitLocal())
                {
                    _requestWill(optionHandlers[ii].getOptionCode());
                }

                if (optionHandlers[ii].getInitRemote())
                {
                    _requestDo(optionHandlers[ii].getOptionCode());
                }
            }
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /**
     * Sends a DO.
     *
     * @param option - Option code.
     * @throws IOException - Exception in I/O.
     **/
    final synchronized void _sendDo(int option)
    throws IOException
    {
        if (debug || debugoptions)
        {
            System.err.println("DO: " + TelnetOption.getOption(option));
        }
        _output_.write(_COMMAND_DO);
        _output_.write(option);

        /* Code Section added for sending the negotiation ASAP (start)*/
        _output_.flush();
        /* Code Section added for sending the negotiation ASAP (end)*/
    }

    /**
     * Requests a DO.
     *
     * @param option - Option code.
     * @throws IOException - Exception in I/O.
     **/
    final synchronized void _requestDo(int option)
    throws IOException
    {
        if ((_doResponse[option] == 0 && _stateIsDo(option))
            || _requestedDo(option))
        {
            return ;
        }
        _setWantDo(option);
        ++_doResponse[option];
        _sendDo(option);
    }

    /**
     * Sends a DONT.
     *
     * @param option - Option code.
     * @throws IOException - Exception in I/O.
     **/
    final synchronized void _sendDont(int option)
    throws IOException
    {
        if (debug || debugoptions)
        {
            System.err.println("DONT: " + TelnetOption.getOption(option));
        }
        _output_.write(_COMMAND_DONT);
        _output_.write(option);

        /* Code Section added for sending the negotiation ASAP (start)*/
        _output_.flush();
        /* Code Section added for sending the negotiation ASAP (end)*/
    }

    /**
     * Requests a DONT.
     *
     * @param option - Option code.
     * @throws IOException - Exception in I/O.
     **/
    final synchronized void _requestDont(int option)
    throws IOException
    {
        if ((_doResponse[option] == 0 && _stateIsDont(option))
            || _requestedDont(option))
        {
            return ;
        }
        _setWantDont(option);
        ++_doResponse[option];
        _sendDont(option);
    }


    /**
     * Sends a WILL.
     *
     * @param option - Option code.
     * @throws IOException - Exception in I/O.
     **/
    final synchronized void _sendWill(int option)
    throws IOException
    {
        if (debug || debugoptions)
        {
            System.err.println("WILL: " + TelnetOption.getOption(option));
        }
        _output_.write(_COMMAND_WILL);
        _output_.write(option);

        /* Code Section added for sending the negotiation ASAP (start)*/
        _output_.flush();
        /* Code Section added for sending the negotiation ASAP (end)*/
    }

    /**
     * Requests a WILL.
     *
     * @param option - Option code.
     * @throws IOException - Exception in I/O.
     **/
    final synchronized void _requestWill(int option)
    throws IOException
    {
        if ((_willResponse[option] == 0 && _stateIsWill(option))
            || _requestedWill(option))
        {
            return ;
        }
        _setWantWill(option);
        ++_doResponse[option];
        _sendWill(option);
    }

    /**
     * Sends a WONT.
     *
     * @param option - Option code.
     * @throws IOException - Exception in I/O.
     **/
    final synchronized void _sendWont(int option)
    throws IOException
    {
        if (debug || debugoptions)
        {
            System.err.println("WONT: " + TelnetOption.getOption(option));
        }
        _output_.write(_COMMAND_WONT);
        _output_.write(option);

        /* Code Section added for sending the negotiation ASAP (start)*/
        _output_.flush();
        /* Code Section added for sending the negotiation ASAP (end)*/
    }

    /**
     * Requests a WONT.
     *
     * @param option - Option code.
     * @throws IOException - Exception in I/O.
     **/
    final synchronized void _requestWont(int option)
    throws IOException
    {
        if ((_willResponse[option] == 0 && _stateIsWont(option))
            || _requestedWont(option))
        {
            return ;
        }
        _setWantWont(option);
        ++_doResponse[option];
        _sendWont(option);
    }

    /**
     * Sends a byte.
     *
     * @param b - byte to send
     * @throws IOException - Exception in I/O.
     **/
    final synchronized void _sendByte(int b)
    throws IOException
    {
        _output_.write(b);

        /* Code Section added for supporting spystreams (start)*/
        _spyWrite(b);
        /* Code Section added for supporting spystreams (end)*/

    }

    /* Code Section added for supporting AYT (start)*/
    /**
     * Sends an Are You There sequence and waits for the result.
     *
     * @param timeout - Time to wait for a response (millis.)
     * @throws IOException - Exception in I/O.
     * @throws IllegalArgumentException - Illegal argument
     * @throws InterruptedException - Interrupted during wait.
     * @return true if AYT received a response, false otherwise
     **/
    final boolean _sendAYT(long timeout)
    throws IOException, IllegalArgumentException, InterruptedException
    {
        boolean retValue = false;
        synchronized (aytMonitor)
        {
            synchronized (this)
            {
                aytFlag = false;
                _output_.write(_COMMAND_AYT);
                _output_.flush();
            }
            aytMonitor.wait(timeout);
            if (aytFlag == false)
            {
                retValue = false;
                aytFlag = true;
            }
            else
            {
                retValue = true;
            }
        }

        return (retValue);
    }
    /* Code Section added for supporting AYT (end)*/

    /* open TelnetOptionHandler functionality (start)*/

    /**
     * Registers a new TelnetOptionHandler for this telnet  to use.
     *
     * @param opthand - option handler to be registered.
     * @throws InvalidTelnetOptionException - The option code is invalid.
     * @throws IOException on error
     **/
    void addOptionHandler(TelnetOptionHandler opthand)
    throws InvalidTelnetOptionException, IOException
    {
        int optcode = opthand.getOptionCode();
        if (TelnetOption.isValidOption(optcode))
        {
            if (optionHandlers[optcode] == null)
            {
                optionHandlers[optcode] = opthand;
                if (isConnected())
                {
                    if (opthand.getInitLocal())
                    {
                        _requestWill(optcode);
                    }

                    if (opthand.getInitRemote())
                    {
                        _requestDo(optcode);
                    }
                }
            }
            else
            {
                throw (new InvalidTelnetOptionException(
                    "Already registered option", optcode));
            }
        }
        else
        {
            throw (new InvalidTelnetOptionException(
                "Invalid Option Code", optcode));
        }
    }

    /**
     * Unregisters a  TelnetOptionHandler.
     *
     * @param optcode - Code of the option to be unregistered.
     * @throws InvalidTelnetOptionException - The option code is invalid.
     * @throws IOException on error
     **/
    void deleteOptionHandler(int optcode)
    throws InvalidTelnetOptionException, IOException
    {
        if (TelnetOption.isValidOption(optcode))
        {
            if (optionHandlers[optcode] == null)
            {
                throw (new InvalidTelnetOptionException(
                    "Unregistered option", optcode));
            }
            else
            {
                TelnetOptionHandler opthand = optionHandlers[optcode];
                optionHandlers[optcode] = null;

                if (opthand.getWill())
                {
                    _requestWont(optcode);
                }

                if (opthand.getDo())
                {
                    _requestDont(optcode);
                }
            }
        }
        else
        {
            throw (new InvalidTelnetOptionException(
                "Invalid Option Code", optcode));
        }
    }
    /* open TelnetOptionHandler functionality (end)*/

    /* Code Section added for supporting spystreams (start)*/
    /***
     * Registers an OutputStream for spying what's going on in
     * the Telnet session.
     *
     * @param spystream - OutputStream on which session activity
     * will be echoed.
     ***/
    void _registerSpyStream(OutputStream  spystream)
    {
        spyStream = spystream;
    }

    /***
     * Stops spying this Telnet.
     *
     ***/
    void _stopSpyStream()
    {
        spyStream = null;
    }

    /***
     * Sends a read char on the spy stream.
     *
     * @param ch - character read from the session
     ***/
    void _spyRead(int ch)
    {
        OutputStream spy = spyStream;
        if (spy != null)
        {
            try
            {
                if (ch != '\r') // never write '\r' on its own
                {
                    if (ch == '\n')
                    {
                        spy.write('\r'); // add '\r' before '\n'
                    }
                    spy.write(ch); // write original character
                    spy.flush();
                }
            }
            catch (IOException e)
            {
                spyStream = null;
            }
        }
    }

    /***
     * Sends a written char on the spy stream.
     *
     * @param ch - character written to the session
     ***/
    void _spyWrite(int ch)
    {
        if (!(_stateIsDo(TelnetOption.ECHO)
            && _requestedDo(TelnetOption.ECHO)))
        {
            OutputStream spy = spyStream;
            if (spy != null)
            {
                try
                {
                    spy.write(ch);
                    spy.flush();
                }
                catch (IOException e)
                {
                    spyStream = null;
                }
            }
        }
    }
    /* Code Section added for supporting spystreams (end)*/

    /***
     * Registers a notification handler to which will be sent
     * notifications of received telnet option negotiation commands.
     *
     * @param notifhand - TelnetNotificationHandler to be registered
     ***/
    public void registerNotifHandler(TelnetNotificationHandler  notifhand)
    {
        __notifhand = notifhand;
    }

    /***
     * Unregisters the current notification handler.
     *
     ***/
    public void unregisterNotifHandler()
    {
        __notifhand = null;
    }
}
