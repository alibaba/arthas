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
package com.taobao.arthas.core.testtool;

import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author earayu
 */
public class TestUtils {

    public static <T> List<T> newArrayList(T ... items){
        List<T> list = new ArrayList<T>();
        if(items!=null) {
            Collections.addAll(list, items);
        }
        return list;
    }

    /**
     * copied from https://github.com/apache/commons-io/blob/master/src/test/java/org/apache/commons/io/testtools/TestUtils.java
     * Assert that the content of a file is equal to that in a byte[].
     *
     * @param b0   the expected contents
     * @param file the file to check
     * @throws IOException If an I/O error occurs while reading the file contents
     */
    public static void assertEqualContent(final byte[] b0, final File file) throws IOException {
        int count = 0, numRead = 0;
        final byte[] b1 = new byte[b0.length];
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            while (count < b0.length && numRead >= 0) {
                numRead = is.read(b1, count, b0.length);
                count += numRead;
            }
            Assert.assertEquals("Different number of bytes: ", b0.length, count);
            for (int i = 0; i < count; i++) {
                Assert.assertEquals("byte " + i + " differs", b0[i], b1[i]);
            }
        }finally {
            if(is!=null){
                is.close();
            }
        }
    }

}
