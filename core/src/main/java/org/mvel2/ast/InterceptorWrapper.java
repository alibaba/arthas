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

import org.mvel2.ParserContext;
import org.mvel2.integration.Interceptor;
import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class InterceptorWrapper extends ASTNode {

    private Interceptor interceptor;
    private ASTNode node;

    public InterceptorWrapper(Interceptor interceptor, ASTNode node, ParserContext pCtx) {
        super(pCtx);
        this.interceptor = interceptor;
        this.node = node;
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        interceptor.doBefore(node, factory);
        interceptor.doAfter(ctx = node.getReducedValueAccelerated(ctx, thisValue, factory), node, factory);
        return ctx;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        interceptor.doBefore(node, factory);
        interceptor.doAfter(ctx = node.getReducedValue(ctx, thisValue, factory), node, factory);
        return ctx;
    }
}
