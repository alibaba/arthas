package com.taobao.arthas.core.command.klass100;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.taobao.arthas.core.advisor.TransformerManager;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.RetransformModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 
 * Retransform Classes.
 *
 * @author hengyunabc 2021-01-05
 * @see java.lang.instrument.Instrumentation#retransformClasses(Class...)
 */
@Name("retransform")
@Summary("Retransform classes. @see Instrumentation#retransformClasses(Class...)")
@Description(Constants.EXAMPLE + "  retransform /tmp/Test.class\n"
        + "  retransform -l \n"
        + "  retransform -d 1                    # delete retransform entry\n"
        + "  retransform --deleteAll             # delete all retransform entries\n"
        + "  retransform --classPattern demo.*   # triger retransform classes\n"
        + "  retransform -c 327a647b /tmp/Test.class /tmp/Test\\$Inner.class \n"
        + "  retransform --classLoaderClass 'sun.misc.Launcher$AppClassLoader' /tmp/Test.class\n"
        + Constants.WIKI + Constants.WIKI_HOME
        + "retransform")
public class RetransformCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(RetransformCommand.class);
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static volatile List<RetransformEntry> retransformEntries = new ArrayList<RetransformEntry>();
    private static volatile ClassFileTransformer transformer = null;
    
    private String hashCode;
    private String classLoaderClass;

    private List<String> paths;

    private boolean list;

    private int delete = -1;

    private boolean deleteAll;

    private String classPattern;

    private int limit;

    @Option(shortName = "l", longName = "list", flag = true)
    @Description("list all retransform entry.")
    public void setList(boolean list) {
        this.list = list;
    }

    @Option(shortName = "d", longName = "delete")
    @Description("delete retransform entry by id.")
    public void setDelete(int delete) {
        this.delete = delete;
    }

    @Option(longName = "deleteAll", flag = true)
    @Description("delete all retransform entries.")
    public void setDeleteAll(boolean deleteAll) {
        this.deleteAll = deleteAll;
    }

    @Option(longName = "classPattern")
    @Description("trigger retransform matched classes by class pattern.")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("classLoader hashcode")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Argument(argName = "classfilePaths", index = 0, required = false)
    @Description(".class file paths")
    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    @Option(longName = "limit")
    @Description("The limit of dump classes size, default value is 50")
    @DefaultValue("50")
    public void setLimit(int limit) {
        this.limit = limit;
    }

    private static void initTransformer() {
        if (transformer != null) {
            return;
        } else {
            synchronized (RetransformCommand.class) {
                if (transformer == null) {
                    transformer = new RetransformClassFileTransformer();
                    TransformerManager transformerManager = ArthasBootstrap.getInstance().getTransformerManager();
                    transformerManager.addRetransformer(transformer);
                }
            }
        }
    }

    @Override
    public void process(CommandProcess process) {
        initTransformer();

        RetransformModel retransformModel = new RetransformModel();
        Instrumentation inst = process.session().getInstrumentation();

        if (this.list) {
            List<RetransformEntry> retransformEntryList = allRetransformEntries();
            retransformModel.setRetransformEntries(retransformEntryList);
            process.appendResult(retransformModel);
            process.end();
            return;
        } else if (this.deleteAll) {
            deleteAllRetransformEntry();
            process.appendResult(retransformModel);
            process.end();
            return;
        } else if (this.delete > 0) {
            deleteRetransformEntry(this.delete);
            process.end();
            return;
        } else if (this.classPattern != null) {
            Set<Class<?>> searchClass = SearchUtils.searchClass(inst, classPattern, false, this.hashCode);
            if (searchClass.isEmpty()) {
                process.end(-1, "These classes are not found in the JVM and may not be loaded: " + classPattern);
                return;
            }

            if (searchClass.size() > limit) {
                process.end(-1, "match classes size: " + searchClass.size() + ", more than limit: " + limit
                        + ", It is recommended to use a more precise class pattern.");
            }
            try {
                inst.retransformClasses(searchClass.toArray(new Class[0]));
                for (Class<?> clazz : searchClass) {
                    retransformModel.addRetransformClass(clazz.getName());
                }
                process.appendResult(retransformModel);
                process.end();
                return;
            } catch (Throwable e) {
                String message = "retransform error! " + e.toString();
                logger.error(message, e);
                process.end(-1, message);
                return;
            }
        }

        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                process.end(-1, "file does not exist, path:" + path);
                return;
            }
            if (!file.isFile()) {
                process.end(-1, "not a normal file, path:" + path);
                return;
            }
            if (file.length() >= MAX_FILE_SIZE) {
                process.end(-1, "file size: " + file.length() + " >= " + MAX_FILE_SIZE + ", path: " + path);
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
                logger.warn("load class file failed: " + path, e);
                process.end(-1, "load class file failed: " + path + ", error: " + e);
                return;
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
            process.end(-1, "paths may contains same class name!");
            return;
        }

        List<RetransformEntry> retransformEntryList = new ArrayList<RetransformEntry>();

        List<Class<?>> classList = new ArrayList<Class<?>>();

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (bytesMap.containsKey(clazz.getName())) {

                if (hashCode == null && classLoaderClass != null) {
                    List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst,
                            classLoaderClass);
                    if (matchedClassLoaders.size() == 1) {
                        hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
                    } else if (matchedClassLoaders.size() > 1) {
                        Collection<ClassLoaderVO> classLoaderVOList = ClassUtils
                                .createClassLoaderVOList(matchedClassLoaders);
                        retransformModel.setClassLoaderClass(classLoaderClass)
                                .setMatchedClassLoaders(classLoaderVOList);
                        process.appendResult(retransformModel);
                        process.end(-1,
                                "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                        return;
                    } else {
                        process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                        return;
                    }
                }

                ClassLoader classLoader = clazz.getClassLoader();
                if (classLoader != null && hashCode != null
                        && !Integer.toHexString(classLoader.hashCode()).equals(hashCode)) {
                    continue;
                }

                RetransformEntry retransformEntry = new RetransformEntry(clazz.getName(), bytesMap.get(clazz.getName()),
                        hashCode, classLoaderClass);
                retransformEntryList.add(retransformEntry);
                classList.add(clazz);
                retransformModel.addRetransformClass(clazz.getName());

                logger.info("Try retransform class name: {}, ClassLoader: {}", clazz.getName(), clazz.getClassLoader());
            }
        }

        try {
            if (retransformEntryList.isEmpty()) {
                process.end(-1, "These classes are not found in the JVM and may not be loaded: " + bytesMap.keySet());
                return;
            }
            addRetransformEntry(retransformEntryList);

            inst.retransformClasses(classList.toArray(new Class[0]));

            process.appendResult(retransformModel);
            process.end();
        } catch (Throwable e) {
            String message = "retransform error! " + e.toString();
            logger.error(message, e);
            process.end(-1, message);
        }

    }

    private static String readClassName(final byte[] bytes) {
        return new ClassReader(bytes).getClassName().replace('/', '.');
    }

    @Override
    public void complete(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();

        if (CompletionUtils.shouldCompleteOption(completion, "--classPattern")) {
            CompletionUtils.completeClassName(completion);
            return;
        }

        for (CliToken token : tokens) {
            String tokenStr = token.value();
            if (tokenStr != null && tokenStr.startsWith("-")) {
                super.complete(completion);
                return;
            }
        }

        // 最后，没有有 - 开头的，才尝试补全 file path
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

    public static class RetransformEntry {
        private static final AtomicInteger counter = new AtomicInteger(0);
        private int id;
        private String className;
        private byte[] bytes;
        private String hashCode;
        private String classLoaderClass;

        /**
         * 被 transform 触发次数
         */
        private int transformCount = 0;

        public RetransformEntry(String className, byte[] bytes, String hashCode, String classLoaderClass) {
            id = counter.incrementAndGet();
            this.className = className;
            this.bytes = bytes;
            this.hashCode = hashCode;
            this.classLoaderClass = classLoaderClass;
        }

        public void incTransformCount() {
            transformCount++;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getTransformCount() {
            return transformCount;
        }

        public void setTransformCount(int transformCount) {
            this.transformCount = transformCount;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public String getHashCode() {
            return hashCode;
        }

        public void setHashCode(String hashCode) {
            this.hashCode = hashCode;
        }

        public String getClassLoaderClass() {
            return classLoaderClass;
        }

        public void setClassLoaderClass(String classLoaderClass) {
            this.classLoaderClass = classLoaderClass;
        }
    }

    public static synchronized void addRetransformEntry(List<RetransformEntry> retransformEntryList) {
        List<RetransformEntry> tmp = new ArrayList<RetransformEntry>();
        tmp.addAll(retransformEntries);
        tmp.addAll(retransformEntryList);
        Collections.sort(tmp, new Comparator<RetransformEntry>() {
            @Override
            public int compare(RetransformEntry entry1, RetransformEntry entry2) {
                return entry1.getId() - entry2.getId();
            }
        });
        retransformEntries = tmp;
    }

    public static synchronized RetransformEntry deleteRetransformEntry(int id) {
        RetransformEntry result = null;
        List<RetransformEntry> tmp = new ArrayList<RetransformEntry>();
        for (RetransformEntry entry : retransformEntries) {
            if (entry.getId() != id) {
                tmp.add(entry);
            } else {
                result = entry;
            }
        }
        retransformEntries = tmp;
        return result;
    }

    public static List<RetransformEntry> allRetransformEntries() {
        return retransformEntries;
    }

    public static synchronized void deleteAllRetransformEntry() {
        retransformEntries = new ArrayList<RetransformEntry>();
    }

    static class RetransformClassFileTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

            if (className == null) {
                return null;
            }

            className = className.replace('/', '.');

            List<RetransformEntry> allRetransformEntries = allRetransformEntries();
            // 倒序，因为要执行的配置生效
            ListIterator<RetransformEntry> listIterator = allRetransformEntries
                    .listIterator(allRetransformEntries.size());
            while (listIterator.hasPrevious()) {
                RetransformEntry retransformEntry = listIterator.previous();
                int id = retransformEntry.getId();
                // 判断类名是否一致
                boolean updateFlag = false;
                // 类名一致，则看是否要比较 loader，如果不需要比较 loader，则认为成功
                if (className.equals(retransformEntry.getClassName())) {
                    if (retransformEntry.getClassLoaderClass() != null || retransformEntry.getHashCode() != null) {
                        updateFlag = isLoaderMatch(retransformEntry, loader);
                    } else {
                        updateFlag = true;
                    }
                }

                if (updateFlag) {
                    logger.info("RetransformCommand match class: {}, id: {}, classLoaderClass: {}, hashCode: {}",
                            className, id, retransformEntry.getClassLoaderClass(), retransformEntry.getHashCode());
                    retransformEntry.incTransformCount();
                    return retransformEntry.getBytes();
                }

            }

            return null;
        }

        private boolean isLoaderMatch(RetransformEntry retransformEntry, ClassLoader loader) {
            if (loader == null) {
                return false;
            }
            if (retransformEntry.getClassLoaderClass() != null) {
                if (loader.getClass().getName().equals(retransformEntry.getClassLoaderClass())) {
                    return true;
                }
            }
            if (retransformEntry.getHashCode() != null) {
                String hashCode = Integer.toHexString(loader.hashCode());
                if (hashCode.equals(retransformEntry.getHashCode())) {
                    return true;
                }
            }
            return false;
        }

    }
}
