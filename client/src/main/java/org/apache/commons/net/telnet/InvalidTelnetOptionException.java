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
 * The InvalidTelnetOptionException is the exception that is
 * thrown whenever a TelnetOptionHandler with an invlaid
 * option code is registered in TelnetClient with addOptionHandler.
 ***/
public class InvalidTelnetOptionException extends Exception
{

    private static final long serialVersionUID = -2516777155928793597L;

    /***
     * Option code
     ***/
    private final int optionCode;

    /***
     * Error message
     ***/
    private final String msg;

    /***
     * Constructor for the exception.
     * <p>
     * @param message - Error message.
     * @param optcode - Option code.
     ***/
    public InvalidTelnetOptionException(String message, int optcode)
    {
        optionCode = optcode;
        msg = message;
    }

    /***
     * Gets the error message of ths exception.
     * <p>
     * @return the error message.
     ***/
    @Override
    public String getMessage()
    {
        return (msg + ": " + optionCode);
    }
}
