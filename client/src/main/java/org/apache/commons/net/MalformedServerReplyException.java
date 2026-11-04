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

import java.io.IOException;

/***
 * This exception is used to indicate that the reply from a server
 * could not be interpreted.  Most of the NetComponents classes attempt
 * to be as lenient as possible when receiving server replies.  Many
 * server implementations deviate from IETF protocol specifications, making
 * it necessary to be as flexible as possible.  However, there will be
 * certain situations where it is not possible to continue an operation
 * because the server reply could not be interpreted in a meaningful manner.
 * In these cases, a MalformedServerReplyException should be thrown.
 *
 *
 ***/

public class MalformedServerReplyException extends IOException
{

    private static final long serialVersionUID = 6006765264250543945L;

    /*** Constructs a MalformedServerReplyException with no message ***/
    public MalformedServerReplyException()
    {
        super();
    }

    /***
     * Constructs a MalformedServerReplyException with a specified message.
     *
     * @param message  The message explaining the reason for the exception.
     ***/
    public MalformedServerReplyException(String message)
    {
        super(message);
    }

}
