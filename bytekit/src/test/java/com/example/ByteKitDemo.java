package com.example;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtEnter;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExit;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.taobao.arthas.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.taobao.arthas.bytekit.utils.AgentUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;
import com.taobao.arthas.bytekit.utils.Decompiler;

/**
 * 
 * @author hengyunabc 2020-05-21
 *
 */
public class ByteKitDemo {

    public static class Sample {
        private int exceptionCount = 0;

        public String hello(String str, boolean exception) {
            if (exception) {
                exceptionCount++;
                throw new RuntimeException("test exception, str: " + str);
            }
            return "hello " + str;
        }
    }

    public static class PrintExceptionSuppressHandler {

        @ExceptionHandler(inline = true)
        public static void onSuppress(@Binding.Throwable Throwable e, @Binding.Class Object clazz) {
            System.out.println("exception handler: " + clazz);
            e.printStackTrace();
        }
    }

    public static class SampleInterceptor {

        @AtEnter(inline = true, suppress = RuntimeException.class, suppressHandler = PrintExceptionSuppressHandler.class)
        public static void atEnter(@Binding.This Object object, 
                @Binding.Class Object clazz,
                @Binding.Args Object[] args, 
                @Binding.MethodName String methodName,
                @Binding.MethodDesc String methodDesc) {
            System.out.println("atEnter, args[0]: " + args[0]);
        }

        @AtExit(inline = true)
        public static void atExit(@Binding.Return Object returnObject) {
            System.out.println("atExit, returnObject: " + returnObject);
        }

        @AtExceptionExit(inline = true, onException = RuntimeException.class)
        public static void atExceptionExit(@Binding.Throwable RuntimeException ex,
                @Binding.Field(name = "exceptionCount") int exceptionCount) {
            System.out.println("atExceptionExit, ex: " + ex.getMessage() + ", field exceptionCount: " + exceptionCount);
        }
    }

    public static void main(String[] args) throws Exception {
        AgentUtils.install();

        // 启动Sample，不断执行
        final Sample sample = new Sample();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; ++i) {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                        String result = sample.hello("" + i, (i % 3) == 0);
                        System.out.println("call hello result: " + result);
                    } catch (Throwable e) {
                        // ignore
                        System.out.println("call hello exception: " + e.getMessage());
                    }
                }
            }
        });
        t.start();

        // 解析定义的 Interceptor类 和相关的注解
        DefaultInterceptorClassParser interceptorClassParser = new DefaultInterceptorClassParser();
        List<InterceptorProcessor> processors = interceptorClassParser.parse(SampleInterceptor.class);

        // 加载字节码
        ClassNode classNode = AsmUtils.loadClass(Sample.class);

        // 对加载到的字节码做增强处理
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("hello")) {
                MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
                for (InterceptorProcessor interceptor : processors) {
                    interceptor.process(methodProcessor);
                }
            }
        }

        // 获取增强后的字节码
        byte[] bytes = AsmUtils.toBytes(classNode);

        // 查看反编译结果
        System.out.println(Decompiler.decompile(bytes));

        // 等待，查看未增强里的输出结果
        TimeUnit.SECONDS.sleep(10);

        // 通过 reTransform 增强类
        AgentUtils.reTransform(Sample.class, bytes);
        System.in.read();
    }

}
