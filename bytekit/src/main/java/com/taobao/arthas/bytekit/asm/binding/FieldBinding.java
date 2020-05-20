package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.FieldNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;

public class FieldBinding extends Binding {
    /**
     * maybe null
     */
    private Type owner;

    private boolean box = false;

    private String name;

    private boolean isStatic = false;

    /**
     * maybe null
     */
    private Type type;
    
    public FieldBinding(Type owner, String name, Type type, boolean isStatic, boolean box) {
        this.owner = owner;
        this.name = name;
        this.isStatic = isStatic;
        this.box = box;
        this.type = type;
    }

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        Type onwerType = owner;
        Type fieldType = type;
        boolean fieldIsStatic = isStatic;
        if (owner == null) {
            onwerType = Type.getObjectType(bindingContext.getMethodProcessor().getOwner());
        }
        // 当type是null里，需要从ClassNode里查找到files，确定type
        MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
        if (fieldType == null) {
            ClassNode classNode = methodProcessor.getClassNode();
            if (classNode == null) {
                throw new IllegalArgumentException(
                        "classNode is null, cann not get owner type. FieldBinding name:" + name);
            }
            FieldNode field = AsmUtils.findField(classNode.fields, name);
            if (field == null) {
                throw new IllegalArgumentException("can not find field in ClassNode. FieldBinding name:" + name);
            }
            fieldType = Type.getType(field.desc);
            if ((field.access & Opcodes.ACC_STATIC) != 0) {
                fieldIsStatic = true;
            }else {
                fieldIsStatic = false;
            }
        }

        if (fieldIsStatic) {
            AsmOpUtils.getStatic(instructions, onwerType, name, fieldType);
        } else {
            methodProcessor.loadThis(instructions);
            AsmOpUtils.getField(instructions, onwerType, name, fieldType);
        }
        if (box) {
            AsmOpUtils.box(instructions, fieldType);
        }
    }
    
    @Override
    public Type getType(BindingContext bindingContext) {
        Type fieldType = type;
        if (fieldType == null) {
            ClassNode classNode = bindingContext.getMethodProcessor().getClassNode();
            if (classNode == null) {
                throw new IllegalArgumentException(
                        "classNode is null, cann not get owner type. FieldBinding name:" + name);
            }
            FieldNode field = AsmUtils.findField(classNode.fields, name);
            if (field == null) {
                throw new IllegalArgumentException("can not find field in ClassNode. FieldBinding name:" + name);
            }
            fieldType = Type.getType(field.desc);
        }
        return fieldType;
    }

}
