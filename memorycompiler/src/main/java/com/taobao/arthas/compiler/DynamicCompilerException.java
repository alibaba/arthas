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

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.*;

/**
 * 动态编译器异常类
 *
 * <p>该类用于封装在动态编译Java源代码过程中发生的异常信息。
 * 继承自RuntimeException，支持携带编译器的诊断信息（Diagnostic），
 * 能够提供详细的编译错误信息，包括错误行号和具体错误消息。</p>
 *
 * <p>主要特性：
 * <ul>
 *   <li>保存编译器产生的诊断信息列表</li>
 *   <li>将诊断信息格式化为易读的错误消息</li>
 *   <li>支持通过消息或异常原因创建异常对象</li>
 * </ul>
 * </p>
 */
public class DynamicCompilerException extends RuntimeException {

    /**
     * 序列化版本UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * 编译器诊断信息列表
     *
     * <p>存储编译过程中产生的所有诊断信息，包括错误、警告等。
     * 每个Diagnostic对象包含编译错误的详细信息，如：
     * - 错误发生的行号
     * - 错误消息
     * - 错误级别（错误、警告等）
     * </p>
     */
    private List<Diagnostic<? extends JavaFileObject>> diagnostics;

    /**
     * 构造函数 - 通过错误消息创建异常
     *
     * @param message 错误消息，描述编译失败的原因
     * @param diagnostics 编译器诊断信息列表，包含详细的编译错误信息
     */
    public DynamicCompilerException(String message, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        super(message);
        this.diagnostics = diagnostics;
    }

    /**
     * 构造函数 - 通过异常原因创建异常
     *
     * @param cause 导致编译失败的底层异常
     * @param diagnostics 编译器诊断信息列表，包含详细的编译错误信息
     */
    public DynamicCompilerException(Throwable cause, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        super(cause);
        this.diagnostics = diagnostics;
    }

    /**
     * 获取格式化后的错误列表
     *
     * <p>将诊断信息转换为Map列表，每个Map包含：
     * <ul>
     *   <li>"line": 错误行号</li>
     *   <li>"message": 错误消息</li>
     * </ul>
     * </p>
     *
     * @return 错误信息列表，每个元素是一个包含行号和消息的Map
     */
    private List<Map<String, Object>> getErrorList() {
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        // 检查诊断信息是否存在
        if (diagnostics != null) {
            // 遍历所有诊断信息
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
                Map<String, Object> message = new HashMap<String, Object>(2);
                // 提取错误行号
                message.put("line", diagnostic.getLineNumber());
                // 提取错误消息（使用美国英语locale）
                message.put("message", diagnostic.getMessage(Locale.US));
                messages.add(message);
            }

        }
        return messages;
    }

    /**
     * 获取格式化的错误字符串
     *
     * <p>将所有错误信息格式化为易读的字符串形式，每行一个错误。
     * 格式为：key: value , key: value
     * </p>
     *
     * @return 格式化后的错误字符串，每个错误占一行
     */
    private String getErrors() {
        StringBuilder errors = new StringBuilder();

        // 遍历错误列表
        for (Map<String, Object> message : getErrorList()) {
            // 遍历每个错误的字段
            for (Map.Entry<String, Object> entry : message.entrySet()) {
                Object value = entry.getValue();
                // 只处理非空值
                if (value != null && !value.toString().isEmpty()) {
                    errors.append(entry.getKey());
                    errors.append(": ");
                    errors.append(value);
                }
                errors.append(" , ");
            }

            // 每个错误后换行
            errors.append("\n");
        }

        return errors.toString();

    }

    /**
     * 获取完整的异常消息
     *
     * <p>重写父类方法，在原始消息后附加详细的编译错误信息。
     * 消息格式为：原始消息 + 换行符 + 格式化的错误详情
     * </p>
     *
     * @return 包含原始消息和详细错误信息的完整异常消息
     */
    @Override
    public String getMessage() {
        return super.getMessage() + "\n" + getErrors();
    }

}
