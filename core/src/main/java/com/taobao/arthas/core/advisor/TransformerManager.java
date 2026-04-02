package com.taobao.arthas.core.advisor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ClassFileTransformer统一管理类
 *
 * <p>此类负责管理Arthas中所有的ClassFileTransformer，提供统一的添加、删除和执行接口。
 * 每个增强命令（如watch、trace等）都对应一个Enhancer，Enhancer会产生一个Transformer，
 * 这些Transformer都由此类统一管理和调度。</p>
 *
 * <p><b>核心功能：</b></p>
 * <pre>
 * * 统一管理 ClassFileTransformer
 * * 每个增强命令对应一个 Enhancer ，也统一在这里管理
 * </pre>
 *
 * <p><b>Transformer分类：</b></p>
 * <ol>
 *   <li><b>reTransformers：</b>先于watch/trace执行的Transformer，用于类重定义场景</li>
 *   <li><b>watchTransformers：</b>用于watch命令的Transformer，监控方法的进入、退出和异常</li>
 *   <li><b>traceTransformers：</b>用于trace命令的Transformer，跟踪方法调用链路</li>
 *   <li><b>lazyTransformers：</b>懒加载模式的Transformer，用于类首次加载时的增强</li>
 * </ol>
 *
 * <p><b>执行顺序：</b></p>
 * <pre>
 * reTransformers -> watchTransformers -> traceTransformers
 * </pre>
 *
 * <p><b>线程安全：</b></p>
 * <p>所有Transformer列表都使用CopyOnWriteArrayList，支持并发修改和遍历。</p>
 *
 * <p><b>设计说明：</b></p>
 * <ul>
 *   <li>TODO 改进为全部用 order 排序？</li>
 *   <li>使用聚合的ClassFileTransformer来统一调用各个类型的Transformer</li>
 *   <li>支持retransform（类重定义）和lazy load（类首次加载）两种模式</li>
 * </ul>
 *
 * @see com.taobao.arthas.core.advisor.Enhancer
 * @author hengyunabc
 * @since 2020-05-18
 */
public class TransformerManager {

    /**
     * Java Instrumentation API实例，用于注册和注销ClassFileTransformer
     */
    private Instrumentation instrumentation;

    /**
     * 用于watch命令的Transformer列表
     *
     * <p>这些Transformer用于监控方法的进入、正常退出和异常退出，
     * 实现watch命令的功能。支持并发修改和遍历。</p>
     */
    private List<ClassFileTransformer> watchTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();

    /**
     * 用于trace命令的Transformer列表
     *
     * <p>这些Transformer用于跟踪方法内部的调用链路，实现trace命令的功能。
     * 支持并发修改和遍历。</p>
     */
    private List<ClassFileTransformer> traceTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();

    /**
     * 先于watch/trace执行的Transformer列表
     *
     * <p>这些Transformer在类重定义时优先执行，用于某些需要在其他增强之前进行的处理。
     * 支持并发修改和遍历。</p>
     *
     * <p>TODO 改进为全部用 order 排序？</p>
     */
    private List<ClassFileTransformer> reTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();

    /**
     * 懒加载模式的Transformer列表
     *
     * <p>这些Transformer用于在类首次加载时进行增强，而不是对已加载的类进行重定义。
     * 这些transformer需要用addTransformer(transformer, false)注册才能在类首次加载时工作。
     * 支持并发修改和遍历。</p>
     */
    private List<ClassFileTransformer> lazyTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();

    /**
     * 聚合的ClassFileTransformer，用于retransform模式
     *
     * <p>这是一个聚合的Transformer，它内部按顺序调用reTransformers、watchTransformers和traceTransformers。
     * 使用canRetransform=true注册，用于对已加载的类进行重定义。</p>
     */
    private ClassFileTransformer classFileTransformer;

    /**
     * 聚合的ClassFileTransformer，用于懒加载模式
     *
     * <p>这是一个聚合的Transformer，它内部调用lazyTransformers。
     * 使用canRetransform=false注册，只有在类首次定义时才会被调用。
     * 用于在类首次加载时进行增强。</p>
     */
    private ClassFileTransformer lazyClassFileTransformer;

    /**
     * 构造函数，初始化TransformerManager
     *
     * <p>此构造函数会创建两个聚合的ClassFileTransformer，并注册到Instrumentation实例中。</p>
     *
     * <p><b>初始化流程：</b></p>
     * <ol>
     *   <li>保存Instrumentation实例的引用</li>
     *   <li>创建retransform模式的聚合Transformer（classFileTransformer）</li>
     *   <li>创建懒加载模式的聚合Transformer（lazyClassFileTransformer）</li>
     *   <li>将两个聚合Transformer注册到Instrumentation</li>
     * </ol>
     *
     * <p><b>关键参数：</b></p>
     * <ul>
     *   <li>canRetransform=true：允许对已加载的类进行重定义</li>
     *   <li>canRetransform=false：只在类首次定义时调用</li>
     * </ul>
     *
     * @param instrumentation Java Instrumentation API实例，用于注册Transformer
     */
    public TransformerManager(Instrumentation instrumentation) {
        // 保存Instrumentation实例的引用
        this.instrumentation = instrumentation;

        // 创建retransform模式的聚合Transformer
        // 这个Transformer会按顺序调用reTransformers、watchTransformers和traceTransformers
        classFileTransformer = new ClassFileTransformer() {

            /**
             * 转换类文件
             *
             * <p>此方法按顺序调用各类Transformer，前一个Transformer的输出作为后一个Transformer的输入。</p>
             *
             * <p><b>执行顺序：</b></p>
             * <pre>
             * 原始字节码 -> reTransformers -> watchTransformers -> traceTransformers -> 最终字节码
             * </pre>
             *
             * <p><b>链式处理：</b></p>
             * <p>每个Transformer处理后的字节码会作为下一个Transformer的输入，
             * 如果某个Transformer返回null，则使用前一个Transformer的输出。</p>
             *
             * @param loader 定义要转换的类的加载器；如果是引导加载器，则为 null
             * @param className 完全限定类名（如 "java/lang/String"）
             * @param classBeingRedefined 如果是被重定义或重转换的类，则为该类；如果是首次加载，则为 null
             * @param protectionDomain 类的定义域
             * @param classfileBuffer 类文件的输入字节码缓冲区
             * @return 转换后的类文件字节码缓冲区；如果没有转换，则返回null
             * @throws IllegalClassFormatException 如果类文件格式不合法
             */
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                // 第一步：应用reTransformers
                for (ClassFileTransformer classFileTransformer : reTransformers) {
                    byte[] transformResult = classFileTransformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    // 如果Transformer返回了新字节码，则更新缓冲区
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                // 第二步：应用watchTransformers
                for (ClassFileTransformer classFileTransformer : watchTransformers) {
                    byte[] transformResult = classFileTransformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    // 如果Transformer返回了新字节码，则更新缓冲区
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                // 第三步：应用traceTransformers
                for (ClassFileTransformer classFileTransformer : traceTransformers) {
                    byte[] transformResult = classFileTransformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    // 如果Transformer返回了新字节码，则更新缓冲区
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                // 返回最终的转换结果
                return classfileBuffer;
            }

        };
        // 注册retransform模式的Transformer，canRetransform=true表示可以重定义已加载的类
        instrumentation.addTransformer(classFileTransformer, true);

        // 创建懒加载模式的聚合Transformer
        // 这个Transformer会调用lazyTransformers，只在类首次加载时工作
        // 懒加载 transformer，用于在类首次加载时增强
        // 注意：必须用 addTransformer(transformer, false) 才能在类首次定义时被调用
        lazyClassFileTransformer = new ClassFileTransformer() {

            /**
             * 转换类文件（仅首次加载）
             *
             * <p>此方法只在类首次定义时被调用，用于对类进行增强。
             * 如果类已经被加载过（classBeingRedefined != null），则直接返回null，不做处理。</p>
             *
             * @param loader 定义要转换的类的加载器；如果是引导加载器，则为 null
             * @param className 完全限定类名
             * @param classBeingRedefined 如果是被重定义或重转换的类，则为该类；如果是首次加载，则为 null
             * @param protectionDomain 类的定义域
             * @param classfileBuffer 类文件的输入字节码缓冲区
             * @return 转换后的类文件字节码缓冲区；如果没有转换，则返回null
             * @throws IllegalClassFormatException 如果类文件格式不合法
             */
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                // 只处理类首次加载的情况（classBeingRedefined == null）
                // 如果是类重定义或重转换，则直接返回null，不做处理
                if (classBeingRedefined != null) {
                    return null;
                }

                // 应用lazyTransformers进行类增强
                for (ClassFileTransformer transformer : lazyTransformers) {
                    byte[] transformResult = transformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    // 如果Transformer返回了新字节码，则更新缓冲区
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                // 返回最终的转换结果
                return classfileBuffer;
            }

        };
        // 使用 false 参数，这样才会在类首次定义时被调用
        // canRetransform=false表示此Transformer不会用于类重定义，只在类首次加载时调用
        instrumentation.addTransformer(lazyClassFileTransformer, false);
    }

    /**
     * 添加watch或trace类型的Transformer
     *
     * <p>根据isTracing参数，将Transformer添加到对应的列表中。</p>
     *
     * <p><b>分类逻辑：</b></p>
     * <ul>
     *   <li>isTracing=true：添加到traceTransformers（用于trace命令）</li>
     *   <li>isTracing=false：添加到watchTransformers（用于watch命令）</li>
     * </ul>
     *
     * @param transformer 要添加的ClassFileTransformer
     * @param isTracing 是否为trace类型的Transformer
     */
    public void addTransformer(ClassFileTransformer transformer, boolean isTracing) {
        if (isTracing) {
            // 添加到trace命令的Transformer列表
            traceTransformers.add(transformer);
        } else {
            // 添加到watch命令的Transformer列表
            watchTransformers.add(transformer);
        }
    }

    /**
     * 添加懒加载模式的Transformer
     *
     * <p>此方法用于添加在类首次加载时进行增强的Transformer。
     * 这些Transformer不会对已加载的类生效，只对之后新加载的类生效。</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>需要在类加载时就进行增强的场景</li>
     *   <li>避免对已加载类进行重定义的性能开销</li>
     *   <li>某些无法通过重定义实现的增强场景</li>
     * </ul>
     *
     * @param transformer 要添加的ClassFileTransformer，用于类首次加载时的增强
     */
    public void addLazyTransformer(ClassFileTransformer transformer) {
        lazyTransformers.add(transformer);
    }

    /**
     * 添加retransform类型的Transformer
     *
     * <p>此方法用于添加需要在类重定义时优先执行的Transformer。
     * 这些Transformer会在watch和trace类型的Transformer之前执行。</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>需要在其他增强之前进行的处理</li>
     *   <li>类重定义时的预处理</li>
     *   <li>某些特殊的增强需求</li>
     * </ul>
     *
     * @param transformer 要添加的ClassFileTransformer，用于类重定义场景
     */
    public void addRetransformer(ClassFileTransformer transformer) {
        reTransformers.add(transformer);
    }

    /**
     * 移除指定的Transformer
     *
     * <p>此方法会从所有Transformer列表中移除指定的Transformer。</p>
     *
     * <p><b>移除范围：</b></p>
     * <ul>
     *   <li>reTransformers</li>
     *   <li>watchTransformers</li>
     *   <li>traceTransformers</li>
     *   <li>lazyTransformers</li>
     * </ul>
     *
     * <p><b>线程安全：</b></p>
     * <p>使用CopyOnWriteArrayList，移除操作是线程安全的。</p>
     *
     * @param transformer 要移除的ClassFileTransformer
     */
    public void removeTransformer(ClassFileTransformer transformer) {
        // 从所有列表中移除此Transformer
        reTransformers.remove(transformer);
        watchTransformers.remove(transformer);
        traceTransformers.remove(transformer);
        lazyTransformers.remove(transformer);
    }

    /**
     * 销毁TransformerManager，清理所有资源
     *
     * <p>此方法会清空所有Transformer列表，并从Instrumentation中注销聚合的Transformer。</p>
     *
     * <p><b>清理流程：</b></p>
     * <ol>
     *   <li>清空所有Transformer列表</li>
     *   <li>从Instrumentation中注销retransform模式的Transformer</li>
     *   <li>从Instrumentation中注销懒加载模式的Transformer</li>
     * </ol>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>Arthas客户端断开连接时</li>
     *   <li>所有增强命令都结束时</li>
     *   <li>需要释放资源时</li>
     * </ul>
     *
     * <p><b>注意事项：</b></p>
     * <p>调用此方法后，所有已增强的类会保持增强状态，直到类被重新加载。
     * 但不会再有新的增强被应用。</p>
     */
    public void destroy() {
        // 清空所有Transformer列表
        reTransformers.clear();
        watchTransformers.clear();
        traceTransformers.clear();
        lazyTransformers.clear();

        // 从Instrumentation中注销聚合的Transformer
        instrumentation.removeTransformer(classFileTransformer);
        instrumentation.removeTransformer(lazyClassFileTransformer);
    }

}
