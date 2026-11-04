
package org.example.jfranalyzerbackend.util;

import java.util.ArrayList;
import java.util.List;

public class GCUtil {

    private static final List<String> PARALLEL_GC = new ArrayList<String>() {{
        add("G1New");
        add("ParNew");
        add("ParallelScavenge");
        add("ParallelOld");
    }};

    private static final List<String> CONCURRENT_GC = new ArrayList<String>() {{
        add("G1Old");
        add("ConcurrentMarkSweep");
    }};

    private static final List<String> SERIAL_GC = new ArrayList<String>() {{
        add("SerialOld");
    }};

    public static boolean isConcGC(String name) {
        return CONCURRENT_GC.contains(name);
    }

    public static boolean isParallelGC(String name) {
        return PARALLEL_GC.contains(name);
    }

    public static boolean isSerialGC(String name) {
        return SERIAL_GC.contains(name);
    }
}
