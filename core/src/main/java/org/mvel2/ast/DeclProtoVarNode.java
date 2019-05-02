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

import static org.mvel2.util.ParseTools.checkNameSafety;

import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;

/**
 * @author Christopher Brock
 */
public class DeclProtoVarNode extends ASTNode implements Assignment {

    private String name;

    public DeclProtoVarNode(String name, Proto type, int fields, ParserContext pCtx) {
        super(pCtx);
        this.egressType = Proto.ProtoInstance.class;
        checkNameSafety(this.name = name);

        if ((fields & COMPILE_IMMEDIATE) != 0) {
            pCtx.addVariable(name, egressType, true);
        }
    }

    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (!factory.isResolveable(name)) factory.createVariable(name, null, egressType);
        else throw new RuntimeException("variable defined within scope: " + name);
        return null;
    }

    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        if (!factory.isResolveable(name)) factory.createVariable(name, null, egressType);
        else throw new RuntimeException("variable defined within scope: " + name);

        return null;
    }

    public String getName() {
        return name;
    }

    public String getAssignmentVar() {
        return name;
    }

    public char[] getExpression() {
        return new char[0];
    }

    public boolean isAssignment() {
        return true;
    }

    public boolean isNewDeclaration() {
        return true;
    }

    public void setValueStatement(ExecutableStatement stmt) {
        throw new RuntimeException("illegal operation");
    }

    public String toString() {
        return "var:" + name;
    }
}