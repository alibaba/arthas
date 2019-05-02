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
import org.mvel2.compiler.BlankLiteral;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.NullType;

/**
 * @author Christopher Brock
 */
public class LiteralNode extends ASTNode {

    public LiteralNode(Object literal, Class type, ParserContext pCtx) {
        this(literal, pCtx);
        this.egressType = type;
    }

    public LiteralNode(Object literal, ParserContext pCtx) {
        super(pCtx);
        if ((this.literal = literal) != null) {
            if ((this.egressType = literal.getClass()) == BlankLiteral.class) this.egressType = Object.class;
        } else {
            this.egressType = NullType.class;
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return literal;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        return literal;
    }

    public Object getLiteralValue() {
        return literal;
    }

    public void setLiteralValue(Object literal) {
        this.literal = literal;
    }

    public boolean isLiteral() {
        return true;
    }

    @Override
    public String toString() {
        return "Literal<" + literal + ">";
    }
}
