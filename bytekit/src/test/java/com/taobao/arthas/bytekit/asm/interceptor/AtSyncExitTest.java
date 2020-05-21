package com.taobao.arthas.bytekit.asm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtSyncExit;
import com.taobao.arthas.bytekit.utils.Decompiler;

public class AtSyncExitTest {
    @Rule
    public OutputCapture capture = new OutputCapture();
    
    static class Sample {
        
        public int testLine(int i) {
            String s = "" + i;
            synchronized (s) {
                if(i > 0) {
                    String abc = s + i;
                    i++;
                    i = i * 100 
                            + i 
                            - 100 + Math.max(100, i);
                    i += s.length() + abc.length();
                }else {
                    if(i == -1) {
                        try {
                            System.err.println("i is -1");
                            throw new RuntimeException();
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                        }
                        
                    }
                }
            }
            
            return i * 2;
        }
        
    }
    
    public static class TestInterceptor {
        
        @AtSyncExit(whenComplete=false, inline = false)
        public static void atSyncExit(
                @Binding.This Object object,
                @Binding.Class Object clazz
                ,
                @Binding.Args Object[] args
                ,
                @Binding.ArgNames String[] argNames
                ,
                @Binding.LocalVars Object[] vars,
                @Binding.LocalVarNames String[] varNames
                ,
                @Binding.Monitor Object monitor
                ) {
            System.err.println("atSyncExit: this" + object);
            System.err.println("args: " + Arrays.toString(args));
            System.err.println("argNames: " + Arrays.toString(argNames));
            
            System.err.println("vars: " + Arrays.toString(vars));
            System.err.println("varNames: " + Arrays.toString(varNames));
            
        }
    }
    
    @Test
    public void test() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(TestInterceptor.class).methodMatcher("*")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        new Sample().testLine(100);

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("atSyncExit: this");
    }


}
