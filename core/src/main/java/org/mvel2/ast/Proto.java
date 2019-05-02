package org.mvel2.ast;

import static org.mvel2.DataConversion.canConvert;
import static org.mvel2.DataConversion.convert;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.integration.impl.SimpleValueResolver;
import org.mvel2.util.CallableProxy;
import org.mvel2.util.SimpleIndexHashMapWrapper;

public class Proto extends ASTNode {

    private String name;
    private Map<String, Receiver> receivers;
    private int cursorStart;
    private int cursorEnd;

    public Proto(String name, ParserContext pCtx) {
        super(pCtx);
        this.name = name;
        this.receivers = new SimpleIndexHashMapWrapper<String, Receiver>();
    }

    public Receiver declareReceiver(String name, Function function) {
        Receiver r = new Receiver(null, ReceiverType.FUNCTION, function);
        receivers.put(name, r);
        return r;
    }

    public Receiver declareReceiver(String name, Class type, ExecutableStatement initCode) {
        Receiver r = new Receiver(null, ReceiverType.PROPERTY, initCode);
        receivers.put(name, r);
        return r;
    }

    public Receiver declareReceiver(String name, ReceiverType type, ExecutableStatement initCode) {
        Receiver r = new Receiver(null, type, initCode);
        receivers.put(name, r);
        return r;
    }

    public ProtoInstance newInstance(Object ctx, Object thisCtx, VariableResolverFactory factory) {
        return new ProtoInstance(this, ctx, thisCtx, factory);
    }

    @Override
    public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createVariable(name, this);
        return this;
    }

    @Override
    public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
        factory.createVariable(name, this);
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "proto " + name;
    }

    public void setCursorPosition(int start, int end) {
        this.cursorStart = start;
        this.cursorEnd = end;
    }

    public int getCursorStart() {
        return cursorStart;
    }

    public int getCursorEnd() {
        return cursorEnd;
    }

    public enum ReceiverType {
        DEFERRED, FUNCTION, PROPERTY
    }

    public class Receiver implements CallableProxy {

        private ReceiverType type;
        private Object receiver;
        private ExecutableStatement initValue;
        private ProtoInstance instance;

        public Receiver(ProtoInstance protoInstance, ReceiverType type, Object receiver) {
            this.instance = protoInstance;
            this.type = type;
            this.receiver = receiver;
        }

        public Receiver(ProtoInstance protoInstance, ReceiverType type, ExecutableStatement stmt) {
            this.instance = protoInstance;
            this.type = type;
            this.initValue = stmt;
        }

        public Object call(Object ctx, Object thisCtx, VariableResolverFactory factory, Object[] parms) {
            switch (type) {
                case FUNCTION:
                    return ((Function) receiver).call(ctx, thisCtx, new InvokationContextFactory(factory, instance.instanceStates), parms);
                case PROPERTY:
                    return receiver;
                case DEFERRED:
                    throw new CompileException("unresolved prototype receiver", expr, start);
            }
            return null;
        }

        public Receiver init(ProtoInstance instance, Object ctx, Object thisCtx, VariableResolverFactory factory) {
            return new Receiver(instance, type,
                    type == ReceiverType.PROPERTY && initValue != null ? initValue.getValue(ctx, thisCtx, factory) : receiver);
        }

        public void setType(ReceiverType type) {
            this.type = type;
        }

        public void setInitValue(ExecutableStatement initValue) {
            this.initValue = initValue;
        }
    }

    public class ProtoInstance implements Map<String, Receiver> {

        private Proto protoType;
        private VariableResolverFactory instanceStates;
        private SimpleIndexHashMapWrapper<String, Receiver> receivers;

        public ProtoInstance(Proto protoType, Object ctx, Object thisCtx, VariableResolverFactory factory) {
            this.protoType = protoType;

            receivers = new SimpleIndexHashMapWrapper<String, Receiver>();
            for (Entry<String, Receiver> entry : protoType.receivers.entrySet()) {
                receivers.put(entry.getKey(), entry.getValue().init(this, ctx, thisCtx, factory));
            }

            instanceStates = new ProtoContextFactory(receivers);
        }

        public Proto getProtoType() {
            return protoType;
        }

        public int size() {
            return receivers.size();
        }

        public boolean isEmpty() {
            return receivers.isEmpty();
        }

        public boolean containsKey(Object key) {
            return receivers.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return receivers.containsValue(value);
        }

        public Receiver get(Object key) {
            return receivers.get(key);
        }

        public Receiver put(String key, Receiver value) {
            return receivers.put(key, value);
        }

        public Receiver remove(Object key) {
            return receivers.remove(key);
        }

        public void putAll(Map m) {
        }

        public void clear() {
        }

        public Set<String> keySet() {
            return receivers.keySet();
        }

        public Collection<Receiver> values() {
            return receivers.values();
        }

        public Set<Entry<String, Receiver>> entrySet() {
            return receivers.entrySet();
        }
    }

    public class ProtoContextFactory extends MapVariableResolverFactory {

        private final SimpleIndexHashMapWrapper<String, VariableResolver> variableResolvers;

        public ProtoContextFactory(SimpleIndexHashMapWrapper variables) {
            super(variables);
            variableResolvers = new SimpleIndexHashMapWrapper<String, VariableResolver>(variables, true);
        }

        @Override
        public VariableResolver createVariable(String name, Object value) {
            VariableResolver vr;

            try {
                (vr = getVariableResolver(name)).setValue(value);
                return vr;
            } catch (UnresolveablePropertyException e) {
                addResolver(name, vr = new ProtoResolver(variables, name)).setValue(value);
                return vr;
            }
        }

        @Override
        public VariableResolver createVariable(String name, Object value, Class<?> type) {
            VariableResolver vr;
            try {
                vr = getVariableResolver(name);
            } catch (UnresolveablePropertyException e) {
                vr = null;
            }

            if (vr != null && vr.getType() != null) {
                throw new CompileException("variable already defined within scope: " + vr.getType() + " " + name, expr, start);
            } else {
                addResolver(name, vr = new ProtoResolver(variables, name, type)).setValue(value);
                return vr;
            }
        }

        @Override
        public String[] getIndexedVariableNames() {
            //
            return null;
        }

        @Override
        public void setIndexedVariableNames(String[] indexedVariableNames) {
            //
        }

        @Override
        public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> type) {
            VariableResolver vr = this.variableResolvers != null ? this.variableResolvers.getByIndex(index) : null;
            if (vr != null && vr.getType() != null) {
                throw new CompileException("variable already defined within scope: " + vr.getType() + " " + name, expr, start);
            } else {
                return createIndexedVariable(variableIndexOf(name), name, value);
            }
        }

        @Override
        public VariableResolver createIndexedVariable(int index, String name, Object value) {
            VariableResolver vr = variableResolvers.getByIndex(index);

            if (vr == null) {
                vr = new SimpleValueResolver(value);
                variableResolvers.putAtIndex(index, vr);
            } else {
                vr.setValue(value);
            }

            return indexedVariableResolvers[index];
        }

        @Override
        public VariableResolver getIndexedVariableResolver(int index) {
            return variableResolvers.getByIndex(index);
        }

        @Override
        public VariableResolver setIndexedVariableResolver(int index, VariableResolver resolver) {
            variableResolvers.putAtIndex(index, resolver);
            return resolver;
        }

        @Override
        public int variableIndexOf(String name) {
            return variableResolvers.indexOf(name);
        }

        public VariableResolver getVariableResolver(String name) {
            VariableResolver vr = variableResolvers.get(name);
            if (vr != null) {
                return vr;
            } else if (variables.containsKey(name)) {
                variableResolvers.put(name, vr = new ProtoResolver(variables, name));
                return vr;
            } else if (nextFactory != null) {
                return nextFactory.getVariableResolver(name);
            }

            throw new UnresolveablePropertyException("unable to resolve variable '" + name + "'");
        }
    }

    public class ProtoResolver implements VariableResolver {

        private String name;
        private Class<?> knownType;
        private Map<String, Object> variableMap;

        public ProtoResolver(Map<String, Object> variableMap, String name) {
            this.variableMap = variableMap;
            this.name = name;
        }

        public ProtoResolver(Map<String, Object> variableMap, String name, Class knownType) {
            this.name = name;
            this.knownType = knownType;
            this.variableMap = variableMap;
        }

        public void setStaticType(Class knownType) {
            this.knownType = knownType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Class getType() {
            return knownType;
        }

        public Object getValue() {
            return ((Receiver) variableMap.get(name)).receiver;
        }

        public void setValue(Object value) {
            if (knownType != null && value != null && value.getClass() != knownType) {
                if (!canConvert(knownType, value.getClass())) {
                    throw new CompileException("cannot assign " + value.getClass().getName() + " to type: " + knownType.getName(), expr,
                            start);
                }
                try {
                    value = convert(value, knownType);
                } catch (Exception e) {
                    throw new CompileException("cannot convert value of " + value.getClass().getName() + " to: " + knownType.getName(),
                            expr, start);
                }
            }

            ((Receiver) variableMap.get(name)).receiver = value;
        }

        public int getFlags() {
            return 0;
        }
    }
}
