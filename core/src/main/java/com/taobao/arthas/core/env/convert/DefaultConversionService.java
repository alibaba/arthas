package com.taobao.arthas.core.env.convert;

import java.lang.reflect.Array;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultConversionService implements ConfigurableConversionService {

    private static ConcurrentHashMap<ConvertiblePair, Converter> converters = new ConcurrentHashMap<ConvertiblePair, Converter>();

    public DefaultConversionService() {
        addDefaultConverter();

    }

    private void addDefaultConverter() {
        converters.put(new ConvertiblePair(String.class, Integer.class), new StringToIntegerConverter());
        converters.put(new ConvertiblePair(String.class, Long.class), new StringToLongConverter());

        converters.put(new ConvertiblePair(String.class, Boolean.class), new StringToBooleanConverter());

        converters.put(new ConvertiblePair(String.class, InetAddress.class), new StringToInetAddressConverter());

        converters.put(new ConvertiblePair(String.class, Enum.class), new StringToEnumConverter());

        converters.put(new ConvertiblePair(String.class, Arrays.class), new StringToArrayConverter(this));

    }

    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        if (sourceType == targetType) {
            return true;
        }

        if (targetType.isPrimitive()) {
            targetType = objectiveClass(targetType);
        }

        if (converters.containsKey(new ConvertiblePair(sourceType, targetType))) {
            return true;
        }
        if (targetType.isEnum()) {
            if (converters.containsKey(new ConvertiblePair(sourceType, Enum.class))) {
                return true;
            }
        }

        if (targetType.isArray()) {
            return true;
        }
        return false;
    }

    @Override
    public <T> T convert(Object source, Class<T> targetType) {

        if (targetType.isPrimitive()) {
            targetType = (Class<T>) objectiveClass(targetType);
        }

        Converter converter = converters.get(new ConvertiblePair(source.getClass(), targetType));

        if (converter == null && targetType.isArray()) {
            converter = converters.get(new ConvertiblePair(source.getClass(), Arrays.class));
        }

        if (converter == null && targetType.isEnum()) {
            converter = converters.get(new ConvertiblePair(source.getClass(), Enum.class));
        }
        if (converter != null) {
            return (T) converter.convert(source, targetType);
        }

        return (T) source;
    }

    /**
     * Get an array class of the given class.
     *
     * @param klass to get an array class of
     * @param <C>   the targeted class
     * @return an array class of the given class
     */
    public static <C> Class<C[]> arrayClass(Class<C> klass) {
        return (Class<C[]>) Array.newInstance(klass, 0).getClass();
    }

    /**
     * Get the class that extends {@link Object} that represent the given class.
     *
     * @param klass to get the object class of
     * @return the class that extends Object class and represent the given class
     */
    public static Class<?> objectiveClass(Class<?> klass) {
        Class<?> component = klass.getComponentType();
        if (component != null) {
            if (component.isPrimitive() || component.isArray())
                return arrayClass(objectiveClass(component));
        } else if (klass.isPrimitive()) {
            if (klass == char.class)
                return Character.class;
            if (klass == int.class)
                return Integer.class;
            if (klass == boolean.class)
                return Boolean.class;
            if (klass == byte.class)
                return Byte.class;
            if (klass == double.class)
                return Double.class;
            if (klass == float.class)
                return Float.class;
            if (klass == long.class)
                return Long.class;
            if (klass == short.class)
                return Short.class;
        }

        return klass;
    }

}
