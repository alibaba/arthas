package com.taobao.arthas.compiler;

/*-
 * #%L
 * compiler
 * %%
 * Copyright (C) 2017 - 2018 SkaLogs
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 动态类加载器
 *
 * <p>继承自ClassLoader，用于在运行时动态加载从内存中编译的类。
 * 该类加载器是动态编译系统的核心组件，负责管理编译后的字节码并将其定义为可用的Java类。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>注册编译后的字节码</li>
 *   <li>从内存中加载类</li>
 *   <li>提供对所有已加载类的访问</li>
 *   <li>提供对所有字节码的访问</li>
 * </ul>
 *
 * <p>工作原理：</p>
 * <ol>
 *   <li>编译器生成类的字节码</li>
 *   <li>字节码通过registerCompiledSource方法注册到此加载器</li>
 *   <li>当需要使用类时，通过findClass方法从内存中定义并加载</li>
 *   <li>如果类不在内存中，则委托给父类加载器</li>
 * </ol>
 *
 * @author arthas
 * @since 2017-2018
 */
public class DynamicClassLoader extends ClassLoader {

    /**
     * 存储编译后的字节码的映射表
     * key: 类的全限定名
     * value: 对应的内存字节码对象
     */
    private final Map<String, MemoryByteCode> byteCodes = new HashMap<String, MemoryByteCode>();

    /**
     * 构造函数
     *
     * @param classLoader 父类加载器，用于双亲委派模型
     */
    public DynamicClassLoader(ClassLoader classLoader) {
        // 调用父类构造函数，设置父类加载器
        super(classLoader);
    }

    /**
     * 注册编译后的源代码
     *
     * <p>将编译后的字节码对象注册到类加载器中，
     * 这样后续就可以通过类名来加载这个类。</p>
     *
     * @param byteCode 包含类名和字节码的内存字节码对象
     */
    public void registerCompiledSource(MemoryByteCode byteCode) {
        // 将字节码对象存入映射表，以类名作为key
        byteCodes.put(byteCode.getClassName(), byteCode);
    }

    /**
     * 查找并加载类
     *
     * <p>重写父类的findClass方法，实现从内存中加载类的逻辑。
     * 首先在内存中查找字节码，如果找到则定义类；否则委托给父类加载器。</p>
     *
     * @param name 类的全限定名
     * @return 加载的Class对象
     * @throws ClassNotFoundException 如果类未找到
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 从内存中查找该类的字节码
        MemoryByteCode byteCode = byteCodes.get(name);

        // 如果在内存中找不到该类的字节码
        if (byteCode == null) {
            // 委托给父类加载器去查找
            return super.findClass(name);
        }

        // 在内存中找到了字节码，使用defineClass方法将其定义为Class对象
        // 参数说明：
        // - name: 类的全限定名
        // - byteCode.getByteCode(): 字节码数据
        // - 0: 字节码数组的起始偏移量
        // - byteCode.getByteCode().length: 要读取的字节码长度
        return super.defineClass(name, byteCode.getByteCode(), 0, byteCode.getByteCode().length);
    }

    /**
     * 获取所有已加载的类
     *
     * <p>遍历所有注册的字节码，将其加载为Class对象并返回。
     * 如果某个类尚未加载，此方法会触发类的加载过程。</p>
     *
     * @return 类名到Class对象的映射表
     * @throws ClassNotFoundException 如果某个类加载失败
     */
    public Map<String, Class<?>> getClasses() throws ClassNotFoundException {
        // 创建结果映射表
        Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

        // 遍历所有已注册的字节码
        for (MemoryByteCode byteCode : byteCodes.values()) {
            // 将字节码加载为Class对象并存入结果映射表
            // findClass方法会触发类的定义和加载
            classes.put(byteCode.getClassName(), findClass(byteCode.getClassName()));
        }

        return classes;
    }

    /**
     * 获取所有字节码
     *
     * <p>返回所有已注册类的原始字节码数据。
     * 这些字节码可以用于持久化、传输或其他目的。</p>
     *
     * @return 类名到字节码数组的映射表
     */
    public Map<String, byte[]> getByteCodes() {
        // 创建结果映射表，设置初始大小以优化性能
        Map<String, byte[]> result = new HashMap<String, byte[]>(byteCodes.size());

        // 遍历所有已注册的字节码
        for (Entry<String, MemoryByteCode> entry : byteCodes.entrySet()) {
            // 提取字节数组并存入结果映射表
            result.put(entry.getKey(), entry.getValue().getByteCode());
        }

        return result;
    }
}
