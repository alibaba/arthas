package com.taobao.arthas.core.command.klass100;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.taobao.arthas.compiler.DynamicCompiler;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.logger.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import static com.taobao.arthas.core.util.StringUtils.normalizeClassName;
import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Type.getType;
import static org.objectweb.asm.commons.Method.getMethod;

@Name("rs")
@Summary("Run Script")
@Description(Constants.WIKI + Constants.WIKI_HOME + "rs")
public class RunScriptCommand extends AnnotatedCommand {

    private static final Logger logger = LogUtil.getArthasLogger();

    private static final String HOLDER_CLASS = "rs/$Holder";

    private String hashCode;

    private List<String> sourcefiles;

    private String encoding;

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special ClassLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Argument(argName = "sourcefiles", index = 0)
    @Description("source files")
    public void setClassPattern(List<String> sourcefiles) {
        this.sourcefiles = sourcefiles;
    }

    @Option(longName = "encoding")
    @Description("Source file encoding")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public void process(CommandProcess process) {
        Instrumentation inst = process.session().getInstrumentation();

        int exitCode = 0;

        try {
            // 1\ get classloader
            ClassLoader classloader;
            if (hashCode == null) {
                classloader = ClassLoader.getSystemClassLoader();
            } else {
                classloader = ClassLoaderUtils.getClassLoader(inst, hashCode);
                if (classloader == null) {
                    process.write("Can not find classloader with hashCode: " + hashCode + ".\n");
                    exitCode = -1;
                    return;
                }
            }

            // 2\ compile
            process.write(">>>>>>" + DateUtils.getCurrentDate() + ": compile begin.\n");
            final Map<String, byte[]> bytesMap = getDynamicCompiler(classloader).buildByteCodes();
            process.write(">>>>>>" + DateUtils.getCurrentDate() + ": compile end.\n");

            ClassLoader runClassloader = new ClassLoader(classloader) {

                @Override
                public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    final Class<?> loadedClass = super.findLoadedClass(name);
                    if (loadedClass != null) {
                        return loadedClass;
                    }

                    byte[] bytes = bytesMap.get(name);
                    if (bytes != null) {
                        return super.defineClass(name, bytes, 0, bytes.length);
                    }
                    return super.loadClass(name, resolve);
                }
            };

            // 3\ enhance
            process.write(">>>>>>" + DateUtils.getCurrentDate() + ": enhance begin.\n");
            enhance(bytesMap, runClassloader);
            process.write(">>>>>>" + DateUtils.getCurrentDate() + ": enhance end.\n");

            // 4\ init & run
            process.write(">>>>>>" + DateUtils.getCurrentDate() + ": runMain begin.\n");
            runMain(process, runClassloader, bytesMap);
            process.write(">>>>>>" + DateUtils.getCurrentDate() + ": runMain end.\n");
        } catch (Throwable e) {
            logger.warn("error", e);
            process.write("error, exception message: " + e.getMessage()
                    + ", please check $HOME/logs/arthas/arthas.log for more details.\n");
            exitCode = -1;
        } finally {
            process.end(exitCode);
        }
    }

    private void enhance(Map<String, byte[]> byteCodes, final ClassLoader runClassloader) {
        for (Entry<String, byte[]> entry : byteCodes.entrySet()) {
            ClassReader classReader = new ClassReader(entry.getValue());
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(ASM7, cw) {

                @Override
                public MethodVisitor visitMethod(int access, String name, final String desc, String signature,
                                                 String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    Class holderClazz;
                    try {
                        holderClazz = runClassloader.loadClass(normalizeClassName(HOLDER_CLASS));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("[ERROR] holder-class not found", e);
                    }
                    return new MethodEnhancer(holderClazz, access, name, desc, signature, exceptions, mv);
                }
            };
            classReader.accept(cv, ClassReader.EXPAND_FRAMES);
            entry.setValue(cw.toByteArray());
        }
    }

    static class MethodEnhancer extends AdviceAdapter {

        private Class holderClazz;

        MethodEnhancer(Class holderClazz, int access, String name, String desc, String signature, String[] exceptions,
                       MethodVisitor mv) {
            super(ASM7, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions), access, name, desc);
            this.holderClazz = holderClazz;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            // 1\ System.exit not allowed
            if (opcode == INVOKESTATIC && "java/lang/System".equals(owner) && "exit".equals(name)) {
                throw new RuntimeException("System.exit not allowed");
            }
            // 2\ copy out.print | err.print 's content，and print to console
            String print = null;
            if (opcode == INVOKEVIRTUAL && "java/io/PrintStream".equals(owner)) {
                if ("print".equals(name)) {
                    print = "print";
                }
                if ("println".equals(name)) {
                    print = "println";
                }
            }
            if (print != null) {
                // 0\ store str
                this.convertByValueOf(descriptor);
                int localVarId = super.newLocal(getType(String.class));
                super.storeLocal(localVarId);

                super.loadLocal(localVarId);
                super.invokeVirtual(getType(PrintStream.class), getMethod("void " + print + "(String)"));

                // 1\ method: printMethod
                super.getStatic(getType(this.holderClazz), print + "Method", getType(Method.class));

                // 2\ arg1: null
                super.push((Type)null);

                // 3\ arg2: create array
                super.push(1);
                super.newArray(getType(Object.class));
                super.dup();

                // 4\ arg2: store str into array
                super.push(0);
                super.loadLocal(localVarId);
                super.arrayStore(getType(String.class));

                // 5\ invoke
                super.invokeVirtual(getType(Method.class), getMethod("Object invoke(Object,Object[])"));
                super.pop();
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }

        private void convertByValueOf(String descriptor) {
            if (descriptor.equals("()V")) {
                // out.println()
                super.push("");
            } else if (descriptor.equals("([C)V")) {
                super.invokeStatic(getType(String.class), getMethod("String valueOf(char[])"));
            } else if (descriptor.equals("(Z)V")) {
                super.invokeStatic(getType(String.class), getMethod("String valueOf(boolean)"));
            } else if (descriptor.equals("(C)V")) {
                super.invokeStatic(getType(String.class), getMethod("String valueOf(char)"));
            } else if (descriptor.equals("(D)V")) {
                super.invokeStatic(getType(String.class), getMethod("String valueOf(double)"));
            } else if (descriptor.equals("(F)V")) {
                super.invokeStatic(getType(String.class), getMethod("String valueOf(float)"));
            } else if (descriptor.equals("(I)V")) {
                super.invokeStatic(getType(String.class), getMethod("String valueOf(int)"));
            } else if (descriptor.equals("(J)V")) {
                super.invokeStatic(getType(String.class), getMethod("String valueOf(long)"));
            } else if (descriptor.equals("(Ljava/lang/Object;)V")) {
                super.invokeStatic(getType(String.class), getMethod("String valueOf(Object)"));
            } else if (descriptor.equals("(Ljava/lang/String;)V")) {
                // to nothing
            }
        }
    }

    private void runMain(CommandProcess process, ClassLoader runClassloader, Map<String, byte[]> bytesMap)
            throws Exception {
        // 1\ init
        CommandPrint.process = process;
        Class<?> holderClazz = Class.forName(normalizeClassName(HOLDER_CLASS), true, runClassloader);
        holderClazz.getDeclaredField("printMethod").set(null, CommandPrint.class.getDeclaredMethod("print", String.class));
        holderClazz.getDeclaredField("printlnMethod").set(null, CommandPrint.class.getDeclaredMethod("println", String.class));

        // 2\ run first main
        for (String aClass : bytesMap.keySet()) {
            Class<?> clazz = Class.forName(aClass, true, runClassloader);
            Method meth = null;
            try {
                meth = clazz.getMethod("main", String[].class);
            } catch (Exception ignore) {
            }
            if (meth != null) {
                process.write(">>>>>>" + DateUtils.getCurrentDate() + ": run " + clazz + ".\n");
                meth.invoke(null, new String[1]);
                return;
            }
        }
        process.write(">>>>>>" + DateUtils.getCurrentDate() + ": [ERROR] no main method found" + ".\n");
    }

    private DynamicCompiler getDynamicCompiler(ClassLoader classloader) throws IOException {
        DynamicCompiler dynamicCompiler = new DynamicCompiler(classloader);
        Charset charset = Charset.defaultCharset();
        if (encoding != null) {
            charset = Charset.forName(encoding);
        }

        // 1\ add java source
        for (String sourceFile : sourcefiles) {
            String sourceCode = FileUtils.readFileToString(new File(sourceFile), charset);
            String name = new File(sourceFile).getName();
            if (name.endsWith(".java")) {
                name = name.substring(0, name.length() - ".java".length());
            }
            dynamicCompiler.addSource(name, sourceCode);
        }

        // 2\ add holder class
        dynamicCompiler.addSource(normalizeClassName(HOLDER_CLASS), ""
            + "package rs;\n"
            + "import java.lang.reflect.Method;\n"
            + "public class $Holder {\n"
            + "    public static Method printMethod;\n"
            + "    public static Method printlnMethod;\n"
            + "}");
        return dynamicCompiler;
    }

    public static class CommandPrint {

        public static CommandProcess process;

        public static void print(String s) {
            process.write(s);
        }

        public static void println(String s) {
            process.write(s + "\n");
        }
    }
}
