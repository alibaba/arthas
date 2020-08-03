package com.taobao.arthas.bytekit.asm;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.commons.Method;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.FrameNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.IincInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.IntInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.JumpInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LabelNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LdcInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LocalVariableNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.TryCatchBlockNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.TypeInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.VarInsnNode;
import com.taobao.arthas.bytekit.asm.location.filter.DefaultLocationFilter;
import com.taobao.arthas.bytekit.asm.location.filter.LocationFilter;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;

public class MethodProcessor {

    private String owner;
    /**
     * maybe null
     */
    private ClassNode classNode;
    private MethodNode methodNode;

    private final Type[] argumentTypes;
    private final Type returnType;

    private int nextLocals;

    private static final Type BYTE_TYPE = Type.getObjectType("java/lang/Byte");

    private static final Type BOOLEAN_TYPE = Type.getObjectType("java/lang/Boolean");

    private static final Type SHORT_TYPE = Type.getObjectType("java/lang/Short");

    private static final Type CHARACTER_TYPE = Type.getObjectType("java/lang/Character");

    private static final Type INTEGER_TYPE = Type.getObjectType("java/lang/Integer");

    private static final Type FLOAT_TYPE = Type.getObjectType("java/lang/Float");

    private static final Type LONG_TYPE = Type.getObjectType("java/lang/Long");

    private static final Type DOUBLE_TYPE = Type.getObjectType("java/lang/Double");

    private static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");

    private static final Type STRING_TYPE = Type.getObjectType("java/lang/String");

    private static final Type THROWABLE_TYPE = Type.getObjectType("java/lang/Throwable");

    private static final Type NUMBER_TYPE = Type.getObjectType("java/lang/Number");

    private static final Type OBJECT_ARRAY_TYPE = Type.getType(Object[].class);

    private static final Method BOOLEAN_VALUE = Method.getMethod("boolean booleanValue()");

    private static final Method CHAR_VALUE = Method.getMethod("char charValue()");

    private static final Method INT_VALUE = Method.getMethod("int intValue()");

    private static final Method FLOAT_VALUE = Method.getMethod("float floatValue()");

    private static final Method LONG_VALUE = Method.getMethod("long longValue()");

    private static final Method DOUBLE_VALUE = Method.getMethod("double doubleValue()");

    public static final String DEFAULT_INNER_VARIABLE_PREFIX = "_$bytekit$_";

    private final LabelNode interceptorVariableStartLabelNode = new LabelNode();
    private final LabelNode interceptorVariableEndLabelNode = new LabelNode();

    private AbstractInsnNode enterInsnNode;
    // TODO 这里应该直接从 InsnList 里来取？因为插入代码之后，这个会改变的。
    // TODO 这个没有被使用到，是不是没用的？？
    private AbstractInsnNode lastInsnNode;

    /**
     * 保留中间生成的 variable的名字
     */
    private boolean keepLocalVariableNames;

    private String innerVariablePrefix;

    private String returnVariableName;
    private String throwVariableName;
    private String invokeArgsVariableName;
    private String monitorVariableName;
    private LocalVariableNode returnVariableNode = null;
    private LocalVariableNode throwVariableNode = null;
    private LocalVariableNode invokeArgsVariableNode = null;
    private LocalVariableNode monitorVariableNode = null;  // for synchronized

    private String invokeReturnVariablePrefix;
    private Map<String, LocalVariableNode> invokeReturnVariableNodeMap = new HashMap<String, LocalVariableNode>();

    private TryCatchBlock tryCatchBlock = null;
    
    private LocationFilter locationFilter = new DefaultLocationFilter();

    public MethodProcessor(final ClassNode classNode, final MethodNode methodNode) {
        this(classNode, methodNode, false);
    }
    
    public MethodProcessor(final ClassNode classNode, final MethodNode methodNode, LocationFilter locationFilter) {
        this(classNode, methodNode, false);
        this.locationFilter = locationFilter;
    }

    public MethodProcessor(final ClassNode classNode, final MethodNode methodNode, boolean keepLocalVariableNames) {
        this(classNode.name, methodNode, keepLocalVariableNames);
        this.classNode = classNode;
    }

    public MethodProcessor(final String owner, final MethodNode methodNode, boolean keepLocalVariableNames) {
        this.owner = owner;
        this.methodNode = methodNode;
        this.nextLocals = methodNode.maxLocals;
        this.argumentTypes = Type.getArgumentTypes(methodNode.desc);
        this.returnType = Type.getReturnType(methodNode.desc);
        this.keepLocalVariableNames = keepLocalVariableNames;

        // find enter & exit instruction.
        if (isConstructor()) {
            this.enterInsnNode = findInitConstructorInstruction();
        } else {
            this.enterInsnNode = methodNode.instructions.getFirst();
        }

        // when the method is empty, both enterInsnNode and lastInsnNode are Opcodes.RETURN ;
        this.lastInsnNode = methodNode.instructions.getLast();

        // setup interceptor variables start/end label.
        this.methodNode.instructions.insertBefore(this.enterInsnNode, this.interceptorVariableStartLabelNode);
        this.methodNode.instructions.insert(this.lastInsnNode, this.interceptorVariableEndLabelNode);

        initInnerVariablePrefix();
    }
    public MethodProcessor(final String owner, final MethodNode methodNode) {
        this(owner, methodNode, false);
    }

    private void initInnerVariablePrefix() {
        String prefix = DEFAULT_INNER_VARIABLE_PREFIX;
        int count = 0;
        while(existLocalVariableWithPrefix(prefix)) {
            prefix = DEFAULT_INNER_VARIABLE_PREFIX + count + "_";
            count++;
        }
        this.innerVariablePrefix = prefix;

        returnVariableName = innerVariablePrefix + "_return";
        throwVariableName = innerVariablePrefix + "_throw";
        invokeArgsVariableName = innerVariablePrefix + "_invokeArgs";
        monitorVariableName = innerVariablePrefix + "_monitor";

        invokeReturnVariablePrefix = innerVariablePrefix + "_invokeReturn_";
    }

    private boolean existLocalVariableWithPrefix(String prefix) {
        for (LocalVariableNode variableNode : this.methodNode.localVariables) {
            if (variableNode.name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public LocalVariableNode initMonitorVariableNode() {
        if (monitorVariableNode == null) {
            monitorVariableNode = this.addInterceptorLocalVariable(monitorVariableName, OBJECT_TYPE.getDescriptor());
        }
        return monitorVariableNode;
    }

    public LocalVariableNode initThrowVariableNode() {
        if (throwVariableNode == null) {
            throwVariableNode = this.addInterceptorLocalVariable(throwVariableName, THROWABLE_TYPE.getDescriptor());
        }
        return throwVariableNode;
    }

    public LocalVariableNode initInvokeArgsVariableNode() {
        if (invokeArgsVariableNode == null) {
            invokeArgsVariableNode = this.addInterceptorLocalVariable(invokeArgsVariableName,
                    OBJECT_ARRAY_TYPE.getDescriptor());
        }
        return invokeArgsVariableNode;
    }

    public LocalVariableNode initReturnVariableNode() {
        if (returnVariableNode == null) {
            returnVariableNode = this.addInterceptorLocalVariable(returnVariableName, returnType.getDescriptor());
        }
        return returnVariableNode;
    }

    /**
     *
     * @param name
     * @param type
     * @return
     */
    public LocalVariableNode initInvokeReturnVariableNode(String name, Type type) {
        String key = this.invokeReturnVariablePrefix + name;
        LocalVariableNode variableNode = invokeReturnVariableNodeMap.get(key);
        if (variableNode == null) {
            variableNode = this.addInterceptorLocalVariable(key, type.getDescriptor());
            invokeReturnVariableNodeMap.put(key, variableNode);
        }
        return variableNode;
    }

    public TryCatchBlock initTryCatchBlock() {
        return initTryCatchBlock(THROWABLE_TYPE.getInternalName());
    }

    public TryCatchBlock initTryCatchBlock(String exception) {
        if( this.tryCatchBlock == null) {
            this.tryCatchBlock = new TryCatchBlock(methodNode, exception);
            this.methodNode.instructions.insertBefore(this.getEnterInsnNode(), tryCatchBlock.getStartLabelNode());
            this.methodNode.instructions.insert(this.getLastInsnNode(), tryCatchBlock.getEndLabelNode());
            InsnList instructions = new InsnList();
            AsmOpUtils.throwException(instructions);
            this.methodNode.instructions.insert(tryCatchBlock.getEndLabelNode(), instructions);

            tryCatchBlock.sort();
        }
        return tryCatchBlock;
    }

    AbstractInsnNode findInitConstructorInstruction() {
        int nested = 0;
        for (AbstractInsnNode insnNode = this.methodNode.instructions.getFirst(); insnNode != null; insnNode = insnNode
                .getNext()) {
            if (insnNode instanceof TypeInsnNode) {
                if (insnNode.getOpcode() == Opcodes.NEW) {
                    // new object().
                    nested++;
                }
            } else if (insnNode instanceof MethodInsnNode) {
                final MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                if (methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL && methodInsnNode.name.equals("<init>")) {
                    if (--nested < 0) {
                        // find this() or super().
                        return insnNode.getNext();
                    }
                }
            }
        }

        return null;
    }

    public AbstractInsnNode getEnterInsnNode() {
        return enterInsnNode;
    }

    public AbstractInsnNode getLastInsnNode() {
        return lastInsnNode;
    }

    public String[] getParameterTypes() {
        final String[] parameterTypes = new String[this.argumentTypes.length];
        for (int i = 0; i < this.argumentTypes.length; i++) {
            parameterTypes[i] = this.argumentTypes[i].getClassName();
        }

        return parameterTypes;
    }

    public String[] getParameterNames() {
        if (this.argumentTypes.length == 0) {
            return new String[0];
        }

        final List<LocalVariableNode> localVariableNodes = this.methodNode.localVariables;
        int localVariableStartIndex = 1;
        if (isStatic()) {
            // static method is none this.
            localVariableStartIndex = 0;
        }

        if (localVariableNodes == null || localVariableNodes.size() <= localVariableStartIndex
                || (this.argumentTypes.length + localVariableStartIndex) > localVariableNodes.size()) {
            // make simple argument names.
            final String[] names = new String[this.argumentTypes.length];
            for (int i = 0; i < this.argumentTypes.length; i++) {
                final String className = this.argumentTypes[i].getClassName();
                if (className != null) {
                    final int findIndex = className.lastIndexOf('.');
                    if (findIndex == -1) {
                        names[i] = className;
                    } else {
                        names[i] = className.substring(findIndex + 1);
                    }
                } else {
                    names[i] = this.argumentTypes[i].getDescriptor();
                }
            }
            return names;
        }

        // sort by index.
        Collections.sort(localVariableNodes, new Comparator<LocalVariableNode>() {

            @Override
            public int compare(LocalVariableNode o1, LocalVariableNode o2) {
                return o1.index - o2.index;
            }
        });
        String[] names = new String[this.argumentTypes.length];

        for (int i = 0; i < this.argumentTypes.length; i++) {
            final String name = localVariableNodes.get(localVariableStartIndex++).name;
            if (name != null) {
                names[i] = name;
            } else {
                names[i] = "";
            }
        }

        return names;
    }

    public Type getReturnType() {
        return this.returnType;
    }

    private boolean hasLocalVariable(String name) {
        List<LocalVariableNode> localVariableNodes = this.methodNode.localVariables;
        if (localVariableNodes == null) {
            return false;
        }

        for (LocalVariableNode node : localVariableNodes) {
            if (node.name.equals(name)) {
                return true;
            }
        }

        return false;
    }

    public void loadThis(final InsnList instructions) {
        if (isConstructor()) {
            // load this.
            loadVar(instructions, 0);
        } else {
            if (isStatic()) {
                // load null.
                loadNull(instructions);
            } else {
                // load this.
                loadVar(instructions, 0);
            }
        }
    }

    void storeVar(final InsnList instructions, final int index) {
        instructions.add(new VarInsnNode(Opcodes.ASTORE, index));
    }

    void storeInt(final InsnList instructions, final int index) {
        instructions.add(new VarInsnNode(Opcodes.ISTORE, index));
    }

    void loadNull(final InsnList instructions) {
        instructions.add(new InsnNode(Opcodes.ACONST_NULL));
    }

    void loadVar(final InsnList instructions, final int index) {
        instructions.add(new VarInsnNode(Opcodes.ALOAD, index));
    }

    void loadInt(final InsnList instructions, final int index) {
        instructions.add(new VarInsnNode(Opcodes.ILOAD, index));
    }

    boolean isReturnCode(final int opcode) {
        return opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN
                || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN;
    }

    Type getBoxedType(final Type type) {
        switch (type.getSort()) {
        case Type.BYTE:
            return BYTE_TYPE;
        case Type.BOOLEAN:
            return BOOLEAN_TYPE;
        case Type.SHORT:
            return SHORT_TYPE;
        case Type.CHAR:
            return CHARACTER_TYPE;
        case Type.INT:
            return INTEGER_TYPE;
        case Type.FLOAT:
            return FLOAT_TYPE;
        case Type.LONG:
            return LONG_TYPE;
        case Type.DOUBLE:
            return DOUBLE_TYPE;
        }
        return type;
    }

    void push(InsnList insnList, final int value) {
        if (value >= -1 && value <= 5) {
            insnList.add(new InsnNode(Opcodes.ICONST_0 + value));
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            insnList.add(new IntInsnNode(Opcodes.BIPUSH, value));
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            insnList.add(new IntInsnNode(Opcodes.SIPUSH, value));
        } else {
            insnList.add(new LdcInsnNode(value));
        }
    }

    void push(InsnList insnList, final String value) {
        if (value == null) {
            insnList.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            insnList.add(new LdcInsnNode(value));
        }
    }

    void newArray(final InsnList insnList, final Type type) {
        insnList.add(new TypeInsnNode(Opcodes.ANEWARRAY, type.getInternalName()));
    }

    void dup(final InsnList insnList) {
        insnList.add(new InsnNode(Opcodes.DUP));
    }

    void dup2(final InsnList insnList) {
        insnList.add(new InsnNode(Opcodes.DUP2));
    }

    void dupX1(final InsnList insnList) {
        insnList.add(new InsnNode(Opcodes.DUP_X1));
    }

    void dupX2(final InsnList insnList) {
        insnList.add(new InsnNode(Opcodes.DUP_X2));
    }

    void pop(final InsnList insnList) {
        insnList.add(new InsnNode(Opcodes.POP));
    }

    void swap(final InsnList insnList) {
        insnList.add(new InsnNode(Opcodes.SWAP));
    }

    void loadArgsVar(final InsnList instructions) {
        if (this.argumentTypes.length == 0) {
            // null.
            loadNull(instructions);
            return;
        }

        push(instructions, this.argumentTypes.length);
        // new array
        newArray(instructions, OBJECT_TYPE);
        for (int i = 0; i < this.argumentTypes.length; i++) {
            Type type = this.argumentTypes[i];
            dup(instructions);
            push(instructions, i);
            // loadArg
            loadArg(instructions, this.argumentTypes, i);
            // box
            box(instructions, type);
            // arrayStore
            arrayStore(instructions, OBJECT_TYPE);
        }
    }

    void loadArgs(final InsnList instructions) {
        for (int i = 0; i < this.argumentTypes.length; i++) {
            loadArg(instructions, this.argumentTypes, i);
        }
    }

    void loadArg(final InsnList instructions, Type[] argumentTypes, int i) {
        final int index = getArgIndex(argumentTypes, i);
        final Type type = argumentTypes[i];
        instructions.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), index));
    }

    int getArgIndex(final Type[] argumentTypes, final int arg) {
        int index = isStatic() ? 0 : 1;
        for (int i = 0; i < arg; i++) {
            index += argumentTypes[i].getSize();
        }
        return index;
    }

    void box(final InsnList instructions, Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return;
        }

        if (type == Type.VOID_TYPE) {
            // push null
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            Type boxed = getBoxedType(type);
            // new instance.
            newInstance(instructions, boxed);
            if (type.getSize() == 2) {
                // Pp -> Ppo -> oPpo -> ooPpo -> ooPp -> o
                // dupX2
                dupX2(instructions);
                // dupX2
                dupX2(instructions);
                // pop
                pop(instructions);
            } else {
                // p -> po -> opo -> oop -> o
                // dupX1
                dupX1(instructions);
                // swap
                swap(instructions);
            }
            invokeConstructor(instructions, boxed, new Method("<init>", Type.VOID_TYPE, new Type[] { type }));
        }
    }

    void unbox(final InsnList instructions, Type type) {
        Type t = NUMBER_TYPE;
        Method sig = null;
        switch (type.getSort()) {
        case Type.VOID:
            return;
        case Type.CHAR:
            t = CHARACTER_TYPE;
            sig = CHAR_VALUE;
            break;
        case Type.BOOLEAN:
            t = BOOLEAN_TYPE;
            sig = BOOLEAN_VALUE;
            break;
        case Type.DOUBLE:
            sig = DOUBLE_VALUE;
            break;
        case Type.FLOAT:
            sig = FLOAT_VALUE;
            break;
        case Type.LONG:
            sig = LONG_VALUE;
            break;
        case Type.INT:
        case Type.SHORT:
        case Type.BYTE:
            sig = INT_VALUE;
        }
        if (sig == null) {
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, type.getInternalName()));
        } else {
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, t.getInternalName()));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, t.getInternalName(), sig.getName(),
                    sig.getDescriptor(), false));
        }
    }

    void arrayStore(final InsnList instructions, final Type type) {
        instructions.add(new InsnNode(type.getOpcode(Opcodes.IASTORE)));
    }

    void arrayLoad(final InsnList instructions, final Type type) {
        instructions.add(new InsnNode(type.getOpcode(Opcodes.IALOAD)));
    }

    void newInstance(final InsnList instructions, final Type type) {
        instructions.add(new TypeInsnNode(Opcodes.NEW, type.getInternalName()));
    }

    void invokeConstructor(final InsnList instructions, final Type type, final Method method) {
        String owner = type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
        instructions
                .add(new MethodInsnNode(Opcodes.INVOKESPECIAL, owner, method.getName(), method.getDescriptor(), false));
    }

    LocalVariableNode addInterceptorLocalVariable(final String name, final String desc) {
        return addLocalVariable(name, desc, this.interceptorVariableStartLabelNode,
                this.interceptorVariableEndLabelNode);
    }

    LocalVariableNode addLocalVariable(final String name, final String desc, final LabelNode start,
            final LabelNode end) {
        Type type = Type.getType(desc);
        int index = this.nextLocals;
        this.nextLocals += type.getSize();
        methodNode.maxLocals = this.nextLocals;
        final LocalVariableNode node = new LocalVariableNode(name, desc, null, start, end, index);
        if (keepLocalVariableNames) {
            this.methodNode.localVariables.add(node);
        }

        return node;
    }

    public void returnValue(final InsnList instructions) {
        instructions.add(new InsnNode(this.returnType.getOpcode(Opcodes.IRETURN)));
    }

    public boolean isStatic() {
        return (this.methodNode.access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isConstructor() {
        return this.methodNode.name != null && this.methodNode.name.equals("<init>");
    }

    public MethodNode getMethodNode() {
        return methodNode;
    }

    public void setMethodNode(MethodNode methodNode) {
        this.methodNode = methodNode;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ClassNode getClassNode() {
        return classNode;
    }
    public void setClassNode(ClassNode classNode) {
        this.classNode = classNode;
    }
    public LocationFilter getLocationFilter() {
        return locationFilter;
    }

    /**
     * TODO 可以考虑实现修改值的功能，原理是传入的 args实际转化为一个stack上的slot，只要在inline之后，把 stack上面的对应的slot保存到想要保存的位置就可以了。
     * @param owner
     * @param tmpToInlineMethodNode
     */
    public void inline(String owner, MethodNode toInlineMethodNode) {

        ListIterator<AbstractInsnNode> originMethodIter = this.methodNode.instructions.iterator();

        while(originMethodIter.hasNext()) {
            AbstractInsnNode originMethodInsnNode = originMethodIter.next();

            if (originMethodInsnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) originMethodInsnNode;
                if (methodInsnNode.owner.equals(owner) && methodInsnNode.name.equals(toInlineMethodNode.name)
                        && methodInsnNode.desc.equals(toInlineMethodNode.desc)) {
                    // 要copy一份，否则inline多次会出问题
                    MethodNode tmpToInlineMethodNode = AsmUtils.copy(toInlineMethodNode);
                    tmpToInlineMethodNode = AsmUtils.removeLineNumbers(tmpToInlineMethodNode);

                    LabelNode end = new LabelNode();
                    this.methodNode.instructions.insert(methodInsnNode, end);

                    InsnList instructions = new InsnList();

                    // 要先记录好当前的 maxLocals ，然后再依次把 栈上的 args保存起来 ，后面调整 VarInsnNode index里，要加上当前的 maxLocals
                    // save args to local vars
                    int currentMaxLocals = this.nextLocals;

                    int off = (tmpToInlineMethodNode.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
                    Type[] args = Type.getArgumentTypes(tmpToInlineMethodNode.desc);
                    int argsOff = off;

                    for(int i = 0; i < args.length; ++i) {
                        argsOff += args[i].getSize();
                    }
                    // 记录新的 maxLocals
                    this.nextLocals += argsOff;
                    methodNode.maxLocals = this.nextLocals;


                    for(int i = args.length - 1; i >= 0; --i) {
                        argsOff -= args[i].getSize();
//                        this.visitVarInsn(args[i].getOpcode(Opcodes.ISTORE), argsOff);

                        AsmOpUtils.storeVar(instructions, args[i], currentMaxLocals + argsOff);
                    }

                    // this
                    if (off > 0) {
//                        this.visitVarInsn(Opcodes.ASTORE, 0);
                        AsmOpUtils.storeVar(instructions, OBJECT_TYPE, currentMaxLocals);
                    }


                    ListIterator<AbstractInsnNode> inlineIterator = tmpToInlineMethodNode.instructions.iterator();
                    while(inlineIterator.hasNext()) {
                        AbstractInsnNode abstractInsnNode = inlineIterator.next();
                        if(abstractInsnNode instanceof FrameNode) {
                            continue;
                        }

                        //修改inline代码中的使用到局部变量的指令的var操作数(变量slot)
                        if(abstractInsnNode instanceof  VarInsnNode) {
                            VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                            varInsnNode.var += currentMaxLocals;
                        } else if (abstractInsnNode instanceof IincInsnNode) {
                            IincInsnNode iincInsnNode = (IincInsnNode) abstractInsnNode;
                            iincInsnNode.var += currentMaxLocals;
                        }

                        int opcode = abstractInsnNode.getOpcode();
                        if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
//                            super.visitJumpInsn(Opcodes.GOTO, end);
//                            instructions.add(new JumpInsnNode(Opcodes.GOTO, end));
                            inlineIterator.remove();
                            instructions.add(new JumpInsnNode(Opcodes.GOTO, end));
                            continue;
                        }
                        inlineIterator.remove();
                        instructions.add(abstractInsnNode);
                    }


                    // 插入inline之后的代码，再删除掉原来的 MethodInsnNode
                    this.methodNode.instructions.insertBefore(methodInsnNode, instructions);
                    originMethodIter.remove();
                    // try catch 块加上，然后排序
                    if(this.methodNode.tryCatchBlocks != null && tmpToInlineMethodNode.tryCatchBlocks != null) {
                        this.methodNode.tryCatchBlocks.addAll(tmpToInlineMethodNode.tryCatchBlocks);
                    }
                    this.sortTryCatchBlock();
                }
            }
        }

    }

    public void sortTryCatchBlock() {
        if (this.methodNode.tryCatchBlocks == null) {
            return;
        }

        // Compares TryCatchBlockNodes by the length of their "try" block.
        Collections.sort(this.methodNode.tryCatchBlocks, new Comparator<TryCatchBlockNode>() {
            @Override
            public int compare(TryCatchBlockNode t1, TryCatchBlockNode t2) {
                int len1 = blockLength(t1);
                int len2 = blockLength(t2);
                return len1 - len2;
            }

            private int blockLength(TryCatchBlockNode block) {
                final int startidx = methodNode.instructions.indexOf(block.start);
                final int endidx = methodNode.instructions.indexOf(block.end);
                return endidx - startidx;
            }
        });

        // Updates the 'target' of each try catch block annotation.
        for (int i = 0; i < this.methodNode.tryCatchBlocks.size(); i++) {
            this.methodNode.tryCatchBlocks.get(i).updateIndex(i);
        }
    }

}
