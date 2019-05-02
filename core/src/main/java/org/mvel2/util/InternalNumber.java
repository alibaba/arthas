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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class InternalNumber extends BigDecimal {

    public InternalNumber(char[] chars, int i, int i1) {
        super(chars, i, i1);
    }

    public InternalNumber(char[] chars, int i, int i1, MathContext mathContext) {
        super(chars, i, i1, mathContext);
    }

    public InternalNumber(char[] chars) {
        super(chars);
    }

    public InternalNumber(char[] chars, MathContext mathContext) {
        super(chars, mathContext);
    }

    public InternalNumber(String s) {
        super(s);
    }

    public InternalNumber(String s, MathContext mathContext) {
        super(s, mathContext);
    }

    public InternalNumber(double v) {
        super(v);
    }

    public InternalNumber(double v, MathContext mathContext) {
        super(v, mathContext);
    }

    public InternalNumber(BigInteger bigInteger) {
        super(bigInteger);
    }

    public InternalNumber(BigInteger bigInteger, MathContext mathContext) {
        super(bigInteger, mathContext);
    }

    public InternalNumber(BigInteger bigInteger, int i) {
        super(bigInteger, i);
    }

    public InternalNumber(BigInteger bigInteger, int i, MathContext mathContext) {
        super(bigInteger, i, mathContext);
    }

    public InternalNumber(int i) {
        super(i);
    }

    public InternalNumber(int i, MathContext mathContext) {
        super(i, mathContext);
    }

    public InternalNumber(long l) {
        super(l);
    }

    public InternalNumber(long l, MathContext mathContext) {
        super(l, mathContext);
    }
}
