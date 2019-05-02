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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.ConversionException;
import org.mvel2.ConversionHandler;

public class BigDecimalCH implements ConversionHandler {

    private static final Map<Class, Converter> CNV = new HashMap<Class, Converter>();

    static {
        CNV.put(Object.class, new Converter() {

            public BigDecimal convert(Object o) {
                return new BigDecimal(String.valueOf(o), MathContext.DECIMAL128);
            }
        });

        CNV.put(BigDecimal.class, new Converter() {

            public BigDecimal convert(Object o) {
                return (BigDecimal) o;
            }
        });

        CNV.put(BigInteger.class, new Converter() {

            public BigDecimal convert(Object o) {
                return new BigDecimal(((BigInteger) o).doubleValue(), MathContext.DECIMAL128);
            }
        });

        CNV.put(String.class, new Converter() {

            public BigDecimal convert(Object o) {
                return new BigDecimal((String) o, MathContext.DECIMAL128);
            }
        });

        CNV.put(Double.class, new Converter() {

            public BigDecimal convert(Object o) {
                return new BigDecimal(((Double) o).doubleValue(), MathContext.DECIMAL128);
            }
        });

        CNV.put(Float.class, new Converter() {

            public BigDecimal convert(Object o) {
                return new BigDecimal(((Float) o).doubleValue(), MathContext.DECIMAL128);
            }
        });

        CNV.put(Short.class, new Converter() {

            public BigDecimal convert(Object o) {
                return new BigDecimal(((Short) o).doubleValue(), MathContext.DECIMAL128);
            }
        });

        CNV.put(Long.class, new Converter() {

            public BigDecimal convert(Object o) {
                return new BigDecimal(((Long) o).doubleValue(), MathContext.DECIMAL128);
            }
        });

        CNV.put(Integer.class, new Converter() {

            public BigDecimal convert(Object o) {
                return new BigDecimal(((Integer) o).doubleValue(), MathContext.DECIMAL128);
            }
        });

        CNV.put(String.class, new Converter() {

            public BigDecimal convert(Object o) {
                return new BigDecimal((String) o, MathContext.DECIMAL128);
            }
        });

        CNV.put(char[].class, new Converter() {

            public BigDecimal convert(Object o) {
                return new BigDecimal((char[]) o, MathContext.DECIMAL128);
            }
        }

        );
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
