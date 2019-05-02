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

package org.mvel2;

import static org.mvel2.util.ParseTools.forNameWithInner;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mvel2.ast.Proto;
import org.mvel2.compiler.AbstractParser;
import org.mvel2.integration.Interceptor;
import org.mvel2.util.MethodStub;

/**
 * The resusable parser configuration object.
 */
public class ParserConfiguration implements Serializable {

    protected final Map<String, Object> imports = new ConcurrentHashMap<String, Object>();
    private final transient Set<String> nonValidImports = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    protected HashSet<String> packageImports;
    protected Map<String, Interceptor> interceptors;
    protected transient ClassLoader classLoader;
    private boolean allowNakedMethCall = MVEL.COMPILER_OPT_ALLOW_NAKED_METH_CALL;

    private boolean allowBootstrapBypass = true;

    public ParserConfiguration() {
    }

    public ParserConfiguration(Map<String, Object> imports, Map<String, Interceptor> interceptors) {
        addAllImports(imports);
        this.interceptors = interceptors;
    }

    public ParserConfiguration(Map<String, Object> imports, HashSet<String> packageImports, Map<String, Interceptor> interceptors) {
        addAllImports(imports);
        this.packageImports = packageImports;
        this.interceptors = interceptors;
    }

    public HashSet<String> getPackageImports() {
        return packageImports;
    }

    public void setPackageImports(HashSet<String> packageImports) {
        this.packageImports = packageImports;
    }

    public Class getImport(String name) {
        if (imports.containsKey(name) && imports.get(name) instanceof Class) {
            return (Class) imports.get(name);
        }
        return (Class) (AbstractParser.LITERALS.get(name) instanceof Class ? AbstractParser.LITERALS.get(name) : null);
    }

    public MethodStub getStaticImport(String name) {
        return (MethodStub) imports.get(name);
    }

    public Object getStaticOrClassImport(String name) {
        return imports.containsKey(name) ? imports.get(name) : AbstractParser.LITERALS.get(name);
    }

    public void addPackageImport(String packageName) {
        if (packageImports == null) packageImports = new LinkedHashSet<String>();
        packageImports.add(packageName);
        if (!addClassMemberStaticImports(packageName)) packageImports.add(packageName);
    }

    private boolean addClassMemberStaticImports(String packageName) {
        try {
            Class c = Class.forName(packageName);
            if (c.isEnum()) {

                //noinspection unchecked
                for (Enum e : (EnumSet<?>) EnumSet.allOf(c)) {
                    imports.put(e.name(), e);
                }
                return true;
            } else {
                for (Field f : c.getDeclaredFields()) {
                    if ((f.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) == (Modifier.STATIC | Modifier.PUBLIC)) {
                        imports.put(f.getName(), f.get(null));
                    }
                }

            }
        } catch (ClassNotFoundException e) {
            // do nothing.
        } catch (IllegalAccessException e) {
            throw new RuntimeException("error adding static imports for: " + packageName, e);
        }
        return false;
    }

    public void addAllImports(Map<String, Object> imports) {
        if (imports == null) return;

        Object o;

        for (Map.Entry<String, Object> entry : imports.entrySet()) {
            if ((o = entry.getValue()) instanceof Method) {
                this.imports.put(entry.getKey(), new MethodStub((Method) o));
            } else {
                this.imports.put(entry.getKey(), o);
            }
        }
    }

    private boolean checkForDynamicImport(String className) {
        if (packageImports == null) return false;
        if (!Character.isJavaIdentifierStart(className.charAt(0))) return false;
        if (nonValidImports.contains(className)) return false;

        int found = 0;
        Class cls = null;
        for (String pkg : packageImports) {
            try {
                cls = forNameWithInner(pkg + "." + className, getClassLoader());
                found++;
            } catch (Throwable cnfe) {
                // do nothing.
            }
        }

        if (found > 1) throw new RuntimeException("ambiguous class name: " + className);
        if (found == 1) {
            addImport(className, cls);
            return true;
        }

        cacheNegativeHitForDynamicImport(className);
        return false;
    }

    public boolean hasImport(String name) {
        return (imports.containsKey(name)) || AbstractParser.CLASS_LITERALS.containsKey(name) || checkForDynamicImport(name);
    }

    public void addImport(Class cls) {
        addImport(cls.getSimpleName(), cls);
    }

    public void addImport(String name, Class cls) {
        this.imports.put(name, cls);
    }

    public void addImport(String name, Proto proto) {
        this.imports.put(name, proto);
    }

    public void addImport(String name, Method method) {
        addImport(name, new MethodStub(method));
    }

    public void addImport(String name, MethodStub method) {
        this.imports.put(name, method);
    }

    public Map<String, Interceptor> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(Map<String, Interceptor> interceptors) {
        this.interceptors = interceptors;
    }

    public Map<String, Object> getImports() {
        return imports;
    }

    public void setImports(HashMap<String, Object> imports) {
        // TODO: this method is here for backward compatibility. Could it be removed/deprecated?
        setAllImports(imports);
    }

    public void setImports(Map<String, Object> imports) {
        if (imports == null) return;

        Object val;

        for (Map.Entry<String, Object> entry : imports.entrySet()) {
            if ((val = entry.getValue()) instanceof Class) {
                addImport(entry.getKey(), (Class) val);
            } else if (val instanceof Method) {
                addImport(entry.getKey(), (Method) val);
            } else if (val instanceof MethodStub) {
                addImport(entry.getKey(), (MethodStub) val);
            } else if (val instanceof Proto) {
                addImport(entry.getKey(), (Proto) entry.getValue());
            } else {
                throw new RuntimeException("invalid element in imports map: " + entry.getKey() + " (" + val + ")");
            }
        }
    }

    public boolean hasImports() {
        return !imports.isEmpty() || (packageImports != null && packageImports.size() != 0);
    }

    public ClassLoader getClassLoader() {
        return classLoader == null ? classLoader = Thread.currentThread().getContextClassLoader() : classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setAllImports(Map<String, Object> imports) {
        this.imports.clear();
        if (imports != null) this.imports.putAll(imports);
    }

    private void cacheNegativeHitForDynamicImport(String negativeHit) {
        nonValidImports.add(negativeHit);
    }

    public void flushCaches() {
        nonValidImports.clear();
    }

    public boolean isAllowNakedMethCall() {
        return allowNakedMethCall;
    }

    public void setAllowNakedMethCall(boolean allowNakedMethCall) {
        this.allowNakedMethCall = allowNakedMethCall;
    }

    public boolean isAllowBootstrapBypass() {
        return allowBootstrapBypass;
    }

    public void setAllowBootstrapBypass(boolean allowBootstrapBypass) {
        this.allowBootstrapBypass = allowBootstrapBypass;
    }
}
