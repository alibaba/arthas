package com.taobao.arthas.core.command.klass100;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * Redefine Classes.
 *
 * @author hengyunabc 2018-07-13
 * @see java.lang.instrument.Instrumentation#redefineClasses(ClassDefinition...)
 */
@Name("redefine")
@Summary("Redefine classes. @see Instrumentation#redefineClasses(ClassDefinition...)")
@Description(Constants.EXAMPLE +
                "  redefine -p /tmp/Test.class\n" +
                "  redefine -c 327a647b -p /tmp/Test.class /tmp/Test\\$Inner.class \n" +
                Constants.WIKI + Constants.WIKI_HOME + "redefine")
public class RedefineCommand extends AnnotatedCommand {

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;

    private String hashCode;

    private List<String> paths;

    @Option(shortName = "c", longName = "classloader")
    @Description("classLoader hashcode")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(shortName = "p", longName = "path", acceptMultipleValues = true)
    @Description(".class file paths")
    public void setPathPatterns(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public void process(CommandProcess process) {
        if (paths == null || paths.isEmpty()) {
            process.write("paths is empty.\n");
            process.end();
            return;
        }
        Instrumentation inst = process.session().getInstrumentation();

        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                process.write("file does not exist, path:" + path + "\n");
                process.end();
                return;
            }
            if (!file.isFile()) {
                process.write("not a normal file, path:" + path + "\n");
                process.end();
                return;
            }
            if (file.length() >= MAX_FILE_SIZE) {
                process.write("file size: " + file.length() + " >= " + MAX_FILE_SIZE + ", path: " + path + "\n");
                process.end();
                return;
            }
        }

        Map<String, byte[]> bytesMap = new HashMap<String, byte[]>();
        for (String path : paths) {
            RandomAccessFile f = null;
            try {
                f = new RandomAccessFile(path, "r");
                final byte[] bytes = new byte[(int) f.length()];
                f.readFully(bytes);

                final String clazzName = readClassName(bytes);

                bytesMap.put(clazzName, bytes);

            } catch (Exception e) {
                process.write("" + e + "\n");
            } finally {
                if (f != null) {
                    try {
                        f.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }

        if (bytesMap.size() != paths.size()) {
            process.write("paths may contains same class name!\n");
            process.end();
            return;
        }

        List<ClassDefinition> definitions = new ArrayList<ClassDefinition>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (bytesMap.containsKey(clazz.getName())) {
                if (hashCode != null && !Integer.toHexString(clazz.getClassLoader().hashCode()).equals(hashCode)) {
                    continue;
                }
                definitions.add(new ClassDefinition(clazz, bytesMap.get(clazz.getName())));
            }
        }

        try {
            inst.redefineClasses(definitions.toArray(new ClassDefinition[0]));
            process.write("redefine success, size: " + definitions.size() + "\n");
        } catch (Exception e) {
            process.write("redefine error! " + e + "\n");
        }

        process.end();
    }

    private static String readClassName(final byte[] bytes) {
        return new ClassReader(bytes).getClassName().replace("/", ".");
    }
}
