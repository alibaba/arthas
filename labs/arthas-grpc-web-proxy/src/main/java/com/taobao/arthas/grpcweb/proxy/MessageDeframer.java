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

import com.taobao.arthas.grpcweb.proxy.MessageUtils.ContentType;
import com.taobao.arthas.common.IOUtils;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

/**
 * Reads frames from the input bytes and returns a single message.
 */
public class MessageDeframer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());
    static final byte DATA_BYTE = (byte) 0x00;

    // TODO: fix this code to be able to handle upto 4GB input size.
    private int mLength = 0;
    private int mReadSoFar = 0;

    private ArrayList<byte[]> mFrames = new ArrayList<>();
    private byte[] mMsg = null;
    private int mNumFrames;

    byte[] getMessageBytes() {
        return mMsg;
    }

    int getLength() {
        return mLength;
    }

    int getNumberOfFrames() {
        return mNumFrames;
    }

    /**
     * Reads the bytes from the given InputStream and populates bytes in
     * {@link #mMsg}
     */
    public boolean processInput(InputStream in, MessageUtils.ContentType contentType) {
        byte[] inBytes;
        try {
            InputStream inStream = (contentType == ContentType.GRPC_WEB_TEXT) ? Base64.getDecoder().wrap(in) : in;
            inBytes = IOUtils.getBytes(inStream);
        } catch (IOException e) {
            e.printStackTrace();
            logger.warn("invalid input");
            return false;
        }
        if (inBytes.length < 5) {
            logger.debug("invalid input. Expected minimum of 5 bytes");
            return false;
        }

        while (getNextFrameBytes(inBytes)) {
        }
        mNumFrames = mFrames.size();

        // common case is only one frame.
        if (mNumFrames == 1) {
            mMsg = mFrames.get(0);
        } else {
            // concatenate all frames into one byte array
            // TODO: this is inefficient.
            mMsg = new byte[mLength];
            int offset = 0;
            for (byte[] f : mFrames) {
                System.arraycopy(f, 0, mMsg, offset, f.length);
                offset += f.length;
            }
            mFrames = null;
        }
        return true;
    }

    /** returns true if the next frame is a DATA frame */
    private boolean getNextFrameBytes(byte[] inBytes) {
        // Firstbyte should be 0x00 (for this to be a DATA frame)
        int firstByteValue = inBytes[mReadSoFar] | DATA_BYTE;
        if (firstByteValue != 0) {
            logger.debug("done with DATA bytes");
            return false;
        }

        // Next 4 bytes = length of the bytes array starting after the 4 bytes.
        int offset = mReadSoFar + 1;
        int len = ByteBuffer.wrap(inBytes, offset, 4).getInt();

        // Empty message is special case.
        // TODO: Can this is special handling be removed?
        if (len == 0) {
            mFrames.add(new byte[0]);
            return false;
        }

        // Make sure we have enough bytes in the inputstream
        int expectedNumBytes = len + 5 + mReadSoFar;
        if (inBytes.length < expectedNumBytes) {
            logger.warn(String.format("input doesn't have enough bytes. expected: %d, found %d", expectedNumBytes,
                    inBytes.length));
            return false;
        }

        // Read "len" bytes into message
        mLength += len;
        offset += 4;
        byte[] inputBytes = Arrays.copyOfRange(inBytes, offset, len + offset);
        mFrames.add(inputBytes);
        mReadSoFar += (len + 5);
        // we have more frames to process, if there are bytes unprocessed
        return inBytes.length > mReadSoFar;
    }
}
