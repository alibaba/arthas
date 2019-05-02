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

package org.mvel2.optimizers;

import java.util.HashMap;
import java.util.Map;

import org.mvel2.optimizers.dynamic.DynamicOptimizer;
import org.mvel2.optimizers.impl.asm.ASMAccessorOptimizer;
import org.mvel2.optimizers.impl.refl.ReflectiveAccessorOptimizer;

public class OptimizerFactory {

    private static final Map<String, AccessorOptimizer> accessorCompilers = new HashMap<String, AccessorOptimizer>();
    public static String DYNAMIC = "dynamic";
    public static String SAFE_REFLECTIVE = "reflective";
    private static String defaultOptimizer;
    private static ThreadLocal<Class<? extends AccessorOptimizer>> threadOptimizer = new ThreadLocal<Class<? extends AccessorOptimizer>>();

    static {
        accessorCompilers.put(SAFE_REFLECTIVE, new ReflectiveAccessorOptimizer());
        accessorCompilers.put(DYNAMIC, new DynamicOptimizer());
        /**
         * By default, activate the JIT if ASM is present in the classpath
         */
        try {
            if (OptimizerFactory.class.getClassLoader() != null) {
                OptimizerFactory.class.getClassLoader().loadClass("org.mvel2.asm.ClassWriter");
            } else {
                ClassLoader.getSystemClassLoader().loadClass("org.mvel2.asm.ClassWriter");
            }
            accessorCompilers.put("ASM", new ASMAccessorOptimizer());
        } catch (ClassNotFoundException e) {
            defaultOptimizer = SAFE_REFLECTIVE;
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("[MVEL] Notice: Possible incorrect version of ASM present (3.0 required).  "
                    + "Disabling JIT compiler.  Reflective Optimizer will be used.");
            defaultOptimizer = SAFE_REFLECTIVE;
        }

        if (Boolean.getBoolean("mvel2.disable.jit")) setDefaultOptimizer(SAFE_REFLECTIVE);
        else setDefaultOptimizer(DYNAMIC);
    }

    public static AccessorOptimizer getDefaultAccessorCompiler() {
        try {
            return accessorCompilers.get(defaultOptimizer).getClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("unable to instantiate accessor compiler", e);
        }
    }

    public static AccessorOptimizer getAccessorCompiler(String name) {
        try {
            return accessorCompilers.get(name).getClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("unable to instantiate accessor compiler", e);
        }
    }

    public static AccessorOptimizer getThreadAccessorOptimizer() {
        if (threadOptimizer.get() == null) {
            threadOptimizer.set(getDefaultAccessorCompiler().getClass());
        }
        try {
            return threadOptimizer.get().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("unable to instantiate accessor compiler", e);
        }
    }

    public static void setThreadAccessorOptimizer(Class<? extends AccessorOptimizer> optimizer) {
        if (optimizer == null) throw new RuntimeException("null optimizer");
        threadOptimizer.set(optimizer);
    }

    public static void setDefaultOptimizer(String name) {
        try {
            //noinspection unchecked
            AccessorOptimizer ao = accessorCompilers.get(defaultOptimizer = name);
            ao.init();
            //clear optimizer so next call to getThreadAccessorOptimizer uses the default again, don't set thread optimizer
            //or else static initializers setting the default will unintentionally set up ThreadLocals
            threadOptimizer.set(null);
        } catch (Exception e) {
            throw new RuntimeException("unable to instantiate accessor compiler", e);
        }
    }

    public static void clearThreadAccessorOptimizer() {
        threadOptimizer.set(null);
        threadOptimizer.remove();
    }

    public static boolean isThreadAccessorOptimizerInitialized() {
        return threadOptimizer.get() != null;
    }
}
