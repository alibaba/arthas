package org.mvel2.conversion;

import org.mvel2.ConversionHandler;

public class CompositeCH implements ConversionHandler {

    private final ConversionHandler[] converters;

    public CompositeCH(ConversionHandler... converters) {
        this.converters = converters;
    }

    public Object convertFrom(Object in) {
        for (ConversionHandler converter : converters) {
            if (converter.canConvertFrom(in.getClass())) return converter.convertFrom(in);
        }
        return null;
    }

    public boolean canConvertFrom(Class cls) {
        for (ConversionHandler converter : converters) {
            if (converter.canConvertFrom(cls)) return true;
        }
        return false;
    }
}
