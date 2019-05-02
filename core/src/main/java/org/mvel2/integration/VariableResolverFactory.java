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

import java.io.Serializable;
import java.util.Set;

/**
 * A VariableResolverFactory is the primary integration point for tying in external variables.  The factory is
 * responsible for returing {@link org.mvel2.integration.VariableResolver}'s to the MVEL runtime.  Factories are
 * also structured in a chain to maintain locality-of-reference.
 */
public interface VariableResolverFactory extends Serializable {

    /**
     * Creates a new variable.  This probably doesn't need to be implemented in most scenarios.  This is
     * used for variable assignment.
     *
     * @param name  - name of the variable being created
     * @param value - value of the variable
     * @return instance of the variable resolver associated with the variable
     */
    public VariableResolver createVariable(String name, Object value);

    public VariableResolver createIndexedVariable(int index, String name, Object value);

    /**
     * Creates a new variable, and assigns a static type. It is expected the underlying factory and resolver
     * will enforce this.
     *
     * @param name  - name of the variable being created
     * @param value - value of the variable
     * @param type  - the static type
     * @return instance of the variable resolver associated with the variable
     */
    public VariableResolver createVariable(String name, Object value, Class<?> type);

    public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> typee);

    public VariableResolver setIndexedVariableResolver(int index, VariableResolver variableResolver);

    /**
     * Returns the next factory in the factory chain.  MVEL uses a hierarchical variable resolution strategy,
     * much in the same way as Classloaders in Java.   For performance reasons, it is the responsibility of
     * the individual VariableResolverFactory to pass off to the next one.
     *
     * @return instance of the next factory - null if none.
     */
    public VariableResolverFactory getNextFactory();

    /**
     * Sets the next factory in the chain. Proper implementation:
     * <code>
     * <p/>
     * return this.nextFactory = resolverFactory;
     * </code>
     *
     * @param resolverFactory - instance of next resolver factory
     * @return - instance of next resolver factory
     */
    public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory);

    /**
     * Return a variable resolver for the specified variable name.  This method is expected to traverse the
     * heirarchy of ResolverFactories.
     *
     * @param name - variable name
     * @return - instance of the VariableResolver for the specified variable
     */
    public VariableResolver getVariableResolver(String name);

    public VariableResolver getIndexedVariableResolver(int index);

    /**
     * Deterimines whether or not the current VariableResolverFactory is the physical target for the actual
     * variable.
     *
     * @param name - variable name
     * @return - boolean indicating whether or not factory is the physical target
     */
    public boolean isTarget(String name);

    /**
     * Determines whether or not the variable is resolver in the chain of factories.
     *
     * @param name - variable name
     * @return - boolean
     */
    public boolean isResolveable(String name);

    /**
     * Return a list of known variables inside the factory.  This method should not recurse into other factories.
     * But rather return only the variables living inside this factory.
     *
     * @return
     */
    public Set<String> getKnownVariables();

    public int variableIndexOf(String name);

    public boolean isIndexedFactory();

    public boolean tiltFlag();

    public void setTiltFlag(boolean tilt);
}
