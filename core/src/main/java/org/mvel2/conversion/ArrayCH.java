/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.conversion;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;

import java.util.HashMap;
import java.util.Map;

import org.mvel2.ConversionException;
import org.mvel2.ConversionHandler;

public class ArrayCH implements ConversionHandler {

    private static final Map<Class, Converter> CNV = new HashMap<Class, Converter>();

    static {
        CNV.put(String[].class, new Converter() {

            public Object convert(Object o) {
                Object[] old = (Object[]) o;
                String[] n = new String[old.length];
                for (int i = 0; i < old.length; i++) {
                    n[i] = valueOf(old[i]);
                }

                return n;
            }
        });

        CNV.put(Integer[].class, new Converter() {

            public Object convert(Object o) {
                Object[] old = (Object[]) o;
                Integer[] n = new Integer[old.length];
                for (int i = 0; i < old.length; i++) {
                    n[i] = parseInt(valueOf(old[i]));
                }

                return n;
            }

        });

        CNV.put(int[].class, new Converter() {

            public Object convert(Object o) {
                Object[] old = (Object[]) o;
                int[] n = new int[old.length];
                for (int i = 0; i < old.length; i++) {
                    n[i] = parseInt(valueOf(old[i]));
                }

                return n;
            }

        });

    }

    public Object convertFrom(Object in) {
        if (!CNV.containsKey(in.getClass()))
            throw new ConversionException("cannot convert type: " + in.getClass().getName() + " to: " + Boolean.class.getName());
        return CNV.get(in.getClass()).convert(in);
    }

    public boolean canConvertFrom(Class cls) {
        return CNV.containsKey(cls);
    }
}
