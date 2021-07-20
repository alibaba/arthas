package com.taobao.arthas.core.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.concurrent.ConcurrentWeakKeyHashMap;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Process;
import com.taobao.arthas.core.shell.system.ProcessAware;

/**
 * 
 * TODO line 的记录 listener方式？ 还是有string为key，不过 classname|method|desc|num 这样子？
 * 判断是否已插入了，可以在两行中间查询，有没有 SpyAPI 的invoke?
 * 
 * TODO trace的怎么搞？ trace 只记录一次就可以了 classname|method|desc|trace ? 怎么避免 trace 到
 * SPY的invoke ？直接忽略？
 * 
 * TODO trace命令可以动态的增加 新的函数进去不？只要关联上同一个 Listener应该是可以的。
 * 
 * TODO 在SPY里放很多的 Object数组，然后动态的设置进去？ 比如有新的 Listener来的时候。 这样子连查表都不用了。 甚至可以动态生成
 * 存放这些 Listener数组的类？ 这样子的话，只要有 Binding那里，查询到一个具体分配好的类， 这样子就可以了？
 * 甚至每个ClassLoader里都动态生成这样子的 存放类，那么这样子不可以避免查 ClassLoader了么？
 * 
 * 动态为每一个增强类，生成一个新的类，新的类里，有各种的 ID 数组，保存每一个类的每一种 trace 点的信息？？
 * 
 * 多个 watch命令 对同一个类，现在的逻辑是，每个watch都有一个自己的 TransForm，但不会重复增强，因为做了判断。
 * watch命令停止时，也没有去掉增强的代码。 只有reset时 才会去掉。
 * 
 * 其实用户想查看局部变量，并不是想查看哪一行！ 而是想看某个函数里子调用时的 局部变量的值！ 所以实际上是想要一个新的命令，比如 watchinmethod
 * ， 可以 在某个子调用里，
 * 
 * TODO 现在的trace 可以输出行号，可能不是很精确，但是可以对应上的。 这个在新的方式里怎么支持？ 增加一个 linenumber binding？
 * 从mehtodNode，向上查找到最近的行号？
 * 
 * TODO 防止重复增强，最重要的应该还是动态增加 annotation，这个才是真正可以做到某一行，某一个子 invoke 都能识别出来的！ 无论是
 * transform多少次！ 字节码怎么动态加 annotation ？ annotation里签名用 url ?的key/value方式表达！
 * 这样子可以有效还原信息
 * 
 * TODO 是否考虑一个 trace /watch命令之后，得到一个具体的 Listener ID， 允许在另外的窗口里，再次
 * trace/watch时指定这个ID，就会查找到，并处理。 这样子的话，真正达到了动态灵活的，一层一层增加的trace ！
 * 
 * 
 * @author hengyunabc 2020-04-24
 *
 */
public class AdviceListenerManager {
    private static final Logger logger = LoggerFactory.getLogger(AdviceListenerManager.class);
    private static final FakeBootstrapClassLoader FAKEBOOTSTRAPCLASSLOADER = new FakeBootstrapClassLoader();

    static {
        // 清理失效的 AdviceListener
        ArthasBootstrap.getInstance().getScheduledExecutorService().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    if (adviceListenerMap != null) {
                        for (Entry<ClassLoader, ClassLoaderAdviceListenerManager> entry : adviceListenerMap.entrySet()) {
                            ClassLoaderAdviceListenerManager adviceListenerManager = entry.getValue();
                            synchronized (adviceListenerManager) {
                                for (Entry<String, List<AdviceListener>> eee : adviceListenerManager.map.entrySet()) {
                                    List<AdviceListener> listeners = eee.getValue();
                                    List<AdviceListener> newResult = new ArrayList<AdviceListener>();
                                    for (AdviceListener listener : listeners) {
                                        if (listener instanceof ProcessAware) {
                                            ProcessAware processAware = (ProcessAware) listener;
                                            Process process = processAware.getProcess();
                                            if (process == null) {
                                                continue;
                                            }
                                            ExecStatus status = process.status();
                                            if (!status.equals(ExecStatus.TERMINATED)) {
                                                newResult.add(listener);
                                            }
                                        }
                                    }

                                    if (newResult.size() != listeners.size()) {
                                        adviceListenerManager.map.put(eee.getKey(), newResult);
                                    }

                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    try {
                        logger.error("clean AdviceListener error", e);
                    } catch (Throwable t) {
                        // ignore
                    }
                }
            }
        }, 3, 3, TimeUnit.SECONDS);
    }

    private static final ConcurrentWeakKeyHashMap<ClassLoader, ClassLoaderAdviceListenerManager> adviceListenerMap = new ConcurrentWeakKeyHashMap<ClassLoader, ClassLoaderAdviceListenerManager>();

    static class ClassLoaderAdviceListenerManager {
        private ConcurrentHashMap<String, List<AdviceListener>> map = new ConcurrentHashMap<String, List<AdviceListener>>();

        private String key(String className, String methodName, String methodDesc) {
            return className + methodName + methodDesc;
        }

        private String keyForTrace(String className, String owner, String methodName, String methodDesc) {
            return className + owner + methodName + methodDesc;
        }

        public void registerAdviceListener(String className, String methodName, String methodDesc,
                AdviceListener listener) {
            synchronized (this) {
                className = className.replace('/', '.');
                String key = key(className, methodName, methodDesc);

                List<AdviceListener> listeners = map.get(key);
                if (listeners == null) {
                    listeners = new ArrayList<AdviceListener>();
                    map.put(key, listeners);
                }
                if (!listeners.contains(listener)) {
                    listeners.add(listener);
                }
            }
        }

        public List<AdviceListener> queryAdviceListeners(String className, String methodName, String methodDesc) {
            className = className.replace('/', '.');
            String key = key(className, methodName, methodDesc);

            List<AdviceListener> listeners = map.get(key);

            return listeners;
        }

        public void registerTraceAdviceListener(String className, String owner, String methodName, String methodDesc,
                AdviceListener listener) {

            className = className.replace('/', '.');
            String key = keyForTrace(className, owner, methodName, methodDesc);

            List<AdviceListener> listeners = map.get(key);
            if (listeners == null) {
                listeners = new ArrayList<AdviceListener>();
                map.put(key, listeners);
            }
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        public List<AdviceListener> queryTraceAdviceListeners(String className, String owner, String methodName,
                String methodDesc) {
            className = className.replace('/', '.');
            String key = keyForTrace(className, owner, methodName, methodDesc);

            List<AdviceListener> listeners = map.get(key);

            return listeners;
        }
    }

    public static void registerAdviceListener(ClassLoader classLoader, String className, String methodName,
            String methodDesc, AdviceListener listener) {
        classLoader = wrap(classLoader);
        className = className.replace('/', '.');

        ClassLoaderAdviceListenerManager manager = adviceListenerMap.get(classLoader);

        if (manager == null) {
            manager = new ClassLoaderAdviceListenerManager();
            adviceListenerMap.put(classLoader, manager);
        }
        manager.registerAdviceListener(className, methodName, methodDesc, listener);
    }

    public static void updateAdviceListeners() {

    }

    public static List<AdviceListener> queryAdviceListeners(ClassLoader classLoader, String className,
            String methodName, String methodDesc) {
        classLoader = wrap(classLoader);
        className = className.replace('/', '.');
        ClassLoaderAdviceListenerManager manager = adviceListenerMap.get(classLoader);

        if (manager != null) {
            return manager.queryAdviceListeners(className, methodName, methodDesc);
        }

        return null;
    }

    public static void registerTraceAdviceListener(ClassLoader classLoader, String className, String owner,
            String methodName, String methodDesc, AdviceListener listener) {
        classLoader = wrap(classLoader);
        className = className.replace('/', '.');

        ClassLoaderAdviceListenerManager manager = adviceListenerMap.get(classLoader);

        if (manager == null) {
            manager = new ClassLoaderAdviceListenerManager();
            adviceListenerMap.put(classLoader, manager);
        }
        manager.registerTraceAdviceListener(className, owner, methodName, methodDesc, listener);
    }

    public static List<AdviceListener> queryTraceAdviceListeners(ClassLoader classLoader, String className,
            String owner, String methodName, String methodDesc) {
        classLoader = wrap(classLoader);
        className = className.replace('/', '.');
        ClassLoaderAdviceListenerManager manager = adviceListenerMap.get(classLoader);

        if (manager != null) {
            return manager.queryTraceAdviceListeners(className, owner, methodName, methodDesc);
        }

        return null;
    }

    private static ClassLoader wrap(ClassLoader classLoader) {
        if (classLoader != null) {
            return classLoader;
        }
        return FAKEBOOTSTRAPCLASSLOADER;
    }

    private static class FakeBootstrapClassLoader extends ClassLoader {

    }
}
