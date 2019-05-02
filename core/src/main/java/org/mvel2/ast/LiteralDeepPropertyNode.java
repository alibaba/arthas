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
package org.mvel2.ast;

import static org.mvel2.PropertyAccessor.get;
import static org.mvel2.optimizers.OptimizerFactory.getThreadAccessorOptimizer;

import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.AccessorOptimizer;
import org.mvel2.optimizers.OptimizerFactory;

/**
 * @author Christopher Brock
 */
@SuppressWarnings({ "CaughtExceptionImmediatelyRethrown" })
public class LiteralDeepPropertyNode extends ASTNode {

    private Object literal;

    public LiteralDeepPropertyNode(char[] expr, int start, int offset, int fields, Object literal, ParserContext pCtx) {
        super(pCtx);
        this.fields = fields;
        this.expr = expr;
        this.start = start;
        this.offset = offset;

        this.literal = literal;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (accessor != null) {
            return accessor.getValue(literal, thisValue, factory);
        } else {
            try {
                AccessorOptimizer aO = getThreadAccessorOptimizer();
                accessor = aO.optimizeAccessor(pCtx, expr, start, offset, literal, thisValue, factory, false, null);
                return aO.getResultOptPass();
            } finally {
                OptimizerFactory.clearThreadAccessorOptimizer();
            }
        }
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return get(expr, start, offset, literal, factory, thisValue, pCtx);
    }
}
