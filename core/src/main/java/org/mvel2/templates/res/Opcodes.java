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

package org.mvel2.templates.res;

public interface Opcodes {

    public static int IF = 1;
    public static int ELSE = 2;
    public static int FOREACH = 3;
    public static int END = 10;

    public static int INCLUDE_FILE = 50;
    public static int INCLUDE_NAMED = 51;
    public static int COMMENT = 52;
    public static int CODE = 53;
    public static int EVAL = 55;

    public static int DECLARE = 54;

    public static int STOP = 70;
}
