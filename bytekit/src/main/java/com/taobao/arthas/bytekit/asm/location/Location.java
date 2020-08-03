package com.taobao.arthas.bytekit.asm.location;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.FieldInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LocalVariableNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.binding.BindingContext;
import com.taobao.arthas.bytekit.asm.binding.StackSaver;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;

/**
 * Specifies a location in a method at which a rule trigger should be inserted
 */
public abstract class Location {

    AbstractInsnNode insnNode;

    boolean whenComplete = false;
    
    boolean stackNeedSave = false;

    public Location(AbstractInsnNode insnNode) {
        this(insnNode, false);
    }

    public Location(AbstractInsnNode insnNode, boolean whenComplete) {
        this.insnNode = insnNode;
        this.whenComplete = whenComplete;
    }

    public boolean isWhenComplete() {
        return whenComplete;
    }

    public AbstractInsnNode getInsnNode() {
        return insnNode;
    }

    /**
     * 标记在这个location，栈上原来的值可以被 callback 函数的return值替换掉
     * 
     * @return
     */
    public boolean canChangeByReturn() {
        return false;
    }
    
    public boolean isStackNeedSave() {
        return stackNeedSave;
    }

    public StackSaver getStackSaver() {
        throw new UnsupportedOperationException("this location do not StackSaver, type:" + getLocationType());
    }

    /**
     * identify the type of this location
     * 
     * @return the type of this location
     */
    public abstract LocationType getLocationType();

    /**
     * flag indicating that a field access location refers to field READ operations
     */
    public static final int ACCESS_READ = 1;

    /**
     * flag indicating that a field access location refers to field WRITE operations
     */
    public static final int ACCESS_WRITE = 2;

    /**
     * location identifying a method enter trigger point
     */
    static class EnterLocation extends Location {
        public EnterLocation(AbstractInsnNode enterInsnNode) {
            super(enterInsnNode);
            this.insnNode = enterInsnNode;
        }

        public LocationType getLocationType() {
            return LocationType.ENTER;
        }

    }

    /**
     * location identifying a method line trigger point
     */
    public static class LineLocation extends Location {
        /**
         * the line at which the trigger point should be inserted
         */
        private int targetLine;

        public LineLocation(AbstractInsnNode insnNode, int targetLine) {
            super(insnNode);
            this.targetLine = targetLine;
        }

        public LocationType getLocationType() {
            return LocationType.LINE;
        }

    }

    /**
     * location identifying a generic access trigger point
     */
    private static abstract class AccessLocation extends Location {
        /**
         * count identifying which access should be taken as the trigger point. if not
         * specified as a parameter this defaults to the first access.
         */
        protected int count;

        /**
         * flags identifying which type of access should be used to identify the
         * trigger. this is either ACCESS_READ, ACCESS_WRITE or an OR of these two
         * values
         */
        protected int flags;

        protected AccessLocation(AbstractInsnNode insnNode, int count, int flags, boolean whenComplete) {
            super(insnNode, whenComplete);
            this.count = count;
            this.flags = flags;
        }

        public LocationType getLocationType() {
            if ((flags & ACCESS_WRITE) != 0) {
                if (whenComplete) {
                    return LocationType.WRITE_COMPLETED;
                } else {
                    return LocationType.WRITE;
                }
            } else {
                if (whenComplete) {
                    return LocationType.READ_COMPLETED;
                } else {
                    return LocationType.READ;
                }
            }
        }
    }

    /**
     * location identifying a field access trigger point
     */
    public static class FieldAccessLocation extends AccessLocation {

        public FieldAccessLocation(FieldInsnNode fieldInsnNode, int count, int flags, boolean whenComplete) {
            super(fieldInsnNode, count, flags, whenComplete);
        }

    }

    /**
     * location identifying a variable access trigger point
     */
    private static class VariableAccessLocation extends AccessLocation {
        /**
         * the name of the variable being accessed at the point where the trigger point
         * should be inserted
         */
        private String variableName;

        /**
         * flag which is true if the name is a method parameter index such as $0, $1 etc
         * otherwise false
         */
        private boolean isIndex;

        protected VariableAccessLocation(AbstractInsnNode insnNode, String variablename, int count, int flags,
                boolean whenComplete) {
            super(insnNode, count, flags, whenComplete);
            this.variableName = variablename;
            isIndex = variablename.matches("[0-9]+");
        }

        public LocationType getLocationType() {
            if ((flags & ACCESS_WRITE) != 0) {
                if (whenComplete) {
                    return LocationType.WRITE_COMPLETED;
                } else {
                    return LocationType.WRITE;
                }
            } else {
                if (whenComplete) {
                    return LocationType.READ_COMPLETED;
                } else {
                    return LocationType.READ;
                }
            }
        }

    }

    /**
     * location identifying a method invocation trigger point
     */
    public static class InvokeLocation extends Location implements MethodInsnNodeWare {

        /**
         * count identifying which invocation should be taken as the trigger point. if
         * not specified as a parameter this defaults to the first invocation.
         */
        private int count;

        public InvokeLocation(MethodInsnNode insnNode, int count, boolean whenComplete) {
            super(insnNode, whenComplete);
            this.count = count;
            this.stackNeedSave = false;
        }

        @Override
        public boolean canChangeByReturn() {
            // 对于 invoke ，只有在 complete 时，才能有返回值
            return whenComplete;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public LocationType getLocationType() {
            if (whenComplete) {
                return LocationType.INVOKE_COMPLETED;
            } else {
                return LocationType.INVOKE;
            }
        }
        
        @Override
        public StackSaver getStackSaver() {
            StackSaver stackSaver = null;
            if(whenComplete) {
                stackSaver = new StackSaver() {

                    @Override
                    public void store(InsnList instructions, BindingContext bindingContext) {
                        AbstractInsnNode insnNode = bindingContext.getLocation().getInsnNode();
                        MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
                        if (insnNode instanceof MethodInsnNode) {
                            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                            String uniqueNameForMethod = AsmUtils.uniqueNameForMethod(methodInsnNode.owner, methodInsnNode.name,
                                    methodInsnNode.desc);
                            Type invokeReturnType = Type.getMethodType(methodInsnNode.desc).getReturnType();
                            
                            if(!invokeReturnType.equals(Type.VOID_TYPE)) {
                                LocalVariableNode invokeReturnVariableNode = methodProcessor.initInvokeReturnVariableNode(
                                        uniqueNameForMethod, invokeReturnType);
                                AsmOpUtils.storeVar(instructions, invokeReturnType, invokeReturnVariableNode.index);
                            }
                        } else {
                            throw new IllegalArgumentException(
                                    "InvokeReturnBinding location is not MethodInsnNode, insnNode: " + insnNode);
                        }
                        
                    }

                    @Override
                    public void load(InsnList instructions, BindingContext bindingContext) {
                        AbstractInsnNode insnNode = bindingContext.getLocation().getInsnNode();
                        MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
                        if (insnNode instanceof MethodInsnNode) {
                            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                            String uniqueNameForMethod = AsmUtils.uniqueNameForMethod(methodInsnNode.owner, methodInsnNode.name,
                                    methodInsnNode.desc);
                            Type invokeReturnType = Type.getMethodType(methodInsnNode.desc).getReturnType();
                            
                            if(!invokeReturnType.equals(Type.VOID_TYPE)) {
                                LocalVariableNode invokeReturnVariableNode = methodProcessor.initInvokeReturnVariableNode(
                                        uniqueNameForMethod, invokeReturnType);
                                AsmOpUtils.loadVar(instructions, invokeReturnType, invokeReturnVariableNode.index);
                            }
                        } else {
                            throw new IllegalArgumentException(
                                    "InvokeReturnBinding location is not MethodInsnNode, insnNode: " + insnNode);
                        }
                    }

                    @Override
                    public Type getType(BindingContext bindingContext) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        return Type.getMethodType(methodInsnNode.desc).getReturnType();
                    }
                    
                };
            }else {
                stackSaver = new StackSaver() {

                    @Override
                    public void store(InsnList instructions, BindingContext bindingContext) {
                        // 需要从要调用的 函数的 des ，找到参数的类型，再从栈上一个个吐出来，再保存到数组里
                        Location location = bindingContext.getLocation();
                        MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
                        if(location instanceof InvokeLocation) {
                            InvokeLocation invokeLocation = (InvokeLocation) location;
                            // 如果是非 static的，会有this指针 
                            MethodInsnNode methodInsnNode = (MethodInsnNode)invokeLocation.getInsnNode();
                            Type methodType = Type.getMethodType(methodInsnNode.desc);
                            boolean isStatic = AsmUtils.isStatic(methodInsnNode);
                            Type[] argumentTypes = methodType.getArgumentTypes();
                            
//                            // 如果是非static，则存放到数组的index要多 1
//                            AsmOpUtils.push(instructions, argumentTypes.length + (isStatic ? 0 : 1));
//                            AsmOpUtils.newArray(instructions, AsmOpUtils.OBJECT_TYPE);
//                            LocalVariableNode invokeArgsVariableNode = methodProcessor.initInvokeArgsVariableNode();
//                            AsmOpUtils.storeVar(instructions, AsmOpUtils.OBJECT_ARRAY_TYPE, invokeArgsVariableNode.index);

                            // 从invoke的参数的后面，一个个存到数组里
//                            for(int i = argumentTypes.length - 1; i >= 0 ; --i) {
//                                AsmOpUtils.loadVar(instructions, AsmOpUtils.OBJECT_ARRAY_TYPE, invokeArgsVariableNode.index);
//
//                                AsmOpUtils.swap(instructions, argumentTypes[i], AsmOpUtils.OBJECT_ARRAY_TYPE);
//                                // 如果是非static，则存放到数组的index要多 1
//                                AsmOpUtils.push(instructions, i + (isStatic ? 0 : 1));
//                                AsmOpUtils.swap(instructions, argumentTypes[i], Type.INT_TYPE);
//                                
//                                AsmOpUtils.box(instructions, argumentTypes[i]);
//                                AsmOpUtils.arrayStore(instructions, AsmOpUtils.OBJECT_TYPE);
//                                
//                            }
                            // 处理this
//                            if(!isStatic) {
//                                AsmOpUtils.loadVar(instructions, AsmOpUtils.OBJECT_ARRAY_TYPE, invokeArgsVariableNode.index);
//
//                                AsmOpUtils.swap(instructions, AsmOpUtils.OBJECT_TYPE, AsmOpUtils.OBJECT_ARRAY_TYPE);
//                                AsmOpUtils.push(instructions, 0);
//                                AsmOpUtils.swap(instructions, AsmOpUtils.OBJECT_TYPE, Type.INT_TYPE);
//                                AsmOpUtils.arrayStore(instructions, AsmOpUtils.OBJECT_TYPE);
//                            }
                            
                        }else {
                            throw new IllegalArgumentException("location is not a InvokeLocation, location: " + location);
                        }
                        
                    }

                    @Override
                    public void load(InsnList instructions, BindingContext bindingContext) {
                        // 从数组里取出来，一个个再放到栈上，要检查是否要unbox
                        Location location = bindingContext.getLocation();
                        MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
                        LocalVariableNode invokeArgsVariableNode = methodProcessor.initInvokeArgsVariableNode();
                        
                        if(location instanceof InvokeLocation) {
                            InvokeLocation invokeLocation = (InvokeLocation) location;
                            // 如果是非 static的，会有this指针 
                            MethodInsnNode methodInsnNode = (MethodInsnNode)invokeLocation.getInsnNode();
                            Type methodType = Type.getMethodType(methodInsnNode.desc);
                            boolean isStatic = AsmUtils.isStatic(methodInsnNode);
                            Type[] argumentTypes = methodType.getArgumentTypes();
                            
//                            if(!isStatic) {
//                                // 取出this
//                                AsmOpUtils.loadVar(instructions, AsmOpUtils.OBJECT_ARRAY_TYPE, invokeArgsVariableNode.index);
//                                AsmOpUtils.push(instructions, 0);
//                                AsmOpUtils.arrayLoad(instructions, AsmOpUtils.OBJECT_TYPE);
//                                AsmOpUtils.checkCast(instructions, Type.getObjectType(methodInsnNode.owner));
//                            }
//                            
//                            for(int i = 0; i < argumentTypes.length; ++i) {
//                                AsmOpUtils.loadVar(instructions, AsmOpUtils.OBJECT_ARRAY_TYPE, invokeArgsVariableNode.index);
//                                AsmOpUtils.push(instructions, i + (isStatic ? 0 : 1));
//                                AsmOpUtils.arrayLoad(instructions, AsmOpUtils.OBJECT_TYPE);
//                                // TODO 这里直接 unbox 就可以了？？unbox里带有 check cast
//                                if(AsmOpUtils.needBox(argumentTypes[i])) {
//                                    AsmOpUtils.unbox(instructions, argumentTypes[i]);
//                                }else {
//                                    AsmOpUtils.checkCast(instructions, argumentTypes[i]);
//                                }
//                            }
                            
                        }else {
                            throw new IllegalArgumentException("location is not a InvokeLocation, location: " + location);
                        }
                    }

                    @Override
                    public Type getType(BindingContext bindingContext) {
                        throw new UnsupportedOperationException("InvokeLocation saver do not support getType()");
                    }
                    
                };
            }

            return stackSaver;
        }

        @Override
        public MethodInsnNode methodInsnNode() {
            return (MethodInsnNode) insnNode;
        }

    }

    /**
     * location identifying a synchronization trigger point
     */
    public static class SyncEnterLocation extends Location {
        /**
         * count identifying which synchronization should be taken as the trigger point.
         * if not specified as a parameter this defaults to the first synchronization.
         */
        private int count;

        public SyncEnterLocation(AbstractInsnNode insnNode, int count, boolean whenComplete) {
            super(insnNode, whenComplete);
            this.count = count;
            this.whenComplete = whenComplete;
            this.stackNeedSave = !whenComplete;
        }

        public LocationType getLocationType() {
            if (whenComplete) {
                return LocationType.SYNC_ENTER_COMPLETED;
            } else {
                return LocationType.SYNC_ENTER;
            }
        }

        @Override
        public StackSaver getStackSaver() {
            return new StackSaver() {

                @Override
                public void store(InsnList instructions, BindingContext bindingContext) {
                    LocalVariableNode variableNode = bindingContext.getMethodProcessor().initMonitorVariableNode();
                    AsmOpUtils.storeVar(instructions, AsmOpUtils.OBJECT_TYPE, variableNode.index);
                }

                @Override
                public void load(InsnList instructions, BindingContext bindingContext) {
                    LocalVariableNode variableNode = bindingContext.getMethodProcessor().initMonitorVariableNode();
                    AsmOpUtils.loadVar(instructions, AsmOpUtils.OBJECT_TYPE, variableNode.index);
                }

                @Override
                public Type getType(BindingContext bindingContext) {
                    return AsmOpUtils.OBJECT_TYPE;
                }

            };
        }
    }
    
    /**
     * location identifying a synchronization trigger point
     */
    public static class SyncExitLocation extends Location {
        /**
         * count identifying which synchronization should be taken as the trigger point.
         * if not specified as a parameter this defaults to the first synchronization.
         */
        private int count;

        public SyncExitLocation(AbstractInsnNode insnNode, int count, boolean whenComplete) {
            super(insnNode, whenComplete);
            this.count = count;
            this.whenComplete = whenComplete;
            this.stackNeedSave = !whenComplete;
        }

        public LocationType getLocationType() {
            if (whenComplete) {
                return LocationType.SYNC_ENTER_COMPLETED;
            } else {
                return LocationType.SYNC_ENTER;
            }
        }

        @Override
        public StackSaver getStackSaver() {
            return new StackSaver() {

                @Override
                public void store(InsnList instructions, BindingContext bindingContext) {
                    LocalVariableNode variableNode = bindingContext.getMethodProcessor().initMonitorVariableNode();
                    AsmOpUtils.storeVar(instructions, AsmOpUtils.OBJECT_TYPE, variableNode.index);
                }

                @Override
                public void load(InsnList instructions, BindingContext bindingContext) {
                    LocalVariableNode variableNode = bindingContext.getMethodProcessor().initMonitorVariableNode();
                    AsmOpUtils.loadVar(instructions, AsmOpUtils.OBJECT_TYPE, variableNode.index);
                }

                @Override
                public Type getType(BindingContext bindingContext) {
                    return AsmOpUtils.OBJECT_TYPE;
                }

            };
        }
    }

    /**
     * location identifying a throw trigger point
     */
    public static class ThrowLocation extends Location {
        /**
         * count identifying which throw operation should be taken as the trigger point.
         * if not specified as a parameter this defaults to the first throw.
         */
        private int count;

        public ThrowLocation(AbstractInsnNode insnNode, int count) {
            super(insnNode);
            this.count = count;
            stackNeedSave = true;
        }

        @Override
        public boolean canChangeByReturn() {
            return true;
        }

        public LocationType getLocationType() {
            return LocationType.THROW;
        }
        
        public StackSaver getStackSaver() {
            StackSaver stackSaver = new StackSaver() {

                @Override
                public void store(InsnList instructions, BindingContext bindingContext) {
                    LocalVariableNode throwVariableNode = bindingContext.getMethodProcessor().initThrowVariableNode();
                    AsmOpUtils.storeVar(instructions, Type.getType(Throwable.class), throwVariableNode.index);
                    
                }

                @Override
                public void load(InsnList instructions, BindingContext bindingContext) {
                    LocalVariableNode throwVariableNode = bindingContext.getMethodProcessor().initThrowVariableNode();
                    AsmOpUtils.loadVar(instructions, Type.getType(Throwable.class), throwVariableNode.index);
                }

                @Override
                public Type getType(BindingContext bindingContext) {
                    return Type.getType(Throwable.class);
                }
                
            };
            return stackSaver;
        }
    }

    /**
     * location identifying a method exit trigger point
     */
    public static class ExitLocation extends Location {

        public ExitLocation(AbstractInsnNode insnNode) {
            super(insnNode);
            stackNeedSave = true;
        }

        @Override
        public boolean canChangeByReturn() {
            return true;
        }

        public LocationType getLocationType() {
            return LocationType.EXIT;
        }
        
        public StackSaver getStackSaver() {
            StackSaver stackSaver = new StackSaver() {

                @Override
                public void store(InsnList instructions, BindingContext bindingContext) {
                    Type returnType = bindingContext.getMethodProcessor().getReturnType();
                    if(!returnType.equals(Type.VOID_TYPE)) {
                        LocalVariableNode returnVariableNode = bindingContext.getMethodProcessor().initReturnVariableNode();
                        AsmOpUtils.storeVar(instructions, returnType, returnVariableNode.index);
                    }
                }

                @Override
                public void load(InsnList instructions, BindingContext bindingContext) {
                    Type returnType = bindingContext.getMethodProcessor().getReturnType();
                    if(!returnType.equals(Type.VOID_TYPE)) {
                        LocalVariableNode returnVariableNode = bindingContext.getMethodProcessor().initReturnVariableNode();
                        AsmOpUtils.loadVar(instructions, returnType, returnVariableNode.index);
                    }
                }

                @Override
                public Type getType(BindingContext bindingContext) {
                    return bindingContext.getMethodProcessor().getReturnType();
                }
                
            };
            return stackSaver;
        }

    }

    /**
     * location identifying a method exceptional exit trigger point
     */
    public static class ExceptionExitLocation extends Location{
        public ExceptionExitLocation(AbstractInsnNode insnNode) {
            super(insnNode, true);
            stackNeedSave = true;
        }

        public LocationType getLocationType() {
            return LocationType.EXCEPTION_EXIT;
        }

        public StackSaver getStackSaver() {
            StackSaver stackSaver = new StackSaver() {

                @Override
                public void store(InsnList instructions, BindingContext bindingContext) {
                    LocalVariableNode throwVariableNode = bindingContext.getMethodProcessor().initThrowVariableNode();
                    AsmOpUtils.storeVar(instructions, Type.getType(Throwable.class), throwVariableNode.index);
                    
                }

                @Override
                public void load(InsnList instructions, BindingContext bindingContext) {
                    LocalVariableNode throwVariableNode = bindingContext.getMethodProcessor().initThrowVariableNode();
                    AsmOpUtils.loadVar(instructions, Type.getType(Throwable.class), throwVariableNode.index);
                }

                @Override
                public Type getType(BindingContext bindingContext) {
                    return Type.getType(Throwable.class);
                }
                
            };
            return stackSaver;
        }
    }

    /**
     * location identifying a method exceptional exit trigger point
     */
    public static class InvokeExceptionExitLocation extends Location implements MethodInsnNodeWare {
        private MethodInsnNode methodInsnNode;

        public InvokeExceptionExitLocation(MethodInsnNode methodInsnNode, AbstractInsnNode insnNode) {
            super(insnNode, true);
            stackNeedSave = true;
            this.methodInsnNode = methodInsnNode;
        }

        public LocationType getLocationType() {
            return LocationType.INVOKE_EXCEPTION_EXIT;
        }

        public StackSaver getStackSaver() {
            StackSaver stackSaver = new StackSaver() {

                @Override
                public void store(InsnList instructions, BindingContext bindingContext) {
                    LocalVariableNode throwVariableNode = bindingContext.getMethodProcessor().initThrowVariableNode();
                    AsmOpUtils.storeVar(instructions, Type.getType(Throwable.class), throwVariableNode.index);

                }

                @Override
                public void load(InsnList instructions, BindingContext bindingContext) {
                    LocalVariableNode throwVariableNode = bindingContext.getMethodProcessor().initThrowVariableNode();
                    AsmOpUtils.loadVar(instructions, Type.getType(Throwable.class), throwVariableNode.index);
                }

                @Override
                public Type getType(BindingContext bindingContext) {
                    return Type.getType(Throwable.class);
                }

            };
            return stackSaver;
        }

        @Override
        public MethodInsnNode methodInsnNode() {
            return methodInsnNode;
        }
    }
}