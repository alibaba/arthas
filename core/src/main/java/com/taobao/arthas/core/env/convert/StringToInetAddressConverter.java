package com.taobao.arthas.core.env.convert;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class StringToInetAddressConverter implements Converter<String, InetAddress> {

    @Override
    public InetAddress convert(String source, Class<InetAddress> targetType) {
        try {
            return InetAddress.getByName(source);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid InetAddress value '" + source + "'", e);
        }
    }

}
