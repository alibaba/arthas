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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mvel2.ParserConfiguration;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

public class ClassImportResolverFactory extends BaseVariableResolverFactory {

    private Set<String> packageImports;
    private ClassLoader classLoader;
    private Map<String, Object> imports;
    private Map<String, Object> dynImports;

    public ClassImportResolverFactory(ParserConfiguration pCfg, VariableResolverFactory nextFactory, boolean compiled) {
        if (pCfg != null) {
            if (!compiled) {
                packageImports = pCfg.getPackageImports();
            }
            classLoader = pCfg.getClassLoader();
            imports = Collections.unmodifiableMap(pCfg.getImports());
        } else {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        this.nextFactory = nextFactory;
    }

    public VariableResolver createVariable(String name, Object value) {
        if (nextFactory == null) {
            nextFactory = new MapVariableResolverFactory(new HashMap());
        }

        return nextFactory.createVariable(name, value);
    }

    public VariableResolver createVariable(String name, Object value, Class type) {
        if (nextFactory == null) {
            nextFactory = new MapVariableResolverFactory(new HashMap());
        }

        return nextFactory.createVariable(name, value);
    }

    public Class addClass(Class clazz) {
        if (dynImports == null) dynImports = new HashMap<String, Object>();
        dynImports.put(clazz.getSimpleName(), clazz);
        return clazz;
    }

    public boolean isTarget(String name) {
        if (name == null) return false;
        return (imports != null && imports.containsKey(name)) || (dynImports != null && dynImports.containsKey(name));
    }

    public boolean isResolveable(String name) {
        if (name == null) return false;
        if ((imports != null && imports.containsKey(name)) || (dynImports != null && dynImports.containsKey(name))
                || isNextResolveable(name)) {
            return true;
        } else if (packageImports != null) {
            for (String s : packageImports) {
                try {
                    addClass(classLoader.loadClass(s + "." + name));
                    return true;
                } catch (ClassNotFoundException e) {
                    // do nothing;
                } catch (NoClassDefFoundError e) {
                    // do nothing;
                }
            }
        }
        return false;
    }

    @Override
    public VariableResolver getVariableResolver(String name) {
        if (isResolveable(name)) {
            if (imports != null && imports.containsKey(name)) {
                return new SimpleValueResolver(imports.get(name));
            } else if (dynImports != null && dynImports.containsKey(name)) {
                return new SimpleValueResolver(dynImports.get(name));
            } else if (nextFactory != null) {
                return nextFactory.getVariableResolver(name);
            }
        }

        throw new UnresolveablePropertyException("unable to resolve variable '" + name + "'");
    }

    public void clear() {
        //   variableResolvers.clear();
    }

    public Map<String, Object> getImportedClasses() {
        return imports;
    }

    public void addPackageImport(String packageName) {
        if (packageImports == null) packageImports = new HashSet<String>();
        packageImports.add(packageName);
    }

    @Override
    public Set<String> getKnownVariables() {
        return nextFactory == null ? new HashSet(0) : nextFactory.getKnownVariables();
    }
}
