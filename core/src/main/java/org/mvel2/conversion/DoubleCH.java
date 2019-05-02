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

import static java.lang.String.valueOf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.ConversionException;
import org.mvel2.ConversionHandler;

public class DoubleCH implements ConversionHandler {

    private static final Map<Class, Converter> CNV = new HashMap<Class, Converter>();

    private static Converter stringConverter = new Converter() {

        public Object convert(Object o) {
            if (((String) o).length() == 0) return (double) 0;

            return Double.parseDouble(((String) o));
        }
    };

    static {
        CNV.put(String.class, stringConverter);

        CNV.put(Object.class, new Converter() {

            public Object convert(Object o) {
                return stringConverter.convert(valueOf(o));
            }
        });

        CNV.put(BigDecimal.class, new Converter() {

            public Double convert(Object o) {
                return ((BigDecimal) o).doubleValue();
            }
        });

        CNV.put(BigInteger.class, new Converter() {

            public Double convert(Object o) {
                return ((BigInteger) o).doubleValue();
            }
        });

        CNV.put(Double.class, new Converter() {

            public Object convert(Object o) {
                return o;
            }
        });

        CNV.put(Float.class, new Converter() {

            public Double convert(Object o) {
                if (((Float) o) > Double.MAX_VALUE) {
                    throw new ConversionException(
                            "cannot coerce Float to Double since the value (" + valueOf(o) + ") exceeds that maximum precision of Double.");

                }

                return ((Float) o).doubleValue();
            }
        });

        CNV.put(Integer.class, new Converter() {

            public Double convert(Object o) {
                //noinspection UnnecessaryBoxing
                return ((Integer) o).doubleValue();
            }
        });

        CNV.put(Short.class, new Converter() {

            public Double convert(Object o) {
                //noinspection UnnecessaryBoxing
                return ((Short) o).doubleValue();
            }
        });

        CNV.put(Long.class, new Converter() {

            public Double convert(Object o) {
                //noinspection UnnecessaryBoxing
                return ((Long) o).doubleValue();
            }
        });

        CNV.put(Boolean.class, new Converter() {

            public Double convert(Object o) {
                if ((Boolean) o) return 1d;
                else return 0d;
            }
        });

    }

    public Object convertFrom(Object in) {
        if (!CNV.containsKey(in.getClass()))
            throw new ConversionException("cannot convert type: " + in.getClass().getName() + " to: " + Integer.class.getName());
        return CNV.get(in.getClass()).convert(in);
    }

    public boolean canConvertFrom(Class cls) {
        return CNV.containsKey(cls);
    }
}
