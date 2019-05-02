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

package org.mvel2.optimizers.dynamic;

import java.util.LinkedList;

import org.mvel2.util.MVELClassLoader;

public class DynamicClassLoader extends ClassLoader implements MVELClassLoader {

    private final LinkedList<DynamicAccessor> allAccessors = new LinkedList<DynamicAccessor>();
    private int totalClasses;
    private int tenureLimit;

    public DynamicClassLoader(ClassLoader classLoader, int tenureLimit) {
        super(classLoader);
        this.tenureLimit = tenureLimit;
    }

    public Class defineClassX(String className, byte[] b, int start, int end) {
        totalClasses++;
        return super.defineClass(className, b, start, end);
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public DynamicAccessor registerDynamicAccessor(DynamicAccessor accessor) {
        synchronized (allAccessors) {
            allAccessors.add(accessor);
            while (allAccessors.size() > tenureLimit) {
                DynamicAccessor da = allAccessors.removeFirst();
                if (da != null) {
                    da.deoptimize();
                }
            }
            assert accessor != null;
            return accessor;
        }
    }

    public void deoptimizeAll() {
        synchronized (allAccessors) {
            for (DynamicAccessor a : allAccessors) {
                if (a != null) a.deoptimize();
            }
            allAccessors.clear();
        }
    }

    public boolean isOverloaded() {
        return tenureLimit < totalClasses;
    }
}
