package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassDetailVO;
import com.taobao.arthas.core.command.model.ClassLoaderModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.ClassSetVO;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ResultUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

@Name("classloader")
@Summary("Show classloader info")
@Description(Constants.EXAMPLE +
        "  classloader\n" +
        "  classloader -t\n" +
        "  classloader -l\n" +
        "  classloader -c 327a647b\n" +
        "  classloader -c 327a647b -r META-INF/MANIFEST.MF\n" +
        "  classloader -a\n" +
        "  classloader -a -c 327a647b\n" +
        "  classloader -c 659e0bfd --load demo.MathGame\n" +
        "  classloader -u      # url statistics\n" +
        Constants.WIKI + Constants.WIKI_HOME + "classloader")
public class ClassLoaderCommand extends AnnotatedCommand {

    private Logger logger = LoggerFactory.getLogger(ClassLoaderCommand.class);
    private boolean isTree = false;
    private String hashCode;
    private String classLoaderClass;
    private boolean all = false;
    private String resource;
    private boolean includeReflectionClassLoader = true;
    private boolean listClassLoader = false;

    private boolean urlStat = false;

    private String loadClass = null;

    private volatile boolean isInterrupted = false;

    @Option(shortName = "t", longName = "tree", flag = true)
    @Description("Display ClassLoader tree")
    public void setTree(boolean tree) {
        isTree = tree;
    }
    
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special ClassLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(shortName = "a", longName = "all", flag = true)
    @Description("Display all classes loaded by ClassLoader")
    public void setAll(boolean all) {
        this.all = all;
    }

    @Option(shortName = "r", longName = "resource")
    @Description("Use ClassLoader to find resources, won't work without -c specified")
    public void setResource(String resource) {
        this.resource = resource;
    }

    @Option(shortName = "i", longName = "include-reflection-classloader", flag = true)
    @Description("Include sun.reflect.DelegatingClassLoader")
    public void setIncludeReflectionClassLoader(boolean includeReflectionClassLoader) {
        this.includeReflectionClassLoader = includeReflectionClassLoader;
    }

    @Option(shortName = "l", longName = "list-classloader", flag = true)
    @Description("Display statistics info by classloader instance")
    public void setListClassLoader(boolean listClassLoader) {
        this.listClassLoader = listClassLoader;
    }

    @Option(longName = "load")
    @Description("Use ClassLoader to load class, won't work without -c specified")
    public void setLoadClass(String className) {
        this.loadClass = className;
    }

    @Option(shortName = "u", longName = "url-stat", flag = true)
    @Description("Display classloader url statistics")
    public void setUrlStat(boolean urlStat) {
        this.urlStat = urlStat;
    }

    @Override
    public void process(CommandProcess process) {
        // ctrl-C support
        process.interruptHandler(new ClassLoaderInterruptHandler(this));
        ClassLoader targetClassLoader = null;
        boolean classLoaderSpecified = false;

        Instrumentation inst = process.session().getInstrumentation();

        if (urlStat) {
            Map<ClassLoaderVO, ClassLoaderUrlStat> urlStats = this.urlStats(inst);
            ClassLoaderModel model = new ClassLoaderModel();
            model.setUrlStats(urlStats);
            process.appendResult(model);
            process.end();
            return;
        }
        
        if (hashCode != null || classLoaderClass != null) {
            classLoaderSpecified = true;
        }
        
        if (hashCode != null) {
            Set<ClassLoader> allClassLoader = getAllClassLoaders(inst);
            for (ClassLoader cl : allClassLoader) {
                if (Integer.toHexString(cl.hashCode()).equals(hashCode)) {
                    targetClassLoader = cl;
                    break;
                }
            }
        } else if (classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                targetClassLoader = matchedClassLoaders.get(0);
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                ClassLoaderModel classloaderModel = new ClassLoaderModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(classloaderModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        if (all) {
            String hashCode = this.hashCode;
            if (StringUtils.isBlank(hashCode) && targetClassLoader != null) {
                hashCode = "" + Integer.toHexString(targetClassLoader.hashCode());
            }
            processAllClasses(process, inst, hashCode);
        } else if (classLoaderSpecified && resource != null) {
            processResources(process, inst, targetClassLoader);
        } else if (classLoaderSpecified && this.loadClass != null) {
            processLoadClass(process, inst, targetClassLoader);
        } else if (classLoaderSpecified) {
            processClassLoader(process, inst, targetClassLoader);
        } else if (listClassLoader || isTree){
            processClassLoaders(process, inst);
        } else {
            processClassLoaderStats(process, inst);
        }
    }

    /**
     * Calculate classloader statistics.
     * e.g. In JVM, there are 100 GrooyClassLoader instances, which loaded 200 classes in total
     * @param process
     * @param inst
     */
    private void processClassLoaderStats(CommandProcess process, Instrumentation inst) {
        RowAffect affect = new RowAffect();
        List<ClassLoaderInfo> classLoaderInfos = getAllClassLoaderInfo(inst);
        Map<String, ClassLoaderStat> classLoaderStats = new HashMap<String, ClassLoaderStat>();
        for (ClassLoaderInfo info: classLoaderInfos) {
            String name = info.classLoader == null ? "BootstrapClassLoader" : info.classLoader.getClass().getName();
            ClassLoaderStat stat = classLoaderStats.get(name);
            if (null == stat) {
                stat = new ClassLoaderStat();
                classLoaderStats.put(name, stat);
            }
            stat.addLoadedCount(info.loadedClassCount);
            stat.addNumberOfInstance(1);
        }

        // sort the map by value
        TreeMap<String, ClassLoaderStat> sorted =
                new TreeMap<String, ClassLoaderStat>(new ValueComparator(classLoaderStats));
        sorted.putAll(classLoaderStats);
        process.appendResult(new ClassLoaderModel().setClassLoaderStats(sorted));

        affect.rCnt(sorted.keySet().size());
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    private void processClassLoaders(CommandProcess process, Instrumentation inst) {
        RowAffect affect = new RowAffect();
        List<ClassLoaderInfo> classLoaderInfos = includeReflectionClassLoader ? getAllClassLoaderInfo(inst) :
                getAllClassLoaderInfo(inst, new SunReflectionClassLoaderFilter());

        List<ClassLoaderVO> classLoaderVOs = new ArrayList<ClassLoaderVO>(classLoaderInfos.size());
        for (ClassLoaderInfo classLoaderInfo : classLoaderInfos) {
            ClassLoaderVO classLoaderVO = ClassUtils.createClassLoaderVO(classLoaderInfo.classLoader);
            classLoaderVO.setLoadedCount(classLoaderInfo.loadedClassCount());
            classLoaderVOs.add(classLoaderVO);
        }
        if (isTree){
            classLoaderVOs = processClassLoaderTree(classLoaderVOs);
        }
        process.appendResult(new ClassLoaderModel().setClassLoaders(classLoaderVOs).setTree(isTree));

        affect.rCnt(classLoaderInfos.size());
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    // 根据 ClassLoader 来打印URLClassLoader的urls
    private void processClassLoader(CommandProcess process, Instrumentation inst, ClassLoader targetClassLoader) {
        RowAffect affect = new RowAffect();
        if (targetClassLoader != null) {
            URL[] classLoaderUrls = ClassLoaderUtils.getUrls(targetClassLoader);
            if (classLoaderUrls != null) {
                affect.rCnt(classLoaderUrls.length);
                if (classLoaderUrls.length == 0) {
                    process.appendResult(new MessageModel("urls is empty."));
                } else {
                    process.appendResult(new ClassLoaderModel().setUrls(StringUtils.toStringList(classLoaderUrls)));
                    affect.rCnt(classLoaderUrls.length);
                }
            } else {
                process.appendResult(new MessageModel("not a URLClassLoader."));
            }
        }
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    // 使用ClassLoader去getResources
    private void processResources(CommandProcess process, Instrumentation inst, ClassLoader targetClassLoader) {
        RowAffect affect = new RowAffect();
        int rowCount = 0;
        List<String> resources = new ArrayList<String>();
        if (targetClassLoader != null) {
            try {
                Enumeration<URL> urls = targetClassLoader.getResources(resource);
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    resources.add(url.toString());
                    rowCount++;
                }
            } catch (Throwable e) {
                logger.warn("get resource failed, resource: {}", resource, e);
            }
        }
        affect.rCnt(rowCount);

        process.appendResult(new ClassLoaderModel().setResources(resources));
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    // Use ClassLoader to loadClass
    private void processLoadClass(CommandProcess process, Instrumentation inst, ClassLoader targetClassLoader) {
        if (targetClassLoader != null) {
            try {
                Class<?> clazz = targetClassLoader.loadClass(this.loadClass);
                process.appendResult(new MessageModel("load class success."));
                ClassDetailVO classInfo = ClassUtils.createClassInfo(clazz, false, null);
                process.appendResult(new ClassLoaderModel().setLoadClass(classInfo));

            } catch (Throwable e) {
                logger.warn("load class error, class: {}", this.loadClass, e);
                process.end(-1, "load class error, class: "+this.loadClass+", error: "+e.toString());
                return;
            }
        }
        process.end();
    }

    private void processAllClasses(CommandProcess process, Instrumentation inst,String hashCode) {
        RowAffect affect = new RowAffect();
        getAllClasses(hashCode, inst, affect, process);
        if (checkInterrupted(process)) {
            return;
        }
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    /**
     * 获取到所有的class, 还有它们的classloader，按classloader归类好，统一输出每个classloader里有哪些class
     * <p>
     * 当hashCode是null，则把所有的classloader的都打印
     *
     */
    @SuppressWarnings("rawtypes")
    private void getAllClasses(String hashCode, Instrumentation inst, RowAffect affect, CommandProcess process) {
        int hashCodeInt = -1;
        if (hashCode != null) {
            hashCodeInt = Integer.valueOf(hashCode, 16);
        }

        SortedSet<Class<?>> bootstrapClassSet = new TreeSet<Class<?>>(new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        Class[] allLoadedClasses = inst.getAllLoadedClasses();
        Map<ClassLoader, SortedSet<Class<?>>> classLoaderClassMap = new HashMap<ClassLoader, SortedSet<Class<?>>>();
        for (Class clazz : allLoadedClasses) {
            ClassLoader classLoader = clazz.getClassLoader();
            // Class loaded by BootstrapClassLoader
            if (classLoader == null) {
                if (hashCode == null) {
                    bootstrapClassSet.add(clazz);
                }
                continue;
            }

            if (hashCode != null && classLoader.hashCode() != hashCodeInt) {
                continue;
            }

            SortedSet<Class<?>> classSet = classLoaderClassMap.get(classLoader);
            if (classSet == null) {
                classSet = new TreeSet<Class<?>>(new Comparator<Class<?>>() {
                    @Override
                    public int compare(Class<?> o1, Class<?> o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                classLoaderClassMap.put(classLoader, classSet);
            }
            classSet.add(clazz);
        }

        // output bootstrapClassSet
        int pageSize = 256;
        processClassSet(process, ClassUtils.createClassLoaderVO(null), bootstrapClassSet, pageSize, affect);

        // output other classSet
        for (Entry<ClassLoader, SortedSet<Class<?>>> entry : classLoaderClassMap.entrySet()) {
            if (checkInterrupted(process)) {
                return;
            }
            ClassLoader classLoader = entry.getKey();
            SortedSet<Class<?>> classSet = entry.getValue();
            processClassSet(process, ClassUtils.createClassLoaderVO(classLoader), classSet, pageSize, affect);
        }
    }

    private void processClassSet(final CommandProcess process, final ClassLoaderVO classLoaderVO, Collection<Class<?>> classes, int pageSize, final RowAffect affect) {
        //分批输出classNames, Ctrl+C可以中断执行
        ResultUtils.processClassNames(classes, pageSize, new ResultUtils.PaginationHandler<List<String>>() {
            @Override
            public boolean handle(List<String> classNames, int segment) {
                process.appendResult(new ClassLoaderModel().setClassSet(new ClassSetVO(classLoaderVO, classNames, segment)));
                affect.rCnt(classNames.size());
                return !checkInterrupted(process);
            }
        });
    }

    private boolean checkInterrupted(CommandProcess process) {
        if (!process.isRunning()) {
            return true;
        }
        if(isInterrupted){
            process.end(-1, "Processing has been interrupted");
            return true;
        } else {
            return false;
        }
    }

    private Map<ClassLoaderVO, ClassLoaderUrlStat> urlStats(Instrumentation inst) {
        Map<ClassLoaderVO, ClassLoaderUrlStat> urlStats = new HashMap<ClassLoaderVO, ClassLoaderUrlStat>();
        Map<ClassLoader, Set<String>> usedUrlsMap = new HashMap<ClassLoader, Set<String>>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader != null) {
                ProtectionDomain protectionDomain = clazz.getProtectionDomain();
                CodeSource codeSource = protectionDomain.getCodeSource();
                if (codeSource != null) {
                    URL location = codeSource.getLocation();
                    if (location != null) {
                        Set<String> urls = usedUrlsMap.get(classLoader);
                        if (urls == null) {
                            urls = new HashSet<String>();
                            usedUrlsMap.put(classLoader, urls);
                        }
                        urls.add(location.toString());
                    }
                }
            }
        }
        for (Entry<ClassLoader, Set<String>> entry : usedUrlsMap.entrySet()) {
            ClassLoader loader = entry.getKey();
            Set<String> usedUrls = entry.getValue();
            URL[] allUrls = ClassLoaderUtils.getUrls(loader);
            List<String> unusedUrls = new ArrayList<String>();
            if (allUrls != null) {
                for (URL url : allUrls) {
                    String urlStr = url.toString();
                    if (!usedUrls.contains(urlStr)) {
                        unusedUrls.add(urlStr);
                    }
                }
            }

            urlStats.put(ClassUtils.createClassLoaderVO(loader), new ClassLoaderUrlStat(usedUrls, unusedUrls));
        }
        return urlStats;
    }

    // 以树状列出ClassLoader的继承结构
    private static List<ClassLoaderVO> processClassLoaderTree(List<ClassLoaderVO> classLoaders) {
        List<ClassLoaderVO> rootClassLoaders = new ArrayList<ClassLoaderVO>();
        List<ClassLoaderVO> parentNotNullClassLoaders = new ArrayList<ClassLoaderVO>();
        for (ClassLoaderVO classLoaderVO : classLoaders) {
            if (classLoaderVO.getParent() == null) {
                rootClassLoaders.add(classLoaderVO);
            } else {
                parentNotNullClassLoaders.add(classLoaderVO);
            }
        }

        for (ClassLoaderVO classLoaderVO : rootClassLoaders) {
            buildTree(classLoaderVO, parentNotNullClassLoaders);
        }
        return rootClassLoaders;
    }

    private static void buildTree(ClassLoaderVO parent, List<ClassLoaderVO> parentNotNullClassLoaders) {
        for (ClassLoaderVO classLoaderVO : parentNotNullClassLoaders) {
            if (parent.getName().equals(classLoaderVO.getParent())){
                parent.addChild(classLoaderVO);
                buildTree(classLoaderVO, parentNotNullClassLoaders);
            }
        }
    }

    private static Set<ClassLoader> getAllClassLoaders(Instrumentation inst, Filter... filters) {
        Set<ClassLoader> classLoaderSet = new HashSet<ClassLoader>();

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader != null) {
                if (shouldInclude(classLoader, filters)) {
                    classLoaderSet.add(classLoader);
                }
            }
        }
        return classLoaderSet;
    }

    private static List<ClassLoaderInfo> getAllClassLoaderInfo(Instrumentation inst, Filter... filters) {
        // 这里认为class.getClassLoader()返回是null的是由BootstrapClassLoader加载的，特殊处理
        ClassLoaderInfo bootstrapInfo = new ClassLoaderInfo(null);

        Map<ClassLoader, ClassLoaderInfo> loaderInfos = new HashMap<ClassLoader, ClassLoaderInfo>();

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader == null) {
                bootstrapInfo.increase();
            } else {
                if (shouldInclude(classLoader, filters)) {
                    ClassLoaderInfo loaderInfo = loaderInfos.get(classLoader);
                    if (loaderInfo == null) {
                        loaderInfo = new ClassLoaderInfo(classLoader);
                        loaderInfos.put(classLoader, loaderInfo);
                        ClassLoader parent = classLoader.getParent();
                        while (parent != null) {
                            ClassLoaderInfo parentLoaderInfo = loaderInfos.get(parent);
                            if (parentLoaderInfo == null) {
                                parentLoaderInfo = new ClassLoaderInfo(parent);
                                loaderInfos.put(parent, parentLoaderInfo);
                            }
                            parent = parent.getParent();
                        }
                    }
                    loaderInfo.increase();
                }
            }
        }

        // 排序时，把用户自己定的ClassLoader排在最前面，以sun.
        // 开头的放后面，因为sun.reflect.DelegatingClassLoader的实例太多
        List<ClassLoaderInfo> sunClassLoaderList = new ArrayList<ClassLoaderInfo>();

        List<ClassLoaderInfo> otherClassLoaderList = new ArrayList<ClassLoaderInfo>();

        for (Entry<ClassLoader, ClassLoaderInfo> entry : loaderInfos.entrySet()) {
            ClassLoader classLoader = entry.getKey();
            if (classLoader.getClass().getName().startsWith("sun.")) {
                sunClassLoaderList.add(entry.getValue());
            } else {
                otherClassLoaderList.add(entry.getValue());
            }
        }

        Collections.sort(sunClassLoaderList);
        Collections.sort(otherClassLoaderList);

        List<ClassLoaderInfo> result = new ArrayList<ClassLoaderInfo>();
        result.add(bootstrapInfo);
        result.addAll(otherClassLoaderList);
        result.addAll(sunClassLoaderList);
        return result;
    }

    private static boolean shouldInclude(ClassLoader classLoader, Filter... filters) {
        if (filters == null) {
            return true;
        }

        for (Filter filter : filters) {
            if (!filter.accept(classLoader)) {
                return false;
            }
        }
        return true;
    }

    private static class ClassLoaderInfo implements Comparable<ClassLoaderInfo> {
        private ClassLoader classLoader;
        private int loadedClassCount = 0;

        ClassLoaderInfo(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public String getName() {
            if (classLoader != null) {
                return classLoader.toString();
            }
            return "BootstrapClassLoader";
        }

        String hashCodeStr() {
            if (classLoader != null) {
                return "" + Integer.toHexString(classLoader.hashCode());
            }
            return "null";
        }

        void increase() {
            loadedClassCount++;
        }

        int loadedClassCount() {
            return loadedClassCount;
        }

        ClassLoader parent() {
            return classLoader == null ? null : classLoader.getParent();
        }

        String parentStr() {
            if (classLoader == null) {
                return "null";
            }
            ClassLoader parent = classLoader.getParent();
            if (parent == null) {
                return "null";
            }
            return parent.toString();
        }

        @Override
        public int compareTo(ClassLoaderInfo other) {
            if (other == null) {
                return -1;
            }
            if (other.classLoader == null) {
                return -1;
            }
            if (this.classLoader == null) {
                return -1;
            }

            return this.classLoader.getClass().getName().compareTo(other.classLoader.getClass().getName());
        }

    }

    private interface Filter {
        boolean accept(ClassLoader classLoader);
    }

    private static class SunReflectionClassLoaderFilter implements Filter {
        private static final List<String> REFLECTION_CLASSLOADERS = Arrays.asList("sun.reflect.DelegatingClassLoader",
                "jdk.internal.reflect.DelegatingClassLoader");

        @Override
        public boolean accept(ClassLoader classLoader) {
            return !REFLECTION_CLASSLOADERS.contains(classLoader.getClass().getName());
        }
    }

    public static class ClassLoaderUrlStat {
        private Collection<String> usedUrls;
        private Collection<String> unUsedUrls;

        public ClassLoaderUrlStat() {
        }

        public ClassLoaderUrlStat(Collection<String> usedUrls, Collection<String> unUsedUrls) {
            super();
            this.usedUrls = usedUrls;
            this.unUsedUrls = unUsedUrls;
        }

        public Collection<String> getUsedUrls() {
            return usedUrls;
        }

        public void setUsedUrls(Collection<String> usedUrls) {
            this.usedUrls = usedUrls;
        }

        public Collection<String> getUnUsedUrls() {
            return unUsedUrls;
        }

        public void setUnUsedUrls(Collection<String> unUsedUrls) {
            this.unUsedUrls = unUsedUrls;
        }
    }

    public static class ClassLoaderStat {
        private int loadedCount;
        private int numberOfInstance;

        void addLoadedCount(int count) {
            this.loadedCount += count;
        }

        void addNumberOfInstance(int count) {
            this.numberOfInstance += count;
        }

        public int getLoadedCount() {
            return loadedCount;
        }

        public int getNumberOfInstance() {
            return numberOfInstance;
        }
    }

    private static class ValueComparator implements Comparator<String> {

        private Map<String, ClassLoaderStat> unsortedStats;

        ValueComparator(Map<String, ClassLoaderStat> stats) {
            this.unsortedStats = stats;
        }

        @Override
        public int compare(String o1, String o2) {
            if (null == unsortedStats) {
                return -1;
            }
            if (!unsortedStats.containsKey(o1)) {
                return 1;
            }
            if (!unsortedStats.containsKey(o2)) {
                return -1;
            }
            return unsortedStats.get(o2).getLoadedCount() - unsortedStats.get(o1).getLoadedCount();
        }
    }

    private static class ClassLoaderInterruptHandler implements Handler<Void> {

        private ClassLoaderCommand command;

        public ClassLoaderInterruptHandler(ClassLoaderCommand command) {
            this.command = command;
        }

        @Override
        public void handle(Void event) {
            command.isInterrupted = true;
        }
    }
}
