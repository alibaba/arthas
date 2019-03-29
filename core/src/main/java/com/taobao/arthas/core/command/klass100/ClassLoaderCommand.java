package com.taobao.arthas.core.command.klass100;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.RowElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.ui.TreeElement;
import com.taobao.text.util.RenderUtil;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
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
        Constants.WIKI + Constants.WIKI_HOME + "classloader")
public class ClassLoaderCommand extends AnnotatedCommand {
    private boolean isTree = false;
    private String hashCode;
    private boolean all = false;
    private String resource;
    private boolean includeReflectionClassLoader = true;
    private boolean listClassLoader = false;

    private String loadClass = null;

    @Option(shortName = "t", longName = "tree", flag = true)
    @Description("Display ClassLoader tree")
    public void setTree(boolean tree) {
        isTree = tree;
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

    @Override
    public void process(CommandProcess process) {
        Instrumentation inst = process.session().getInstrumentation();
        if (all) {
            processAllClasses(process, inst);
        } else if (hashCode != null && resource != null) {
            processResources(process, inst);
        } else if (hashCode != null && this.loadClass != null) {
            processLoadClass(process, inst);
        } else if (hashCode != null) {
            processClassloader(process, inst);
        } else if (listClassLoader || isTree){
            processClassloaders(process, inst);
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

        Element element = renderStat(sorted);
        process.write(RenderUtil.render(element, process.width()))
                .write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
        affect.rCnt(sorted.keySet().size());
        process.write(affect + "\n");
        process.end();
    }

    private void processClassloaders(CommandProcess process, Instrumentation inst) {
        RowAffect affect = new RowAffect();
        List<ClassLoaderInfo> classLoaderInfos = includeReflectionClassLoader ? getAllClassLoaderInfo(inst) :
                getAllClassLoaderInfo(inst, new SunReflectionClassLoaderFilter());
        Element element = isTree ? renderTree(classLoaderInfos) : renderTable(classLoaderInfos);
        process.write(RenderUtil.render(element, process.width()))
                .write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
        affect.rCnt(classLoaderInfos.size());
        process.write(affect + "\n");
        process.end();
    }

    // 根据 hashCode 来打印URLClassLoader的urls
    private void processClassloader(CommandProcess process, Instrumentation inst) {
        RowAffect affect = new RowAffect();

        Set<ClassLoader> allClassLoader = getAllClassLoader(inst);
        for (ClassLoader cl : allClassLoader) {
            if (Integer.toHexString(cl.hashCode()).equals(hashCode)) {
                process.write(RenderUtil.render(renderClassLoaderUrls(cl), process.width()));
            }
        }
        process.write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
        affect.rCnt(allClassLoader.size());
        process.write(affect + "\n");
        process.end();
    }

    // 使用ClassLoader去getResources
    private void processResources(CommandProcess process, Instrumentation inst) {
        RowAffect affect = new RowAffect();
        int rowCount = 0;
        Set<ClassLoader> allClassLoader = getAllClassLoader(inst);
        for (ClassLoader cl : allClassLoader) {
            if (Integer.toHexString(cl.hashCode()).equals(hashCode)) {
                TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
                try {
                    Enumeration<URL> urls = cl.getResources(resource);
                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();
                        table.row(url.toString());
                        rowCount++;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                process.write(RenderUtil.render(table, process.width()) + "\n");
            }
        }
        process.write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
        affect.rCnt(rowCount);
        process.write(affect + "\n");
        process.end();
    }

    // Use ClassLoader to loadClass
    private void processLoadClass(CommandProcess process, Instrumentation inst) {
        Set<ClassLoader> allClassLoader = getAllClassLoader(inst);
        for (ClassLoader cl : allClassLoader) {
            if (Integer.toHexString(cl.hashCode()).equals(hashCode)) {
                try {
                    Class<?> clazz = cl.loadClass(this.loadClass);
                    process.write("load class success.\n");
                    process.write(RenderUtil.render(ClassUtils.renderClassInfo(clazz), process.width()) + "\n");

                } catch (Throwable e) {
                    e.printStackTrace();
                    process.write("load class error.\n" + StringUtils.objectToString(new ObjectView(e, 1).draw()));
                }
            }
        }
        process.write("\n");
        process.end();
    }

    private void processAllClasses(CommandProcess process, Instrumentation inst) {
        RowAffect affect = new RowAffect();
        process.write(RenderUtil.render(renderClasses(hashCode, inst), process.width()));
        process.write(affect + "\n");
        process.end();
    }

    /**
     * 获取到所有的class, 还有它们的classloader，按classloader归类好，统一输出每个classloader里有哪些class
     * <p>
     * 当hashCode是null，则把所有的classloader的都打印
     *
     * @param hashCode
     * @return
     */
    @SuppressWarnings("rawtypes")
    private static Element renderClasses(String hashCode, Instrumentation inst) {
        int hashCodeInt = -1;
        if (hashCode != null) {
            hashCodeInt = Integer.valueOf(hashCode, 16);
        }

        SortedSet<Class> bootstrapClassSet = new TreeSet<Class>(new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        Class[] allLoadedClasses = inst.getAllLoadedClasses();
        Map<ClassLoader, SortedSet<Class>> classLoaderClassMap = new HashMap<ClassLoader, SortedSet<Class>>();
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

            SortedSet<Class> classSet = classLoaderClassMap.get(classLoader);
            if (classSet == null) {
                classSet = new TreeSet<Class>(new Comparator<Class>() {
                    @Override
                    public int compare(Class o1, Class o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                classLoaderClassMap.put(classLoader, classSet);
            }
            classSet.add(clazz);
        }

        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);

        if (!bootstrapClassSet.isEmpty()) {
            table.row(new LabelElement("hash:null, BootstrapClassLoader").style(Decoration.bold.bold()));
            for (Class clazz : bootstrapClassSet) {
                table.row(new LabelElement(clazz.getName()));
            }
            table.row(new LabelElement(" "));
        }

        for (Entry<ClassLoader, SortedSet<Class>> entry : classLoaderClassMap.entrySet()) {
            ClassLoader classLoader = entry.getKey();
            SortedSet<Class> classSet = entry.getValue();

            table.row(new LabelElement("hash:" + classLoader.hashCode() + ", " + classLoader.toString())
                    .style(Decoration.bold.bold()));
            for (Class clazz : classSet) {
                table.row(new LabelElement(clazz.getName()));
            }

            table.row(new LabelElement(" "));
        }
        return table;
    }

    private static Element renderClassLoaderUrls(ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader cl = (URLClassLoader) classLoader;
            URL[] urls = cl.getURLs();
            if (urls != null) {
                for (URL url : urls) {
                    sb.append(url.toString() + "\n");
                }
                return new LabelElement(sb.toString());
            } else {
                return new LabelElement("urls is empty.");
            }
        } else {
            return new LabelElement("not a URLClassLoader.\n");
        }
    }

    // 以树状列出ClassLoader的继承结构
    private static Element renderTree(List<ClassLoaderInfo> classLoaderInfos) {
        TreeElement root = new TreeElement();

        List<ClassLoaderInfo> parentNullClassLoaders = new ArrayList<ClassLoaderInfo>();
        List<ClassLoaderInfo> parentNotNullClassLoaders = new ArrayList<ClassLoaderInfo>();
        for (ClassLoaderInfo info : classLoaderInfos) {
            if (info.parent() == null) {
                parentNullClassLoaders.add(info);
            } else {
                parentNotNullClassLoaders.add(info);
            }
        }

        for (ClassLoaderInfo info : parentNullClassLoaders) {
            if (info.parent() == null) {
                TreeElement parent = new TreeElement(info.getName());
                renderParent(parent, info, parentNotNullClassLoaders);
                root.addChild(parent);
            }
        }

        return root;
    }

    // 统计所有的ClassLoader的信息
    private static TableElement renderTable(List<ClassLoaderInfo> classLoaderInfos) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.add(new RowElement().style(Decoration.bold.bold()).add("name", "loadedCount", "hash", "parent"));
        for (ClassLoaderInfo info : classLoaderInfos) {
            table.row(info.getName(), "" + info.loadedClassCount(), info.hashCodeStr(), info.parentStr());
        }
        return table;
    }

    private static TableElement renderStat(Map<String, ClassLoaderStat> classLoaderStats) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.add(new RowElement().style(Decoration.bold.bold()).add("name", "numberOfInstances", "loadedCountTotal"));
        for (Map.Entry<String, ClassLoaderStat> entry : classLoaderStats.entrySet()) {
            table.row(entry.getKey(), "" + entry.getValue().getNumberOfInstance(), "" + entry.getValue().getLoadedCount());
        }
        return table;
    }

    private static void renderParent(TreeElement node, ClassLoaderInfo parent, List<ClassLoaderInfo> classLoaderInfos) {
        for (ClassLoaderInfo info : classLoaderInfos) {
            if (info.parent() == parent.classLoader) {
                TreeElement child = new TreeElement(info.getName());
                node.addChild(child);
                renderParent(child, info, classLoaderInfos);
            }
        }
    }

    private static Set<ClassLoader> getAllClassLoader(Instrumentation inst, Filter... filters) {
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

        public ClassLoader getClassLoader() {
            return classLoader;
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
        private static final String REFLECTION_CLASSLOADER = "sun.reflect.DelegatingClassLoader";

        @Override
        public boolean accept(ClassLoader classLoader) {
            return !REFLECTION_CLASSLOADER.equals(classLoader.getClass().getName());
        }
    }

    private static class ClassLoaderStat {
        private int loadedCount;
        private int numberOfInstance;

        void addLoadedCount(int count) {
            this.loadedCount += count;
        }

        void addNumberOfInstance(int count) {
            this.numberOfInstance += count;
        }

        int getLoadedCount() {
            return loadedCount;
        }

        int getNumberOfInstance() {
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
}
