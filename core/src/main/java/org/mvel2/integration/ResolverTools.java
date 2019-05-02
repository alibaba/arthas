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
 * A set of tools for dealing with factorys, specifically to make chaining issues easy to deal with.
 *
 * @author Christopher Brock
 */
public class ResolverTools {

    /**
     * Based on a root factory, append the new factory to the end of the chain.
     *
     * @param root       The root factory
     * @param newFactory The new factory
     * @return An instance of the new factory
     */
    public static <T extends VariableResolverFactory> T appendFactory(VariableResolverFactory root, T newFactory) {
        if (root.getNextFactory() == null) {
            root.setNextFactory(newFactory);
        } else {
            VariableResolverFactory vrf = root;

            while (vrf.getNextFactory() != null) {
                vrf = vrf.getNextFactory();
            }
            vrf.setNextFactory(newFactory);
        }

        return newFactory;
    }

    /**
     * Based on the root factory, insert the new factory right after the root, and before any other in the chain.
     *
     * @param root       The root factory
     * @param newFactory The new factory
     * @return An instance of the new factory.
     */
    public static <T extends VariableResolverFactory> T insertFactory(VariableResolverFactory root, T newFactory) {
        if (root.getNextFactory() == null) {
            root.setNextFactory(newFactory);
        } else {
            newFactory.setNextFactory(root.getNextFactory());
            root.setNextFactory(newFactory);
        }

        return newFactory;
    }
}
