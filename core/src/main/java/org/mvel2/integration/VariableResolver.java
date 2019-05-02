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

/**
 * A variable resolver is responsible for physically accessing a variable, for either read or write.  VariableResolver's
 * are obtained via a {@link VariableResolverFactory}.
 */
public interface VariableResolver extends Serializable {

    /**
     * Returns the name of external variable.
     *
     * @return A string representing the variable name.
     */
    public String getName();

    /**
     * This should return the type of the variable.  However, this is not completely necessary, and is particularily
     * only of benefit to systems that require use of MVEL's strict typing facilities.  In most cases, this implementation
     * can simply return: Object.class
     *
     * @return A Class instance representing the type of the target variable.
     */
    public Class getType();

    /*
    * If this is a declared variable of a static type, MVEL will make it known by passing the type here.
    */
    public void setStaticType(Class type);

    /**
     * Returns the bitset of special variable flags.  Internal use only.  This should just return 0 in custom
     * implentations.
     *
     * @return Bitset of special flags.
     */
    public int getFlags();

    /**
     * Returns the physical target value of the variable.
     *
     * @return The actual variable value.
     */
    public Object getValue();

    /**
     * Sets the value of the physical target value.
     *
     * @param value The new value.
     * @return value after any conversion
     */
    public void setValue(Object value);
}
