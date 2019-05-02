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
package org.mvel2.integration;

import org.mvel2.ast.ASTNode;

/**
 * An interceptor can be used to decorate functionality into an expression or to hook into external functionality, such
 * as to log an event or fire some other event.
 *
 * @author Christopher Brock
 */
public interface Interceptor {

    public static final int NORMAL_FLOW = 0;
    public static final int SKIP = 1;
    public static final int END = 2;

    /**
     * This method is executed before the wrapped statement.
     *
     * @param node    The ASTNode wrapped by the interceptor
     * @param factory The variable factory
     * @return The response code.  Should return 0.
     */
    public int doBefore(ASTNode node, VariableResolverFactory factory);

    /**
     * This method is called after the wrapped statement has completed.  A copy of the top-value of the execution
     * stack is also availablehere.
     *
     * @param exitStackValue The value on the top of the stack after executing the statement.
     * @param node           The ASTNode wrapped by the interceptor
     * @param factory        The variable factory
     * @return The response code.  Should return 0.
     */
    public int doAfter(Object exitStackValue, ASTNode node, VariableResolverFactory factory);
}
