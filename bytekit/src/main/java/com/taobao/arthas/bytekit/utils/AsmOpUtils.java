package com.taobao.arthas.bytekit.utils;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.commons.Method;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.FieldInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.IntInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LdcInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LocalVariableNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.TypeInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.VarInsnNode;

public class AsmOpUtils {

	private static final Type BYTE_TYPE = Type.getObjectType("java/lang/Byte");

	private static final Type BOOLEAN_TYPE = Type.getObjectType("java/lang/Boolean");

	private static final Type SHORT_TYPE = Type.getObjectType("java/lang/Short");

	private static final Type CHARACTER_TYPE = Type.getObjectType("java/lang/Character");

	private static final Type INTEGER_TYPE = Type.getObjectType("java/lang/Integer");

	private static final Type FLOAT_TYPE = Type.getObjectType("java/lang/Float");

	private static final Type LONG_TYPE = Type.getObjectType("java/lang/Long");

	private static final Type DOUBLE_TYPE = Type.getObjectType("java/lang/Double");

	public static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");

	public static final Type OBJECT_ARRAY_TYPE = Type.getType(Object[].class);

	public static final Type STRING_TYPE = Type.getObjectType("java/lang/String");

	public static final Type STRING_ARRAY_TYPE = Type.getType(String[].class);

	private static final Type NUMBER_TYPE = Type.getObjectType("java/lang/Number");

	private static final Method BOOLEAN_VALUE = Method.getMethod("boolean booleanValue()");

	private static final Method CHAR_VALUE = Method.getMethod("char charValue()");

	private static final Method BYTE_VALUE = Method.getMethod("byte byteValue()");

	private static final Method SHORT_VALUE = Method.getMethod("short shortValue()");

	private static final Method INT_VALUE = Method.getMethod("int intValue()");

	private static final Method FLOAT_VALUE = Method.getMethod("float floatValue()");

	private static final Method LONG_VALUE = Method.getMethod("long longValue()");

	private static final Method DOUBLE_VALUE = Method.getMethod("double doubleValue()");

    public static boolean isBoxType(final Type type) {
        if (BYTE_TYPE.equals(type) || BOOLEAN_TYPE.equals(type) || SHORT_TYPE.equals(type)
                || CHARACTER_TYPE.equals(type) || INTEGER_TYPE.equals(type) || FLOAT_TYPE.equals(type)
                || LONG_TYPE.equals(type) || DOUBLE_TYPE.equals(type)) {
            return true;
        }
        return false;
    }

	public static Type getBoxedType(final Type type) {
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

    public static Method getUnBoxMethod(final Type type) {
        switch (type.getSort()) {
        case Type.BYTE:
            return BYTE_VALUE;
        case Type.BOOLEAN:
            return BOOLEAN_VALUE;
        case Type.SHORT:
            return SHORT_VALUE;
        case Type.CHAR:
            return CHAR_VALUE;
        case Type.INT:
            return INT_VALUE;
        case Type.FLOAT:
            return FLOAT_VALUE;
        case Type.LONG:
            return LONG_VALUE;
        case Type.DOUBLE:
            return DOUBLE_VALUE;
        }
        throw new IllegalArgumentException(type + " is not a primitive type.");
    }

	public static void newInstance(final InsnList instructions, final Type type) {
		instructions.add(new TypeInsnNode(Opcodes.NEW, type.getInternalName()));
	}

    public static void push(InsnList insnList, final int value) {
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

	public static void push(InsnList insnList, final String value) {
		if (value == null) {
			insnList.add(new InsnNode(Opcodes.ACONST_NULL));
		} else {
			insnList.add(new LdcInsnNode(value));
		}
	}

    public static void pushNUll(InsnList insnList) {
        insnList.add(new InsnNode(Opcodes.ACONST_NULL));
    }

	/**
	 * @see org.objectweb.asm.tree.LdcInsnNode#cst
	 * @param value
	 */
	public static void ldc(InsnList insnList, Object value) {
	    insnList.add(new LdcInsnNode(value));
	}

	public static void newArray(final InsnList insnList, final Type type) {
		insnList.add(new TypeInsnNode(Opcodes.ANEWARRAY, type.getInternalName()));
	}

	public static void dup(final InsnList insnList) {
		insnList.add(new InsnNode(Opcodes.DUP));
	}

	public static void dup2(final InsnList insnList) {
		insnList.add(new InsnNode(Opcodes.DUP2));
	}

	public static void dupX1(final InsnList insnList) {
		insnList.add(new InsnNode(Opcodes.DUP_X1));
	}

	public static void dupX2(final InsnList insnList) {
		insnList.add(new InsnNode(Opcodes.DUP_X2));
	}

    /**
     * Generates a DUP2_X1 instruction.
     */
    public static void dup2X1(final InsnList insnList) {
        insnList.add(new InsnNode(Opcodes.DUP2_X1));
    }

    /**
     * Generates a DUP2_X2 instruction.
     */
    public static void dup2X2(final InsnList insnList) {
        insnList.add(new InsnNode(Opcodes.DUP2_X2));
    }


	public static void pop(final InsnList insnList) {
		insnList.add(new InsnNode(Opcodes.POP));
	}

    /**
     * Generates a POP2 instruction.
     */
    public static void pop2(final InsnList insnList) {
        insnList.add(new InsnNode(Opcodes.POP2));
    }

	public static void swap(final InsnList insnList) {
		insnList.add(new InsnNode(Opcodes.SWAP));
	}

    /**
     * Generates the instructions to swap the top two stack values.
     *
     * @param prev
     *            type of the top - 1 stack value.
     * @param type
     *            type of the top stack value.
     */
    public static void swap(final InsnList insnList, final Type prev, final Type type) {
        if (type.getSize() == 1) {
            if (prev.getSize() == 1) {
                swap(insnList); // same as dupX1(), pop();
            } else {
                dupX2(insnList);
                pop(insnList);
            }
        } else {
            if (prev.getSize() == 1) {
                dup2X1(insnList);
                pop2(insnList);
            } else {
                dup2X2(insnList);
                pop2(insnList);
            }
        }
    }

	public static void box(final InsnList instructions, Type type) {
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

	public static void invokeConstructor(final InsnList instructions, final Type type, final Method method) {
		String owner = type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
		instructions
				.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, owner, method.getName(), method.getDescriptor(), false));
	}

	/**
	 *
	 * @param instructions
	 * @param type
	 * @see org.objectweb.asm.commons.GeneratorAdapter#unbox(Type)
	 */
	public static void unbox(final InsnList instructions, Type type) {
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

    public static boolean needBox(Type type) {
        switch (type.getSort()) {
        case Type.BYTE:
        case Type.BOOLEAN:
        case Type.SHORT:
        case Type.CHAR:
        case Type.INT:
        case Type.FLOAT:
        case Type.LONG:
        case Type.DOUBLE:
            return true;
        }
        return false;
    }

	public static void getStatic(final InsnList insnList, final Type owner, final String name, final Type type) {
		insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, owner.getInternalName(), name, type.getDescriptor()));
	}

	/**
	 * Generates the instruction to push the value of a non static field on the
	 * stack.
	 *
	 * @param owner
	 *            the class in which the field is defined.
	 * @param name
	 *            the name of the field.
	 * @param type
	 *            the type of the field.
	 */
	public static void getField(final InsnList insnList, final Type owner, final String name, final Type type) {
		insnList.add(new FieldInsnNode(Opcodes.GETFIELD, owner.getInternalName(), name, type.getDescriptor()));
	}

	public static void arrayStore(final InsnList instructions, final Type type) {
        instructions.add(new InsnNode(type.getOpcode(Opcodes.IASTORE)));
    }

	public static void arrayLoad(final InsnList instructions, final Type type) {
        instructions.add(new InsnNode(type.getOpcode(Opcodes.IALOAD)));
    }


	/**
	 * Generates the instruction to load 'this' on the stack.
	 * @see org.objectweb.asm.commons.GeneratorAdapter#loadThis()
	 * @param instructions
	 */
	  public static void loadThis(final InsnList instructions) {
	    instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
	  }

	/**
	 * Generates the instructions to load all the method arguments on the stack,
	 * as a single object array.
	 *
	 * @see org.objectweb.asm.commons.GeneratorAdapter#loadArgArray()
	 */
	public static void loadArgArray(final InsnList instructions, MethodNode methodNode) {
		boolean isStatic = AsmUtils.isStatic(methodNode);
		Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
		push(instructions, argumentTypes.length);
		newArray(instructions, OBJECT_TYPE);
		for (int i = 0; i < argumentTypes.length; i++) {
			dup(instructions);
			push(instructions, i);
			loadArg(isStatic, instructions, argumentTypes, i);
			box(instructions, argumentTypes[i]);
			arrayStore(instructions, OBJECT_TYPE);
		}
	}

	public static  void loadArgs(final InsnList instructions, MethodNode methodNode) {
        Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
        boolean isStatic = AsmUtils.isStatic(methodNode);
        for (int i = 0; i < argumentTypes.length; i++) {
            loadArg(isStatic, instructions, argumentTypes, i);
        }
    }

	public static void loadArg(boolean staticAccess, final InsnList instructions, Type[] argumentTypes, int i) {
        final int index = getArgIndex(staticAccess, argumentTypes, i);
        final Type type = argumentTypes[i];
        instructions.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), index));
    }

    static int getArgIndex(boolean staticAccess, final Type[] argumentTypes, final int arg) {
        int index = staticAccess ? 0 : 1;
        for (int i = 0; i < arg; i++) {
            index += argumentTypes[i].getSize();
        }
        return index;
    }

    public static void loadVar(final InsnList instructions, Type type, final int index) {
        instructions.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), index));
    }

    public static void storeVar(final InsnList instructions, Type type, final int index) {
        instructions.add(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), index));
    }

    /**
     * Generates a type dependent instruction.
     *
     * @param opcode
     *            the instruction's opcode.
     * @param type
     *            the instruction's operand.
     */
    private static void typeInsn(final InsnList instructions, final int opcode, final Type type) {
        instructions.add(new TypeInsnNode(opcode, type.getInternalName()));
    }

    /**
     * Generates the instruction to check that the top stack value is of the
     * given type.
     *
     * @param type
     *            a class or interface type.
     */
    public static void checkCast(final InsnList instructions, final Type type) {
        if (!type.equals(OBJECT_TYPE)) {
            typeInsn(instructions, Opcodes.CHECKCAST, type);
        }
    }

    public static void throwException(final InsnList instructions) {
        instructions.add(new InsnNode(Opcodes.ATHROW));
    }

    public static boolean isReturnCode(final int opcode) {
        return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
    }

    public static List<LocalVariableNode> validVariables(List<LocalVariableNode> localVariables,
            AbstractInsnNode currentInsnNode) {
        List<LocalVariableNode> results = new ArrayList<LocalVariableNode>();

        // find out current valid local variables
        for (LocalVariableNode localVariableNode : localVariables) {
            for (AbstractInsnNode iter = localVariableNode.start; iter != null
                    && (!iter.equals(localVariableNode.end)); iter = iter.getNext()) {
                if (iter.equals(currentInsnNode)) {
                    results.add(localVariableNode);
                    break;
                }
            }
        }

        return results;
    }
}
