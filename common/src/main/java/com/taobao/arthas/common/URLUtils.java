package com.taobao.arthas.common;

import java.net.URISyntaxException;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 *
 * @author hengyunabc 2019-03-22
 *
 */
public class URLUtils {

    public static String classLocation(Class<?> clazz) {
        ProtectionDomain domain = clazz.getProtectionDomain();
        CodeSource codeSource = domain.getCodeSource();
        if (codeSource != null) {
            try {
                return codeSource.getLocation().toURI().getSchemeSpecificPart();
            } catch (URISyntaxException e) {
                // ignore
            }
        }
        return null;
    }

}
