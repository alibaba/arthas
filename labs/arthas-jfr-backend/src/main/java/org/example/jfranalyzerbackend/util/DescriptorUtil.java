package org.example.jfranalyzerbackend.util;


import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Type;

public class DescriptorUtil {
    private final Map<String, String> CACHE = new HashMap<>();
    public String decodeMethodArgs(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return "";
        }
        if (CACHE.containsKey(descriptor)) {
            return CACHE.get(descriptor);
        }
        Type methodType = Type.getMethodType(descriptor);
        StringBuilder b = new StringBuilder("(");
        Type[] argTypes = methodType.getArgumentTypes();
        for (int ix = 0; ix < argTypes.length; ix++) {
            if (ix != 0) {
                b.append(", ");
            }
            b.append(trimPackage(argTypes[ix].getClassName()));
        }
        b.append(')');
        String str = b.toString();
        CACHE.put(descriptor, str);
        return str;
    }

    private static String trimPackage(String className) {
        return className.contains(".") ? className.substring(className.lastIndexOf(".") + 1) : className;
    }
}