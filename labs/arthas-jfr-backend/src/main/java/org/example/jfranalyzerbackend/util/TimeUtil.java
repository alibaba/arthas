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

import static java.util.concurrent.TimeUnit.*;

public class TimeUtil {
    public static long parseTimespan(String s) {
        long timeSpan = Long.parseLong(s.substring(0, s.length() - 2).trim());
        if (s.endsWith("ns")) {
            return timeSpan;
        }
        if (s.endsWith("us")) {
            return NANOSECONDS.convert(timeSpan, MICROSECONDS);
        }
        if (s.endsWith("ms")) {
            return NANOSECONDS.convert(timeSpan, MILLISECONDS);
        }
        timeSpan = Long.parseLong(s.substring(0, s.length() - 1).trim());
        if (s.endsWith("s")) {
            return NANOSECONDS.convert(timeSpan, SECONDS);
        }
        if (s.endsWith("m")) {
            return 60 * NANOSECONDS.convert(timeSpan, SECONDS);
        }
        if (s.endsWith("h")) {
            return 60 * 60 * NANOSECONDS.convert(timeSpan, SECONDS);
        }
        if (s.endsWith("d")) {
            return 24 * 60 * 60 * NANOSECONDS.convert(timeSpan, SECONDS);
        }

        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            // Only accept values with units
            throw new NumberFormatException("Timespan '" + s + "' is invalid. Valid units are ns, us, s, m, h and d.");
        }
    }
}
