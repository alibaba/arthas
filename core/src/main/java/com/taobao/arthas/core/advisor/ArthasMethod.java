package com.taobao.arthas.core.advisor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.alibaba.deps.org.objectweb.asm.Type;
import com.taobao.arthas.core.util.StringUtils;

/**
 * Arthas方法类
 *
 * 该类封装了Java方法的反射信息，主要用于tt（TimeTunnel）命令的方法重放功能。
 * 它将ASM类型系统的方法描述符转换为Java反射的Method或Constructor对象，
 * 从而支持方法的动态调用。
 *
 * <h2>核心功能</h2>
 * <ul>
 * <li>解析ASM方法描述符，获取方法参数类型</li>
 * <li>支持普通方法和构造方法</li>
 * <li>提供反射调用方法的能力</li>
 * <li>支持访问权限控制（setAccessible）</li>
 * </ul>
 *
 * <h2>使用场景</h2>
 * 主要用于tt命令，该命令可以记录方法调用信息，并在之后重新调用（重放）该方法。
 * 重放时需要根据类名、方法名和方法描述符来定位具体的方法，然后通过反射进行调用。
 *
 * @author vlinux on 15/5/24
 * @author hengyunabc 2020-05-20
 */
public class ArthasMethod {
    /**
     * 目标类
     * 方法或构造方法所属的类
     */
    private final Class<?> clazz;

    /**
     * 方法名称
     * 对于构造方法，名称为"<init>"
     */
    private final String methodName;

    /**
     * 方法描述符
     * ASM类型的描述符，例如："(Ljava/lang/String;I)V"表示参数为String和int，返回值为void
     */
    private final String methodDesc;

    /**
     * 构造方法对象
     * 如果methodName为"<init>"，则此字段有值
     */
    private Constructor<?> constructor;

    /**
     * 普通方法对象
     * 如果MethodName不是"<init>"，则此字段有值
     */
    private Method method;

    /**
     * 初始化方法对象（延迟初始化）
     *
     * 该方法负责将ASM类型的方法描述符转换为Java反射的Method或Constructor对象。
     * 采用延迟初始化策略，只有在真正需要调用方法时才进行解析。
     *
     * 执行流程：
     * 1. 检查是否已经初始化，如果已初始化则直接返回
     * 2. 获取类的ClassLoader
     * 3. 解析方法描述符，获取参数类型列表
     * 4. 遍历每个参数，将ASM类型转换为Java Class对象
     * 5. 根据方法名判断是构造方法还是普通方法
     * 6. 使用反射获取对应的Method或Constructor对象
     *
     * @throws RuntimeException 如果解析或获取方法失败
     */
    private void initMethod() {
        // 如果已经初始化过，直接返回
        if (constructor != null || method != null) {
            return;
        }

        try {
            // 获取类的ClassLoader
            ClassLoader loader = this.clazz.getClassLoader();
            // 解析ASM类型的方法描述符
            final Type asmType = Type.getMethodType(methodDesc);

            // 准备参数类型数组
            final Class<?>[] argsClasses = new Class<?>[asmType.getArgumentTypes().length];
            for (int index = 0; index < argsClasses.length; index++) {
                // 将ASM类型转换为JVM Class对象
                final Class<?> argumentClass;
                final Type argumentAsmType = asmType.getArgumentTypes()[index];
                // 根据ASM类型的种类进行转换
                switch (argumentAsmType.getSort()) {
                case Type.BOOLEAN: {
                    // 布尔类型
                    argumentClass = boolean.class;
                    break;
                }
                case Type.CHAR: {
                    // 字符类型
                    argumentClass = char.class;
                    break;
                }
                case Type.BYTE: {
                    // 字节类型
                    argumentClass = byte.class;
                    break;
                }
                case Type.SHORT: {
                    // 短整型
                    argumentClass = short.class;
                    break;
                }
                case Type.INT: {
                    // 整型
                    argumentClass = int.class;
                    break;
                }
                case Type.FLOAT: {
                    // 单精度浮点型
                    argumentClass = float.class;
                    break;
                }
                case Type.LONG: {
                    // 长整型
                    argumentClass = long.class;
                    break;
                }
                case Type.DOUBLE: {
                    // 双精度浮点型
                    argumentClass = double.class;
                    break;
                }
                case Type.ARRAY: {
                    // 数组类型，使用ASM内部名称加载
                    argumentClass = toClass(loader, argumentAsmType.getInternalName());
                    break;
                }
                case Type.VOID: {
                    // void类型
                    argumentClass = void.class;
                    break;
                }
                case Type.OBJECT:
                case Type.METHOD:
                default: {
                    // 对象类型，使用全限定类名加载
                    argumentClass = toClass(loader, argumentAsmType.getClassName());
                    break;
                }
                }

                // 保存转换后的参数类型
                argsClasses[index] = argumentClass;
            }

            // 判断是构造方法还是普通方法
            if ("<init>".equals(this.methodName)) {
                // 构造方法：使用getDeclaredConstructor
                this.constructor = clazz.getDeclaredConstructor(argsClasses);
            } else {
                // 普通方法：使用getDeclaredMethod
                this.method = clazz.getDeclaredMethod(methodName, argsClasses);
            }
        } catch (Throwable e) {
            // 将任何异常包装为RuntimeException抛出
            throw new RuntimeException(e);
        }

    }

    /**
     * 将类名转换为Class对象
     *
     * @param loader 类加载器
     * @param className 类名（可能是内部名称或全限定名）
     * @return 对应的Class对象
     * @throws ClassNotFoundException 如果类未找到
     */
    private Class<?> toClass(ClassLoader loader, String className) throws ClassNotFoundException {
        // 使用反射加载类，normalizeClassName处理类名格式
        return Class.forName(StringUtils.normalizeClassName(className), true, toClassLoader(loader));
    }

    /**
     * 处理ClassLoader
     * 如果传入的ClassLoader为null，则使用当前类的ClassLoader
     *
     * @param loader 原始的ClassLoader
     * @return 有效的ClassLoader，保证不为null
     */
    private ClassLoader toClassLoader(ClassLoader loader) {
        return null != loader ? loader : ArthasMethod.class.getClassLoader();
    }

    /**
     * 获取方法名称
     *
     * @return 返回方法名称，对于构造方法返回"<init>"
     */
    public String getName() {
        return this.methodName;
    }

    /**
     * 获取方法的字符串表示
     * 返回方法或构造方法的完整描述信息
     *
     * @return 方法的toString()结果，如果未初始化成功则返回"ERROR_METHOD"
     */
    @Override
    public String toString() {
        // 确保方法对象已初始化
        initMethod();
        if (constructor != null) {
            // 返回构造方法的字符串表示
            return constructor.toString();
        } else if (method != null) {
            // 返回普通方法的字符串表示
            return method.toString();
        }
        // 初始化失败的情况
        return "ERROR_METHOD";
    }

    /**
     * 检查方法是否可访问
     * 判断当前方法或构造方法的accessible标志位
     *
     * @return 如果可访问返回true，否则返回false
     */
    public boolean isAccessible() {
        // 确保方法对象已初始化
        initMethod();
        if (this.method != null) {
            // 检查普通方法的访问权限
            return method.isAccessible();
        } else if (this.constructor != null) {
            // 检查构造方法的访问权限
            return constructor.isAccessible();
        }
        return false;
    }

    /**
     * 设置方法的可访问性
     * 用于覆盖Java的访问控制检查，允许访问private等方法
     *
     * @param accessFlag true表示设置为可访问，false表示恢复默认状态
     */
    public void setAccessible(boolean accessFlag) {
        // 确保方法对象已初始化
        initMethod();
        if (constructor != null) {
            // 设置构造方法的可访问性
            constructor.setAccessible(accessFlag);
        } else if (method != null) {
            // 设置普通方法的可访问性
            method.setAccessible(accessFlag);
        }
    }

    /**
     * 调用方法或构造方法
     * 这是ArthasMethod的核心方法，用于反射调用封装的方法
     *
     * 执行流程：
     * 1. 确保方法对象已初始化
     * 2. 判断是普通方法还是构造方法
     * 3. 使用反射进行调用
     * 4. 返回调用结果
     *
     * @param target 目标对象，对于静态方法或构造方法应为null
     * @param args 方法参数列表
     * @return 方法调用的返回值，构造方法返回新创建的对象，void方法返回null
     * @throws IllegalAccessException 如果访问权限不足
     * @throws InvocationTargetException 如果方法调用抛出异常
     * @throws InstantiationException 如果实例化失败（构造方法）
     */
    public Object invoke(Object target, Object... args)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // 确保方法对象已初始化
        initMethod();
        if (method != null) {
            // 调用普通方法：Method.invoke(目标对象, 参数数组)
            return method.invoke(target, args);
        } else if (this.constructor != null) {
            // 调用构造方法：Constructor.newInstance(参数数组)
            return constructor.newInstance(args);
        }
        // 不应该执行到这里
        return null;
    }

    /**
     * 构造ArthasMethod实例
     * 创建一个封装方法信息的对象
     *
     * @param clazz 方法所属的类
     * @param methodName 方法名称，对于构造方法应为"<init>"
     * @param methodDesc ASM类型的方法描述符
     */
    public ArthasMethod(Class<?> clazz, String methodName, String methodDesc) {
        this.clazz = clazz;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }
}
