
package com.taobao.arthas.core.env.convert;

@SuppressWarnings("rawtypes")
final class StringToEnumConverter<T extends Enum> implements Converter<String, T> {
    @SuppressWarnings("unchecked")
    @Override
    public T convert(String source, Class<T> targetType) {
        return (T) Enum.valueOf(targetType, source);
    }

}
