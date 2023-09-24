/*
 * Copyright 2020  Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taobao.arthas.grpcweb.proxy;

import com.google.common.annotations.VisibleForTesting;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MessageUtils {
    @VisibleForTesting
    public
    enum ContentType {
        GRPC_WEB_BINARY, GRPC_WEB_TEXT;
    }

    private static Map<String, ContentType> GRPC_GCP_CONTENT_TYPES = new HashMap<String, ContentType>() {
        {
            put("application/grpc-web", ContentType.GRPC_WEB_BINARY);
            put("application/grpc-web+proto", ContentType.GRPC_WEB_BINARY);
            put("application/grpc-web-text", ContentType.GRPC_WEB_TEXT);
            put("application/grpc-web-text+proto", ContentType.GRPC_WEB_TEXT);
        }
    };

    /**
     * Validate the content-type
     */
    public static ContentType validateContentType(String contentType) throws IllegalArgumentException {
        if (contentType == null || !GRPC_GCP_CONTENT_TYPES.containsKey(contentType)) {
            throw new IllegalArgumentException("This content type is not used for grpc-web: " + contentType);
        }
        return getContentType(contentType);
    }

    static ContentType getContentType(String type) {
        return GRPC_GCP_CONTENT_TYPES.get(type);
    }

    /**
     * Find the input arg protobuf class for the given rpc-method. Convert the given
     * bytes to the input protobuf. return that.
     */
    static Object getInputProtobufObj(Method rpcMethod, byte[] in) {
        Class[] inputArgs = rpcMethod.getParameterTypes();
        Class inputArgClass = inputArgs[0];

        // use the inputArg classtype to create a protobuf object
        Method parseFromObj;
        try {
            parseFromObj = inputArgClass.getMethod("parseFrom", byte[].class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Couldn't find method in 'parseFrom' in " + inputArgClass.getName());
        }

        Object inputObj;
        try {
            inputObj = parseFromObj.invoke(null, in);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }

        if (inputObj == null || !inputArgClass.isInstance(inputObj)) {
            throw new IllegalArgumentException("Input obj is **not** instance of the correct input class type");
        }
        return inputObj;
    }
}
