/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.util;

/**
 * An implementation of Knuth's soundex algorithm.  Used by the <tt>soundslike</tt> operator.
 */
public class Soundex {

    /* Implements the mapping
    * from: AEHIOUWYBFPVCGJKQSXZDTLMNR
    * to:   00000000111122222222334556
    */
    public static final char[] MAP = {
            //A   B    C    D    E    F    G    H    I    J    K    L    M
            '0', '1', '2', '3', '0', '1', '2', '0', '0', '2', '2', '4', '5',
            //N  O   P   W   R   S   T   U   V   W   X   Y   Z
            '5', '0', '1', '2', '6', '2', '3', '0', '1', '0', '2', '0', '2' };

    /**
     * Convert the given String to its Soundex code.
     *
     * @param s input string
     * @return null If the given string can't be mapped to Soundex.
     */
    public static String soundex(String s) {
        char[] ca = s.toUpperCase().toCharArray();

        StringBuilder res = new StringBuilder();
        char c, prev = '?';

        // Main loop: find up to 4 chars that map.
        for (int i = 0; i < ca.length && res.length() < 4 && (c = ca[i]) != ','; i++) {

            // Check to see if the given character is alphabetic.
            // Text is already converted to uppercase. Algorithm
            // only handles ASCII letters, do NOT use Character.isLetter()!
            // Also, skip double letters.
            if (c >= 'A' && c <= 'Z' && c != prev) {
                prev = c;

                char m = MAP[c - 'A'];
                if (m != '0') res.append(m);
            }
        }

        if (res.length() == 0) return null;

        for (int i = res.length(); i < 4; i++)
            res.append('0');

        return res.toString();
    }
}
