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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/***
 * The TelnetClient class implements the simple network virtual
 * terminal (NVT) for the Telnet protocol according to RFC 854.  It
 * does not implement any of the extra Telnet options because it
 * is meant to be used within a Java program providing automated
 * access to Telnet accessible resources.
 * <p>
 * The class can be used by first connecting to a server using the
 * SocketClient
 * {@link org.apache.commons.net.SocketClient#connect connect}
 * method.  Then an InputStream and OutputStream for sending and
 * receiving data over the Telnet connection can be obtained by
 * using the {@link #getInputStream  getInputStream() } and
 * {@link #getOutputStream  getOutputStream() } methods.
 * When you finish using the streams, you must call
 * {@link #disconnect  disconnect } rather than simply
 * closing the streams.
 ***/

public class TelnetClient extends Telnet
{
    private InputStream __input;
    private OutputStream __output;
    protected boolean readerThread = true;
    private TelnetInputListener inputListener;

    /***
     * Default TelnetClient constructor, sets terminal-type {@code VT100}.
     ***/
    public TelnetClient()
    {
        /* TERMINAL-TYPE option (start)*/
        super ("VT100");
        /* TERMINAL-TYPE option (end)*/
        __input = null;
        __output = null;
    }

    /**
     * Construct an instance with the specified terminal type.
     *
     * @param termtype the terminal type to use, e.g. {@code VT100}
     */
    /* TERMINAL-TYPE option (start)*/
    public TelnetClient(String termtype)
    {
        super (termtype);
        __input = null;
        __output = null;
    }
    /* TERMINAL-TYPE option (end)*/

    void _flushOutputStream() throws IOException
    {
        _output_.flush();
    }
    void _closeOutputStream() throws IOException
    {
        _output_.close();
    }

    /***
     * Handles special connection requirements.
     *
     * @exception IOException  If an error occurs during connection setup.
     ***/
    @Override
    protected void _connectAction_() throws IOException
    {
        super._connectAction_();
        TelnetInputStream tmp = new TelnetInputStream(_input_, this, readerThread);
        if(readerThread)
        {
            tmp._start();
        }
        // __input CANNOT refer to the TelnetInputStream.  We run into
        // blocking problems when some classes use TelnetInputStream, so
        // we wrap it with a BufferedInputStream which we know is safe.
        // This blocking behavior requires further investigation, but right
        // now it looks like classes like InputStreamReader are not implemented
        // in a safe manner.
        __input = new BufferedInputStream(tmp);
        __output = new TelnetOutputStream(this);
    }

    /***
     * Disconnects the telnet session, closing the input and output streams
     * as well as the socket.  If you have references to the
     * input and output streams of the telnet connection, you should not
     * close them yourself, but rather call disconnect to properly close
     * the connection.
     ***/
    @Override
    public void disconnect() throws IOException
    {
        if (__input != null) {
            __input.close();
        }
        if (__output != null) {
            __output.close();
        }
        super.disconnect();
    }

    /***
     * Returns the telnet connection output stream.  You should not close the
     * stream when you finish with it.  Rather, you should call
     * {@link #disconnect  disconnect }.
     *
     * @return The telnet connection output stream.
     ***/
    public OutputStream getOutputStream()
    {
        return __output;
    }

    /***
     * Returns the telnet connection input stream.  You should not close the
     * stream when you finish with it.  Rather, you should call
     * {@link #disconnect  disconnect }.
     *
     * @return The telnet connection input stream.
     ***/
    public InputStream getInputStream()
    {
        return __input;
    }

    /***
     * Returns the state of the option on the local side.
     *
     * @param option - Option to be checked.
     *
     * @return The state of the option on the local side.
     ***/
    public boolean getLocalOptionState(int option)
    {
        /* BUG (option active when not already acknowledged) (start)*/
        return (_stateIsWill(option) && _requestedWill(option));
        /* BUG (option active when not already acknowledged) (end)*/
    }

    /***
     * Returns the state of the option on the remote side.
     *
     * @param option - Option to be checked.
     *
     * @return The state of the option on the remote side.
     ***/
    public boolean getRemoteOptionState(int option)
    {
        /* BUG (option active when not already acknowledged) (start)*/
        return (_stateIsDo(option) && _requestedDo(option));
        /* BUG (option active when not already acknowledged) (end)*/
    }
    /* open TelnetOptionHandler functionality (end)*/

    /* Code Section added for supporting AYT (start)*/

    /***
     * Sends an Are You There sequence and waits for the result.
     *
     * @param timeout - Time to wait for a response (millis.)
     *
     * @return true if AYT received a response, false otherwise
     *
     * @throws InterruptedException on error
     * @throws IllegalArgumentException on error
     * @throws IOException on error
     ***/
    public boolean sendAYT(long timeout)
    throws IOException, IllegalArgumentException, InterruptedException
    {
        return (_sendAYT(timeout));
    }
    /* Code Section added for supporting AYT (start)*/

    /***
     * Sends a protocol-specific subnegotiation message to the remote peer.
     * {@link TelnetClient} will add the IAC SB &amp; IAC SE framing bytes;
     * the first byte in {@code message} should be the appropriate telnet
     * option code.
     *
     * <p>
     * This method does not wait for any response. Subnegotiation messages
     * sent by the remote end can be handled by registering an approrpriate
     * {@link TelnetOptionHandler}.
     * </p>
     *
     * @param message option code followed by subnegotiation payload
     * @throws IllegalArgumentException if {@code message} has length zero
     * @throws IOException if an I/O error occurs while writing the message
     * @since 3.0
     ***/
    public void sendSubnegotiation(int[] message)
    throws IOException, IllegalArgumentException
    {
        if (message.length < 1) {
            throw new IllegalArgumentException("zero length message");
        }
        _sendSubnegotiation(message);
    }

    /***
     * Sends a command byte to the remote peer, adding the IAC prefix.
     *
     * <p>
     * This method does not wait for any response. Messages
     * sent by the remote end can be handled by registering an approrpriate
     * {@link TelnetOptionHandler}.
     * </p>
     *
     * @param command the code for the command
     * @throws IOException if an I/O error occurs while writing the message
     * @throws IllegalArgumentException  on error
     * @since 3.0
     ***/
    public void sendCommand(byte command)
    throws IOException, IllegalArgumentException
    {
        _sendCommand(command);
    }

    /* open TelnetOptionHandler functionality (start)*/

    /***
     * Registers a new TelnetOptionHandler for this telnet client to use.
     *
     * @param opthand - option handler to be registered.
     *
     * @throws InvalidTelnetOptionException on error
     * @throws IOException on error
     ***/
    @Override
    public void addOptionHandler(TelnetOptionHandler opthand)
    throws InvalidTelnetOptionException, IOException
    {
        super.addOptionHandler(opthand);
    }
    /* open TelnetOptionHandler functionality (end)*/

    /***
     * Unregisters a  TelnetOptionHandler.
     *
     * @param optcode - Code of the option to be unregistered.
     *
     * @throws InvalidTelnetOptionException on error
     * @throws IOException on error
     ***/
    @Override
    public void deleteOptionHandler(int optcode)
    throws InvalidTelnetOptionException, IOException
    {
        super.deleteOptionHandler(optcode);
    }

    /* Code Section added for supporting spystreams (start)*/
    /***
     * Registers an OutputStream for spying what's going on in
     * the TelnetClient session.
     *
     * @param spystream - OutputStream on which session activity
     * will be echoed.
     ***/
    public void registerSpyStream(OutputStream  spystream)
    {
        super._registerSpyStream(spystream);
    }

    /***
     * Stops spying this TelnetClient.
     *
     ***/
    public void stopSpyStream()
    {
        super._stopSpyStream();
    }
    /* Code Section added for supporting spystreams (end)*/

    /***
     * Registers a notification handler to which will be sent
     * notifications of received telnet option negotiation commands.
     *
     * @param notifhand - TelnetNotificationHandler to be registered
     ***/
    @Override
    public void registerNotifHandler(TelnetNotificationHandler  notifhand)
    {
        super.registerNotifHandler(notifhand);
    }

    /***
     * Unregisters the current notification handler.
     *
     ***/
    @Override
    public void unregisterNotifHandler()
    {
        super.unregisterNotifHandler();
    }

    /***
     * Sets the status of the reader thread.
     *
     * <p>
     * When enabled, a seaparate internal reader thread is created for new
     * connections to read incoming data as it arrives. This results in
     * immediate handling of option negotiation, notifications, etc.
     * (at least until the fixed-size internal buffer fills up).
     * Otherwise, no thread is created an all negotiation and option
     * handling is deferred until a read() is performed on the
     * {@link #getInputStream input stream}.
     * </p>
     *
     * <p>
     * The reader thread must be enabled for {@link TelnetInputListener}
     * support.
     * </p>
     *
     * <p>
     * When this method is invoked, the reader thread status will apply to all
     * subsequent connections; the current connection (if any) is not affected.
     * </p>
     *
     * @param flag true to enable the reader thread, false to disable
     * @see #registerInputListener
     ***/
    public void setReaderThread(boolean flag)
    {
        readerThread = flag;
    }

    /***
     * Gets the status of the reader thread.
     *
     * @return true if the reader thread is enabled, false otherwise
     ***/
    public boolean getReaderThread()
    {
        return (readerThread);
    }

    /***
     * Register a listener to be notified when new incoming data is
     * available to be read on the {@link #getInputStream input stream}.
     * Only one listener is supported at a time.
     *
     * <p>
     * More precisely, notifications are issued whenever the number of
     * bytes available for immediate reading (i.e., the value returned
     * by {@link InputStream#available}) transitions from zero to non-zero.
     * Note that (in general) multiple reads may be required to empty the
     * buffer and reset this notification, because incoming bytes are being
     * added to the internal buffer asynchronously.
     * </p>
     *
     * <p>
     * Notifications are only supported when a {@link #setReaderThread
     * reader thread} is enabled for the connection.
     * </p>
     *
     * @param listener listener to be registered; replaces any previous
     * @since 3.0
     ***/
    public synchronized void registerInputListener(TelnetInputListener listener)
    {
        this.inputListener = listener;
    }

    /***
     * Unregisters the current {@link TelnetInputListener}, if any.
     *
     * @since 3.0
     ***/
    public synchronized void unregisterInputListener()
    {
        this.inputListener = null;
    }

    // Notify input listener
    void notifyInputListener() {
        TelnetInputListener listener;
        synchronized (this) {
            listener = this.inputListener;
        }
        if (listener != null) {
            listener.telnetInputAvailable();
        }
    }
}
