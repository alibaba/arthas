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

/**
 * Creates frames from the input bytes.
 */
public class MessageFramer {
  public enum Type {
    DATA ((byte) 0x00),
    TRAILER ((byte) 0x80);

    public final byte value;
    Type(byte b) {
      value = b;
    }
  }

  // TODO: handle more than single frame; i.e., input byte array size > (2GB - 1)
  public byte[] getPrefix(byte[] in, Type type) {
    int len = in.length;
    return new byte[] {
        type.value,
        (byte) ((len >> 24) & 0xff),
        (byte) ((len >> 16) & 0xff),
        (byte) ((len >> 8) & 0xff),
        (byte) ((len >> 0) & 0xff),
    };
  }
}
