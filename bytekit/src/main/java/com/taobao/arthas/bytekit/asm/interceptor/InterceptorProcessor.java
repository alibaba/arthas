package com.taobao.arthas.bytekit.asm.interceptor;

import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.JumpInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LabelNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.TryCatchBlock;
import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.binding.BindingContext;
import com.taobao.arthas.bytekit.asm.binding.StackSaver;
import com.taobao.arthas.bytekit.asm.location.Location;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;
import com.taobao.arthas.bytekit.utils.Decompiler;

public class InterceptorProcessor {

    private LocationMatcher locationMatcher;

    /**
     * 插入的回调函数的配置
     */
    private InterceptorMethodConfig interceptorMethodConfig;

    /**
     * 插入的代码被 try/catch 包围的配置，注意有一些location插入try/catch会可能失败，因为不能确切知道栈上的情况
     */
    private InterceptorMethodConfig exceptionHandlerConfig;

    /**
     * 加载inlne类所需要的ClassLoader
     */
    private ClassLoader classLoader;

    public InterceptorProcessor(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public List<Location> process(MethodProcessor methodProcessor) throws Exception {
        List<Location> locations = locationMatcher.match(methodProcessor);

        List<Binding> interceptorBindings = interceptorMethodConfig.getBindings();

        for (Location location : locations) {

            // 有三小段代码，1: 保存当前栈上的值的 , 2: 插入的回调的 ， 3：恢复当前栈的

            InsnList toInsert = new InsnList();

            InsnList stackSaveInsnList = new InsnList();
            InsnList stackLoadInsnList = new InsnList();

            StackSaver stackSaver = null;
            if(location.isStackNeedSave()) {
                stackSaver = location.getStackSaver();
            }
            BindingContext bindingContext = new BindingContext(location, methodProcessor, stackSaver);

            if(stackSaver != null) {
                stackSaver.store(stackSaveInsnList, bindingContext);
                stackSaver.load(stackLoadInsnList, bindingContext);
            }


            Type methodType = Type.getMethodType(interceptorMethodConfig.getMethodDesc());
            Type[] argumentTypes = methodType.getArgumentTypes();
            // 检查回调函数的参数和 binding数一致
            if(interceptorBindings.size() != argumentTypes.length) {
                throw new IllegalArgumentException("interceptorBindings size no equals with interceptorMethod args size.");
            }

            // 把当前栈上的数据保存起来
            int fromStackBindingCount = 0;
            for (Binding binding : interceptorBindings) {
                if(binding.fromStack()) {
                    fromStackBindingCount++;
                }
            }
            // 只允许一个binding从栈上保存数据
            if(fromStackBindingCount > 1) {
                throw new IllegalArgumentException("interceptorBindings have more than one from stack Binding.");
            }


           // 组装好要调用的 static 函数的参数
            for(int i = 0 ; i < argumentTypes.length; ++i) {
                Binding binding = interceptorBindings.get(i);
                binding.pushOntoStack(toInsert, bindingContext);
                // 检查 回调函数的参数类型，看是否要box一下 ，检查是否原始类型就可以了。
                // 只有类型不一样时，才需要判断。比如两个都是 long，则不用判断
                Type bindingType = binding.getType(bindingContext);
                if(!bindingType.equals(argumentTypes[i])) {
                    if(AsmOpUtils.needBox(bindingType)) {
                        AsmOpUtils.box(toInsert, binding.getType(bindingContext));
                    }
                }
            }

            // TODO 要检查 binding 和 回调的函数的参数类型是否一致。回调函数的类型可以是 Object，或者super。但是不允许一些明显的类型问题，比如array转到int

            toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, interceptorMethodConfig.getOwner(), interceptorMethodConfig.getMethodName(),
                    interceptorMethodConfig.getMethodDesc(), false));

            if (!methodType.getReturnType().equals(Type.VOID_TYPE)) {
                if (location.canChangeByReturn()) {
                    // 当回调函数有返回值时，需要更新到之前保存的栈上
                    // TODO 这里应该有 type 的问题？需要检查是否要 box
                    Type returnType = methodType.getReturnType();
                    Type stackSaverType = stackSaver.getType(bindingContext);
                    if (!returnType.equals(stackSaverType)) {
                        AsmOpUtils.unbox(toInsert, stackSaverType);
                    }
                    stackSaver.store(toInsert, bindingContext);
                } else {
                    // 没有使用到回调函数的返回值的话，则需要从栈上清理掉
                    int size = methodType.getReturnType().getSize();
                    if (size == 1) {
                        AsmOpUtils.pop(toInsert);
                    } else if (size == 2) {
                        AsmOpUtils.pop2(toInsert);
                    }
                }
            }


            TryCatchBlock errorHandlerTryCatchBlock = null;
            // 生成的代码用try/catch包围起来
            if( exceptionHandlerConfig != null) {
                LabelNode gotoDest = new LabelNode();

                errorHandlerTryCatchBlock = new TryCatchBlock(methodProcessor.getMethodNode(), exceptionHandlerConfig.getSuppress());
                toInsert.insertBefore(toInsert.getFirst(), errorHandlerTryCatchBlock.getStartLabelNode());
                toInsert.add(new JumpInsnNode(Opcodes.GOTO, gotoDest));
                toInsert.add(errorHandlerTryCatchBlock.getEndLabelNode());
//                这里怎么把栈上的数据保存起来？还是强制回调函数的第一个参数是 exception，后面的binding可以随便搞。

//                MethodInsnNode printStackTrace = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
//                toInsert.add(printStackTrace);

                errorHandler(methodProcessor, toInsert);

                toInsert.add(gotoDest);
            }

//            System.err.println(Decompiler.toString(toInsert));


            stackSaveInsnList.add(toInsert);
            stackSaveInsnList.add(stackLoadInsnList);
            if (location.isWhenComplete()) {
                methodProcessor.getMethodNode().instructions.insert(location.getInsnNode(), stackSaveInsnList);
            }else {
                methodProcessor.getMethodNode().instructions.insertBefore(location.getInsnNode(), stackSaveInsnList);
            }

            if( exceptionHandlerConfig != null) {
                errorHandlerTryCatchBlock.sort();
            }

            // inline callback
            if(interceptorMethodConfig.isInline()) {
//                Class<?> forName = Class.forName(Type.getObjectType(interceptorMethodConfig.getOwner()).getClassName());

                Class<?> forName = classLoader.loadClass(Type.getObjectType(interceptorMethodConfig.getOwner()).getClassName());
                MethodNode toInlineMethodNode = AsmUtils.findMethod(AsmUtils.loadClass(forName).methods, interceptorMethodConfig.getMethodName(), interceptorMethodConfig.getMethodDesc());

                methodProcessor.inline(interceptorMethodConfig.getOwner(), toInlineMethodNode);
            }
            if(exceptionHandlerConfig != null && exceptionHandlerConfig.isInline()) {
//                Class<?> forName = Class.forName(Type.getObjectType(exceptionHandlerConfig.getOwner()).getClassName());

                Class<?> forName = classLoader.loadClass(Type.getObjectType(exceptionHandlerConfig.getOwner()).getClassName());
                MethodNode toInlineMethodNode = AsmUtils.findMethod(AsmUtils.loadClass(forName).methods, exceptionHandlerConfig.getMethodName(), exceptionHandlerConfig.getMethodDesc());

                methodProcessor.inline(exceptionHandlerConfig.getOwner(), toInlineMethodNode);
            }

//            System.err.println(Decompiler.toString(methodProcessor.getMethodNode()));
//            System.err.println(AsmUtils.toASMCode(methodProcessor.getMethodNode()));
        }
        
        return locations;
    }

    private void errorHandler(MethodProcessor methodProcessor, InsnList insnList) {
//      MethodInsnNode printStackTrace = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
//      insnList.add(printStackTrace);
        // 第一个参数要求是 throwable ，或者一个exception ？
        // 有很多 binding 并不能使用的，因为location不生效
        BindingContext bindingContext = new BindingContext(null, methodProcessor, null);
        Type methodType = Type.getMethodType(this.exceptionHandlerConfig.getMethodDesc());
        Type[] argumentTypes = methodType.getArgumentTypes();
        List<Binding> bindings = this.exceptionHandlerConfig.getBindings();
        if(bindings.size() + 1 != argumentTypes.length) {
            throw new IllegalArgumentException("errorHandler bindings size do not match error method args size.");
        }
        if(!argumentTypes[0].equals(Type.getType(Throwable.class))) {
            throw new IllegalArgumentException("errorHandler method first arg type must be Throwable.");
        }
        // 组装好要调用的 static 函数的参数
        for(Binding binding: bindings) {
            if(binding.fromStack()) {
                throw new IllegalArgumentException("errorHandler binding can not load value from stack!");
            }
            binding.pushOntoStack(insnList, bindingContext);
            // 检查 回调函数的参数类型，看是否要box一下 ，检查是否原始类型就可以了。
            if(AsmOpUtils.needBox(binding.getType(bindingContext))) {
                AsmOpUtils.box(insnList, binding.getType(bindingContext));
            }
        }

        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, exceptionHandlerConfig.getOwner(), exceptionHandlerConfig.getMethodName(),
                exceptionHandlerConfig.getMethodDesc(), false));

        int size = methodType.getReturnType().getSize();
        if (size == 1) {
            AsmOpUtils.pop(insnList);
        } else if (size == 2) {
            AsmOpUtils.pop2(insnList);
        }
    }

    public LocationMatcher getLocationMatcher() {
        return locationMatcher;
    }

    public void setLocationMatcher(LocationMatcher locationMatcher) {
        this.locationMatcher = locationMatcher;
    }

    public InterceptorMethodConfig getInterceptorMethodConfig() {
        return interceptorMethodConfig;
    }

    public void setInterceptorMethodConfig(InterceptorMethodConfig interceptorMethodConfig) {
        this.interceptorMethodConfig = interceptorMethodConfig;
    }

    public InterceptorMethodConfig getExceptionHandlerConfig() {
        return exceptionHandlerConfig;
    }

    public void setExceptionHandlerConfig(InterceptorMethodConfig exceptionHandlerConfig) {
        this.exceptionHandlerConfig = exceptionHandlerConfig;
    }

}
