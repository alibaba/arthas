package com.taobao.arthas.bytekit.asm.binding;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;

import com.taobao.arthas.bytekit.asm.binding.annotation.BindingParser;
import com.taobao.arthas.bytekit.asm.binding.annotation.BindingParserHandler;


public abstract class Binding {

    /**
     * 是否可选的，当不符合条件，或者获取不到值时，会转为 null，这个不支持原始类型，就像java.util.Optional 一样？
     * @return
     */
    public boolean optional() {
        return false;
    }
    
    /**
     * 检查当前条件下这个binding是否可以工作，比如检查field是否有这个field。
     * @return
     */
    public boolean check(BindingContext bindingContext) {
        return true;
    }

    /**
     * 把这个binding本身放到栈上
     * @param instructions
     * @param bindingContext
     */
    public abstract void pushOntoStack(InsnList instructions, BindingContext bindingContext);

    public abstract Type getType( BindingContext bindingContext);
    
    public boolean fromStack() {
        return false;
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = ArgsBindingParser.class)
    public @interface Args {
        
        boolean optional() default false;

    }
    public static class ArgsBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new ArgsBinding();
        }
        
    }
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = ArgNamesBindingParser.class)
    public @interface ArgNames {
        
        boolean optional() default false;

    }
    public static class ArgNamesBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new ArgNamesBinding();
        }
        
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = LocalVarsBindingParser.class)
    public @interface LocalVars {
        
        boolean optional() default false;

    }
    public static class LocalVarsBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new LocalVarsBinding();
        }
        
    }
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = LocalVarNamesBindingParser.class)
    public @interface LocalVarNames {
        
        boolean optional() default false;

    }
    public static class LocalVarNamesBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new LocalVarNamesBinding();
        }
        
    }
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = ClassBindingParser.class)
    public @interface Class {
        
        boolean optional() default false;

    }
    
    public static class ClassBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new ClassBinding();
        }
        
    }
    

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = FieldBindingParser.class)
    public @interface Field {
        boolean optional() default false;
        java.lang.Class<?> owner() default Void.class;
        java.lang.Class<?> type() default Void.class;
        String name();
        boolean isStatic() default false;
        boolean box() default false;
    }

    public static class FieldBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            Field field = (Field) annotation;
            Type ownerType = Type.getType(field.owner());
            if(field.owner().equals(Void.class)) {
                ownerType = null;
            }
            Type fieldType = Type.getType(field.type());
            if(field.type().equals(Void.class)) {
                fieldType = null;
            }
            return new FieldBinding(ownerType, field.name(), fieldType,
                    field.isStatic(), field.box());
        }
    }
    
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = InvokeArgsBindingParser.class)
    public @interface InvokeArgs {
        
        boolean optional() default false;

    }
    
    public static class InvokeArgsBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new InvokeArgsBinding();
        }
    }
    
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = InvokeReturnBindingParser.class)
    public @interface InvokeReturn {
        
        boolean optional() default false;

    }
    
    public static class InvokeReturnBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new InvokeReturnBinding();
        }
    }
    
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = InvokeMethodDeclarationBindingParser.class)
    public @interface InvokeMethodDeclaration {
        
        boolean optional() default false;

    }
    
    public static class InvokeMethodDeclarationBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new InvokeMethodDeclarationBinding();
        }
    }
    

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = MethodBindingParser.class)
    public @interface Method {
        boolean optional() default false;
    }
    
    public static class MethodBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new MethodBinding();
        }
        
    }
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = ReturnBindingParser.class)
    public @interface Return {
        
        boolean optional() default false;

    }
    
    public static class ReturnBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new ReturnBinding();
        }
        
    }
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = ThisBindingParser.class)
    public @interface This {

    }
    
    public static class ThisBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new ThisBinding();
        }
        
    }
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = ThrowableBindingParser.class)
    public @interface Throwable {
        
        boolean optional() default false;

    }
    
    public static class ThrowableBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new ThrowableBinding();
        }
        
    }
    
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = LineBindingParser.class)
    public @interface Line {
        
        boolean optional() default false;

    }
    
    public static class LineBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new LineBinding();
        }
        
    }
    
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    @BindingParserHandler(parser = MonitorBindingParser.class)
    public @interface Monitor {
        
        boolean optional() default false;

    }
    
    public static class MonitorBindingParser implements BindingParser {
        @Override
        public Binding parse(Annotation annotation) {
            return new MonitorBinding();
        }
    }
}
