package com.taobao.arthas.core.bytecode;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.zeroturnaround.zip.ZipUtil;

import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.bytekit.utils.AgentUtils;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.MatchUtils;
import com.alibaba.bytekit.utils.VerifyUtils;

/**
 * 
 * @author hengyunabc 2020-05-19
 *
 */
public class TestHelper {

    private Class<?> interceptorClass;

    private boolean redefine;

    private String methodMatcher = "*";

    private boolean asmVerity = true;

    public static TestHelper builder() {
        return new TestHelper();
    }

    public TestHelper interceptorClass(Class<?> interceptorClass) {
        this.interceptorClass = interceptorClass;
        return this;
    }

    public TestHelper redefine(boolean redefine) {
        this.redefine = redefine;
        return this;
    }

    public TestHelper methodMatcher(String methodMatcher) {
        this.methodMatcher = methodMatcher;
        return this;
    }

    public byte[] process(Class<?> transform) throws Exception {
        DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

        List<InterceptorProcessor> interceptorProcessors = defaultInterceptorClassParser.parse(interceptorClass);

        ClassNode classNode = AsmUtils.loadClass(transform);

        List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
        for (MethodNode methodNode : classNode.methods) {
            if (MatchUtils.wildcardMatch(methodNode.name, methodMatcher)) {
                matchedMethods.add(methodNode);
            }
        }

        for (MethodNode methodNode : matchedMethods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
            for (InterceptorProcessor interceptor : interceptorProcessors) {
                interceptor.process(methodProcessor);
            }
        }

        byte[] bytes = AsmUtils.toBytes(classNode);
        if (asmVerity) {
            VerifyUtils.asmVerify(bytes);
        }

        if (redefine) {
            AgentUtils.redefine(transform, bytes);
        }

        return bytes;
    }
    
    public static void appendSpyJar(Instrumentation instrumentation) throws IOException {
        // find spy target/classes directory
        String file = TestHelper.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        
        File spyClassDir = new File(file, "../../../spy/target/classes").getAbsoluteFile();
        
        File destJarFile = new File(file, "../../../spy/target/test-spy.jar").getAbsoluteFile();
        
        ZipUtil.pack(spyClassDir, destJarFile);
        
        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(destJarFile));
        
    }
}
