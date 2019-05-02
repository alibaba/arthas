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

package org.mvel2.util;

import static java.lang.String.valueOf;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;
import static java.lang.reflect.Modifier.isPublic;
import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.util.ParseTools.boxPrimitive;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.mvel2.ParserContext;
import org.mvel2.compiler.PropertyVerifier;

public class PropertyTools {

    public static boolean isEmpty(Object o) {
        if (o != null) {
            if (o instanceof Object[]) {
                return ((Object[]) o).length == 0 || (((Object[]) o).length == 1 && isEmpty(((Object[]) o)[0]));
            } else {
                return ("".equals(valueOf(o))) || "null".equals(valueOf(o)) || (o instanceof Collection && ((Collection) o).size() == 0)
                        || (o instanceof Map && ((Map) o).size() == 0);
            }
        }
        return true;
    }

    public static Method getSetter(Class clazz, String property) {
        property = ReflectionUtil.getSetter(property);

        for (Method meth : clazz.getMethods()) {
            if ((meth.getModifiers() & PUBLIC) != 0 && meth.getParameterTypes().length == 1 && property.equals(meth.getName())) {
                return meth;
            }
        }

        return null;
    }

    public static Method getSetter(Class clazz, String property, Class type) {
        String simple = "set" + property;
        property = ReflectionUtil.getSetter(property);

        for (Method meth : clazz.getMethods()) {
            if ((meth.getModifiers() & PUBLIC) != 0 && meth.getParameterTypes().length == 1
                    && (property.equals(meth.getName()) || simple.equals(meth.getName()))
                    && (type == null || canConvert(meth.getParameterTypes()[0], type))) {
                return meth;
            }
        }

        return null;
    }

    public static boolean hasGetter(Field field) {
        Method meth = getGetter(field.getDeclaringClass(), field.getName());
        return meth != null && field.getType().isAssignableFrom(meth.getReturnType());
    }

    public static boolean hasSetter(Field field) {
        Method meth = getSetter(field.getDeclaringClass(), field.getName());
        return meth != null && meth.getParameterTypes().length == 1 && field.getType().isAssignableFrom(meth.getParameterTypes()[0]);
    }

    public static Method getGetter(Class clazz, String property) {
        String simple = "get" + property;
        String simpleIsGet = "is" + property;
        String isGet = ReflectionUtil.getIsGetter(property);
        String getter = ReflectionUtil.getGetter(property);

        Method candidate = null;

        if (Collection.class.isAssignableFrom(clazz) && "isEmpty".equals(isGet)) {
            try {
                return Collection.class.getMethod("isEmpty");
            } catch (NoSuchMethodException ignore) {}
        }

        for (Method meth : clazz.getMethods()) {
            if ((meth.getModifiers() & PUBLIC) != 0 && (meth.getModifiers() & STATIC) == 0 && meth.getParameterTypes().length == 0
                    && (getter.equals(meth.getName()) || property.equals(meth.getName())
                            || ((isGet.equals(meth.getName()) || simpleIsGet.equals(meth.getName()))
                                    && meth.getReturnType() == boolean.class)
                            || simple.equals(meth.getName()))) {
                if (candidate == null || candidate.getReturnType().isAssignableFrom(meth.getReturnType())) {
                    candidate = meth;
                }
            }
        }
        return candidate;
    }

    public static Class getReturnType(Class clazz, String property, ParserContext ctx) {
        return new PropertyVerifier(property, ctx, clazz).analyze();
    }

    public static Member getFieldOrAccessor(Class clazz, String property) {
        Field[] fields = clazz.getFields();
        for (Field f : fields) {
            f.setAccessible(true);
            if (property.equals(f.getName())) {
                return f;
            }
        }
        fields = clazz.getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
            if (property.equals(f.getName())) {
                return f;
            }
        }
        return getGetter(clazz, property);
    }

    public static Member getFieldOrWriteAccessor(Class clazz, String property) {
        Field field;
        try {
            if ((field = clazz.getField(property)) != null && isPublic(field.getModifiers())) {
                return field;
            }
        } catch (NullPointerException e) {
            return null;
        } catch (NoSuchFieldException e) {
            // do nothing.
        }

        return getSetter(clazz, property);
    }

    public static Member getFieldOrWriteAccessor(Class clazz, String property, Class type) {
        for (Field f : clazz.getFields()) {
            if (property.equals(f.getName()) && (type == null || canConvert(f.getType(), type))) {
                return f;
            }
        }

        return getSetter(clazz, property, type);
    }

    public static boolean contains(Object toCompare, Object testValue) {
        if (toCompare == null) return false;
        else if (toCompare instanceof String) return ((String) toCompare).contains(valueOf(testValue));
        //    return ((String) toCompare).indexOf(valueOf(testValue)) > -1;
        else if (toCompare instanceof Collection) return ((Collection) toCompare).contains(testValue);
        else if (toCompare instanceof Map) return ((Map) toCompare).containsKey(testValue);
        else if (toCompare.getClass().isArray()) {
            for (Object o : ((Object[]) toCompare)) {
                if (testValue == null && o == null) return true;
                else if (o != null && o.equals(testValue)) return true;
            }
        }
        return false;
    }

    public static Object getPrimitiveInitialValue(Class type) {
        if (type == int.class) {
            return 0;
        } else if (type == boolean.class) {
            return false;
        } else if (type == char.class) {
            return (char) 0;
        } else if (type == double.class) {
            return 0d;
        } else if (type == long.class) {
            return 0l;
        } else if (type == float.class) {
            return 0f;
        } else if (type == short.class) {
            return (short) 0;
        } else if (type == byte.class) {
            return (byte) 0;
        } else {
            return 0;
        }
    }

    public static boolean isAssignable(Class to, Class from) {
        return (to.isPrimitive() ? boxPrimitive(to) : to).isAssignableFrom(from.isPrimitive() ? boxPrimitive(from) : from);
    }

    /**
     * Get the JVM version
     * @return first <code>mvel.java.version</code>, then <code>java.version</code>
     * @see System.getProperty("mvel.java.version");
     * @see System.getProperty("java.version");
     */
    public static String getJavaVersion() {
        return System.getProperty("mvel.java.version") != null ? System.getProperty("mvel.java.version") : System
                .getProperty("java.version");
    }
}
