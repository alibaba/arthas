/**
 * MVEL 2.0
 * Copyright (C) 2007  MVFLEX/Valhalla Project and the Codehaus
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

package org.mvel2;

/**
 * Contains constants for standard internal types.
 */
public interface DataTypes {

    public static final int NULL = -1;

    public static final int OBJECT = 0;
    public static final int STRING = 1;
    public static final int SHORT = 100;
    public static final int INTEGER = 101;
    public static final int LONG = 102;
    public static final int DOUBLE = 103;
    public static final int FLOAT = 104;
    public static final int BOOLEAN = 7;
    public static final int CHAR = 8;
    public static final int BYTE = 9;

    public static final int W_BOOLEAN = 15;

    public static final int COLLECTION = 50;

    public static final int W_SHORT = 105;
    public static final int W_INTEGER = 106;
    public static final int W_LONG = 107;
    public static final int W_FLOAT = 108;
    public static final int W_DOUBLE = 109;

    public static final int W_CHAR = 112;
    public static final int W_BYTE = 113;

    public static final int BIG_DECIMAL = 110;
    public static final int BIG_INTEGER = 111;

    public static final int EMPTY = 200;

    public static final int UNIT = 300;
}
