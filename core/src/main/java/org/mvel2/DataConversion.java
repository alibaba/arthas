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

package org.mvel2;

import static org.mvel2.util.ReflectionUtil.isAssignableFrom;
import static org.mvel2.util.ReflectionUtil.toNonPrimitiveType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.mvel2.conversion.ArrayHandler;
import org.mvel2.conversion.BigDecimalCH;
import org.mvel2.conversion.BigIntegerCH;
import org.mvel2.conversion.BooleanCH;
import org.mvel2.conversion.ByteCH;
import org.mvel2.conversion.CharArrayCH;
import org.mvel2.conversion.CharCH;
import org.mvel2.conversion.CompositeCH;
import org.mvel2.conversion.DoubleCH;
import org.mvel2.conversion.FloatCH;
import org.mvel2.conversion.IntArrayCH;
import org.mvel2.conversion.IntegerCH;
import org.mvel2.conversion.ListCH;
import org.mvel2.conversion.LongCH;
import org.mvel2.conversion.ObjectCH;
import org.mvel2.conversion.SetCH;
import org.mvel2.conversion.ShortCH;
import org.mvel2.conversion.StringArrayCH;
import org.mvel2.conversion.StringCH;
import org.mvel2.util.FastList;

/**
 * The DataConversion factory is where all of MVEL's type converters are registered with the runtime.
 *
 * @author Mike Brock
 * @see ConversionHandler
 */
public class DataConversion {

    private static final Map<Class, ConversionHandler> CONVERTERS = new HashMap<Class, ConversionHandler>(38 * 2, 0.5f);

    static {
        ConversionHandler ch;

        CONVERTERS.put(Integer.class, ch = new IntegerCH());
        CONVERTERS.put(int.class, ch);

        CONVERTERS.put(Short.class, ch = new ShortCH());
        CONVERTERS.put(short.class, ch);

        CONVERTERS.put(Long.class, ch = new LongCH());
        CONVERTERS.put(long.class, ch);

        CONVERTERS.put(Character.class, ch = new CharCH());
        CONVERTERS.put(char.class, ch);

        CONVERTERS.put(Byte.class, ch = new ByteCH());
        CONVERTERS.put(byte.class, ch);

        CONVERTERS.put(Float.class, ch = new FloatCH());
        CONVERTERS.put(float.class, ch);

        CONVERTERS.put(Double.class, ch = new DoubleCH());
        CONVERTERS.put(double.class, ch);

        CONVERTERS.put(Boolean.class, ch = new BooleanCH());
        CONVERTERS.put(boolean.class, ch);

        CONVERTERS.put(String.class, new StringCH());

        CONVERTERS.put(Object.class, new ObjectCH());

        CONVERTERS.put(Character[].class, ch = new CharArrayCH());
        CONVERTERS.put(char[].class, new CompositeCH(ch, new ArrayHandler(char[].class)));

        CONVERTERS.put(String[].class, new StringArrayCH());

        CONVERTERS.put(Integer[].class, new IntArrayCH());

        CONVERTERS.put(int[].class, new ArrayHandler(int[].class));
        CONVERTERS.put(long[].class, new ArrayHandler(long[].class));
        CONVERTERS.put(double[].class, new ArrayHandler(double[].class));
        CONVERTERS.put(float[].class, new ArrayHandler(float[].class));
        CONVERTERS.put(short[].class, new ArrayHandler(short[].class));
        CONVERTERS.put(boolean[].class, new ArrayHandler(boolean[].class));
        CONVERTERS.put(byte[].class, new ArrayHandler(byte[].class));

        CONVERTERS.put(BigDecimal.class, new BigDecimalCH());
        CONVERTERS.put(BigInteger.class, new BigIntegerCH());

        CONVERTERS.put(List.class, ch = new ListCH());
        CONVERTERS.put(FastList.class, ch);
        CONVERTERS.put(ArrayList.class, ch);
        CONVERTERS.put(LinkedList.class, ch);

        CONVERTERS.put(Set.class, ch = new SetCH());
        CONVERTERS.put(HashSet.class, ch);
        CONVERTERS.put(LinkedHashSet.class, ch);
        CONVERTERS.put(TreeSet.class, ch);
    }

    public static boolean canConvert(Class toType, Class convertFrom) {
        if (isAssignableFrom(toType, convertFrom)) return true;
        if (CONVERTERS.containsKey(toType)) {
            return CONVERTERS.get(toType).canConvertFrom(toNonPrimitiveType(convertFrom));
        } else if (toType.isArray() && canConvert(toType.getComponentType(), convertFrom)) {
            return true;
        }
        return false;
    }

    public static <T> T convert(Object in, Class<T> toType) {
        if (in == null) return null;
        if (toType == in.getClass() || toType.isAssignableFrom(in.getClass())) {
            return (T) in;
        }

        ConversionHandler h = CONVERTERS.get(toType);
        if (h == null && toType.isArray()) {
            ArrayHandler ah;
            CONVERTERS.put(toType, ah = new ArrayHandler(toType));
            return (T) ah.convertFrom(in);
        } else {
            return (T) h.convertFrom(in);
        }
    }

    /**
     * Register a new {@link ConversionHandler} with the factory.
     *
     * @param type    - Target type represented by the conversion handler.
     * @param handler - An instance of the handler.
     */
    public static void addConversionHandler(Class type, ConversionHandler handler) {
        CONVERTERS.put(type, handler);
    }

    public static void main(String[] args) {
        System.out.println(char[][].class);
    }

    private interface ArrayTypeMarker {
    }
}
