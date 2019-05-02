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

import static java.lang.System.currentTimeMillis;

import org.mvel2.ParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.OptimizerFactory;

public class DynamicSetAccessor implements DynamicAccessor {

    private final Accessor _safeAccessor;
    private char[] property;
    private int start;
    private int offset;
    private boolean opt = false;
    private int runcount = 0;
    private long stamp;
    private ParserContext context;
    private Accessor _accessor;
    private String description;

    public DynamicSetAccessor(ParserContext context, char[] property, int start, int offset, Accessor _accessor) {
        assert _accessor != null;
        this._safeAccessor = this._accessor = _accessor;
        this.context = context;

        this.property = property;
        this.start = start;
        this.offset = offset;

        this.stamp = System.currentTimeMillis();
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        if (!opt) {
            if (++runcount > DynamicOptimizer.tenuringThreshold) {
                if ((currentTimeMillis() - stamp) < DynamicOptimizer.timeSpan) {
                    opt = true;
                    return optimize(ctx, elCtx, variableFactory, value);
                } else {
                    runcount = 0;
                    stamp = currentTimeMillis();
                }
            }
        }

        _accessor.setValue(ctx, elCtx, variableFactory, value);
        return value;
    }

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        throw new RuntimeException("value cannot be read with this accessor");
    }

    private Object optimize(Object ctx, Object elCtx, VariableResolverFactory variableResolverFactory, Object value) {
        if (DynamicOptimizer.isOverloaded()) {
            DynamicOptimizer.enforceTenureLimit();
        }

        AccessorOptimizer ao = OptimizerFactory.getAccessorCompiler("ASM");
        _accessor = ao.optimizeSetAccessor(context, property, start, offset, ctx, elCtx, variableResolverFactory, false, value,
                value != null ? value.getClass() : Object.class);
        assert _accessor != null;

        return value;
    }

    public void deoptimize() {
        this._accessor = this._safeAccessor;
        opt = false;
        runcount = 0;
        stamp = currentTimeMillis();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Class getKnownEgressType() {
        return _safeAccessor.getKnownEgressType();
    }
}