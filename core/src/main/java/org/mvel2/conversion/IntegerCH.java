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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.ConversionException;
import org.mvel2.ConversionHandler;

public class IntegerCH implements ConversionHandler {

    private static final Map<Class, Converter> CNV = new HashMap<Class, Converter>(10);

    static {
        CNV.put(Object.class, new Converter() {

            public Object convert(Object o) {
                if (((String) o).length() == 0) return 0;

                return parseInt(valueOf(o));
            }
        });

        CNV.put(BigDecimal.class, new Converter() {

            public Integer convert(Object o) {
                return ((BigDecimal) o).intValue();
            }
        });

        CNV.put(BigInteger.class, new Converter() {

            public Integer convert(Object o) {
                return ((BigInteger) o).intValue();
            }
        });

        CNV.put(String.class, new Converter() {

            public Object convert(Object o) {
                return parseInt(((String) o));
            }
        });

        CNV.put(Short.class, new Converter() {

            public Object convert(Object o) {
                return ((Short) o).intValue();
            }
        });

        CNV.put(Long.class, new Converter() {

            public Object convert(Object o) {
                //noinspection UnnecessaryBoxing
                if (((Long) o) > Integer.MAX_VALUE) {
                    throw new ConversionException("cannot coerce Long to Integer since the value (" + valueOf(o)
                            + ") exceeds that maximum precision of Integer.");
                } else {
                    return ((Long) o).intValue();
                }
            }
        });

        CNV.put(Float.class, new Converter() {

            public Object convert(Object o) {
                //noinspection UnnecessaryBoxing
                if (((Float) o) > Integer.MAX_VALUE) {
                    throw new ConversionException("cannot coerce Float to Integer since the value (" + valueOf(o)
                            + ") exceeds that maximum precision of Integer.");
                } else {
                    return ((Float) o).intValue();
                }
            }
        });

        CNV.put(Double.class, new Converter() {

            public Object convert(Object o) {
                //noinspection UnnecessaryBoxing
                if (((Double) o) > Integer.MAX_VALUE) {
                    throw new ConversionException("cannot coerce Long to Integer since the value (" + valueOf(o)
                            + ") exceeds that maximum precision of Integer.");
                } else {
                    return ((Double) o).intValue();
                }
            }
        });

        CNV.put(Integer.class, new Converter() {

            public Object convert(Object o) {
                return o;
            }
        });

        CNV.put(Boolean.class, new Converter() {

            public Integer convert(Object o) {
                if ((Boolean) o) return 1;
                else return 0;
            }
        });

        CNV.put(Character.class, new Converter() {

            public Integer convert(Object o) {
                return (int) ((Character) o).charValue();
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
