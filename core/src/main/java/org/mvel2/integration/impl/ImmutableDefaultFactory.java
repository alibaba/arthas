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

package org.mvel2.integration.impl;

import java.util.Set;

import org.mvel2.ScriptRuntimeException;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

public class ImmutableDefaultFactory implements VariableResolverFactory {

    private boolean tiltFlag;

    private void throwError() {
        throw new ScriptRuntimeException("cannot assign variables; no variable resolver factory available.");
    }

    public VariableResolver createVariable(String name, Object value) {
        throwError();
        return null;
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value) {
        throwError();
        return null;
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type) {
        throwError();
        return null;
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> typee) {
        throwError();
        return null;
    }

    public VariableResolver setIndexedVariableResolver(int index, VariableResolver variableResolver) {
        throwError();
        return null;
    }

    public VariableResolverFactory getNextFactory() {
        return null;
    }

    public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory) {
        throw new RuntimeException("cannot chain to this factory");
    }

    public VariableResolver getVariableResolver(String name) {
        throw new UnresolveablePropertyException(name);
    }

    public VariableResolver getIndexedVariableResolver(int index) {
        throwError();
        return null;
    }

    public boolean isTarget(String name) {
        return false;
    }

    public boolean isResolveable(String name) {
        return false;
    }

    public Set<String> getKnownVariables() {
        return null;
    }

    public int variableIndexOf(String name) {
        return -1;
    }

    public boolean isIndexedFactory() {
        return false;
    }

    public boolean tiltFlag() {
        return tiltFlag;
    }

    public void setTiltFlag(boolean tilt) {
        this.tiltFlag = tilt;
    }
}
