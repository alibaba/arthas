package com.taobao.arthas.bytekit.asm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtLine;
import com.taobao.arthas.bytekit.utils.Decompiler;

public class AtLineTest {
    @Rule
    public OutputCapture capture = new OutputCapture();
    
    static class Sample {
        
        public int testLine(int i) {
            String s = "" + i;
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
            return i * 2;
        }
        
    }
    
    public static class TestAccessInterceptor {
        
        @AtLine(lines = { -1}, inline = false)
        public static void atLine(
                @Binding.This Object object,
                @Binding.Class Object clazz
                ,
                @Binding.Line int line,
                @Binding.Args Object[] args
                ,
                @Binding.ArgNames String[] argNames
                ,
                @Binding.LocalVars Object[] vars,
                @Binding.LocalVarNames String[] varNames
                ) {
            System.err.println("atLine: this" + object);
            System.err.println("line: " + line);
            System.err.println("args: " + Arrays.toString(args));
            System.err.println("argNames: " + Arrays.toString(argNames));
            
            System.err.println("vars: " + Arrays.toString(vars));
            System.err.println("varNames: " + Arrays.toString(varNames));
        }
    }
    
    @Test
    public void testLine() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(TestAccessInterceptor.class).methodMatcher("*")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        new Sample().testLine(100);

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("atLine: this");
    }


}
