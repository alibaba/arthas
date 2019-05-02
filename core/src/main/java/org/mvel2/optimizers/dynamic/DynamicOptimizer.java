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

import static java.lang.Thread.currentThread;
import static org.mvel2.optimizers.OptimizerFactory.SAFE_REFLECTIVE;
import static org.mvel2.optimizers.OptimizerFactory.getAccessorCompiler;
import static org.mvel2.optimizers.impl.asm.ASMAccessorOptimizer.setMVELClassLoader;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mvel2.ParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AbstractOptimizer;
import org.mvel2.optimizers.AccessorOptimizer;

public class DynamicOptimizer extends AbstractOptimizer implements AccessorOptimizer {

    public static final int REGULAR_ACCESSOR = 0;
    public static final int SET_ACCESSOR = 1;
    public static final int COLLECTION = 2;
    public static final int OBJ_CREATION = 3;
    private static final Object oLock = new Object();
    public static int tenuringThreshold = 50;
    public static long timeSpan = 100;
    public static int maximumTenure = 1500;
    public static int totalRecycled = 0;
    private volatile static DynamicClassLoader classLoader;
    private static volatile boolean useSafeClassloading = false;
    private static ReadWriteLock lock = new ReentrantReadWriteLock();
    private static Lock readLock = lock.readLock();
    private static Lock writeLock = lock.writeLock();
    private AccessorOptimizer firstStage = getAccessorCompiler(SAFE_REFLECTIVE);

    private static void _init() {
        setMVELClassLoader(classLoader = new DynamicClassLoader(currentThread().getContextClassLoader(), maximumTenure));
    }

    public static void enforceTenureLimit() {
        writeLock.lock();
        try {
            if (classLoader.isOverloaded()) {
                classLoader.deoptimizeAll();
                totalRecycled = +classLoader.getTotalClasses();
                _init();
            }
        } finally {
            writeLock.unlock();
        }
    }

    public static boolean isOverloaded() {
        return classLoader.isOverloaded();
    }

    public void init() {
        _init();
    }

    public Accessor optimizeAccessor(ParserContext pCtx, char[] property, int start, int offset, Object ctx, Object thisRef,
            VariableResolverFactory factory, boolean rootThisRef, Class ingressType) {
        readLock.lock();
        try {
            pCtx.optimizationNotify();
            return classLoader.registerDynamicAccessor(new DynamicGetAccessor(pCtx, property, start, offset, 0,
                    firstStage.optimizeAccessor(pCtx, property, start, offset, ctx, thisRef, factory, rootThisRef, ingressType)));
        } finally {
            readLock.unlock();
        }
    }

    public Accessor optimizeSetAccessor(ParserContext pCtx, char[] property, int start, int offset, Object ctx, Object thisRef,
            VariableResolverFactory factory, boolean rootThisRef, Object value, Class valueType) {

        readLock.lock();
        try {
            return classLoader.registerDynamicAccessor(new DynamicSetAccessor(pCtx, property, start, offset,
                    firstStage.optimizeSetAccessor(pCtx, property, start, offset, ctx, thisRef, factory, rootThisRef, value, valueType)));
        } finally {
            readLock.unlock();
        }
    }

    public Accessor optimizeCollection(ParserContext pCtx, Object rootObject, Class type, char[] property, int start, int offset,
            Object ctx, Object thisRef, VariableResolverFactory factory) {
        readLock.lock();
        try {
            return classLoader.registerDynamicAccessor(new DynamicCollectionAccessor(pCtx, rootObject, type, property, start, offset, 2,
                    firstStage.optimizeCollection(pCtx, rootObject, type, property, start, offset, ctx, thisRef, factory)));
        } finally {
            readLock.unlock();
        }
    }

    public Accessor optimizeObjectCreation(ParserContext pCtx, char[] property, int start, int offset, Object ctx, Object thisRef,
            VariableResolverFactory factory) {
        readLock.lock();
        try {
            return classLoader.registerDynamicAccessor(new DynamicGetAccessor(pCtx, property, start, offset, 3,
                    firstStage.optimizeObjectCreation(pCtx, property, start, offset, ctx, thisRef, factory)));
        } finally {
            readLock.unlock();
        }
    }

    public Object getResultOptPass() {
        return firstStage.getResultOptPass();
    }

    public Class getEgressType() {
        return firstStage.getEgressType();
    }

    public boolean isLiteralOnly() {
        return firstStage.isLiteralOnly();
    }
}
