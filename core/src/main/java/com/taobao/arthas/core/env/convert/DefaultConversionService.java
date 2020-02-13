package com.taobao.arthas.core.env;

public class DefaultConversionService implements ConfigurableConversionService {

    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public <T> T convert(Object source, Class<T> targetType) {
        // TODO Auto-generated method stub
        return (T) source;
    }

}
