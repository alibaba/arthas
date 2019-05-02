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

/**
 * This interface allows an external property handler to resolve a property against the provided context.
 *
 * @see org.mvel2.optimizers.impl.asm.ProducesBytecode
 */
public interface PropertyHandler {

    /**
     * Retrieves the value of the property.
     *
     * @param name            - the name of the property to be resolved.
     * @param contextObj      - the current context object.
     * @param variableFactory - the root variable factory provided by the runtime.
     * @return - the value of the property.
     */
    public Object getProperty(String name, Object contextObj, VariableResolverFactory variableFactory);

    /**
     * Sets the value of the property.
     *
     * @param name            - the name of the property to be resolved.
     * @param contextObj      - the current context object.
     * @param variableFactory - the root variable factory provided by the runtime.
     * @param value           - the value to be set to the resolved property
     * @return - the resultant value of the property (should normally be the same as the value passed)
     */
    public Object setProperty(String name, Object contextObj, VariableResolverFactory variableFactory, Object value);
}
