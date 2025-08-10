/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.example.jfranalyzerbackend.util;

import java.util.ArrayList;
import java.util.List;

public class GCUtil {
    // JDK 8 default: ParallelScavenge + ParallelOld
    // CMS: ParNew + ConcurrentMarkSweep + SerialOld
    // G1: G1New + G1Old + SerialOld

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
