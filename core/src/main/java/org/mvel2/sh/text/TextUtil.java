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

package org.mvel2.sh.text;

import static java.lang.String.valueOf;

public class TextUtil {

    public static String pad(int colLength, int tabPos) {
        StringBuilder sAppend = new StringBuilder();
        for (int len = tabPos - colLength; len != -1; len--) {
            sAppend.append(' ');
        }

        return sAppend.toString();
    }

    public static String paint(char c, int amount) {
        StringBuilder append = new StringBuilder();
        for (; amount != -1; amount--) {
            append.append(c);
        }
        return append.toString();
    }

    public static String padTwo(Object first, Object second, int tab) {
        return new StringBuilder(valueOf(first)).append(pad(valueOf(first).length(), tab)).append(second).toString();
    }
}
