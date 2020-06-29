package com.taobao.arthas.core.util.affect;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.util.ClassLoaderUtils;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * 增强影响范围<br/>
 * 统计影响类/方法/耗时
 * Created by vlinux on 15/5/19.
 * @author hengyunabc 2020-06-01
 */
public final class EnhancerAffect extends Affect {

    private final AtomicInteger cCnt = new AtomicInteger();
    private final AtomicInteger mCnt = new AtomicInteger();
    private ClassFileTransformer transformer;
    private long listenerId;

    private Throwable throwable;

    /**
     * dumpClass的文件存放集合
     */
    private final Collection<File> classDumpFiles = new ArrayList<File>();

    private final List<String> methods = new ArrayList<String>();

    public EnhancerAffect() {
    }

    /**
     * 影响类统计
     *
     * @param cc 类影响计数
     * @return 当前影响类个数
     */
    public int cCnt(int cc) {
        return cCnt.addAndGet(cc);
    }

    /**
     * 影响方法统计
     *
     * @param mc 方法影响计数
     * @return 当前影响方法个数
     */
    public int mCnt(int mc) {
        return mCnt.addAndGet(mc);
    }

    /**
     * 记录影响的函数，并增加计数
     * @param mc
     * @return
     */
    public int addMethodAndCount(ClassLoader classLoader, String clazz, String method, String methodDesc) {
        this.methods.add(ClassLoaderUtils.classLoaderHash(classLoader) + "|" + clazz.replace('/', '.') + "#" + method + "|" + methodDesc);
        return mCnt.addAndGet(1);
    }

    /**
     * 获取影响类个数
     *
     * @return 影响类个数
     */
    public int cCnt() {
        return cCnt.get();
    }

    /**
     * 获取影响方法个数
     *
     * @return 影响方法个数
     */
    public int mCnt() {
        return mCnt.get();
    }

    public void addClassDumpFile(File file) {
        classDumpFiles.add(file);
    }

    public ClassFileTransformer getTransformer() {
        return transformer;
    }

    public void setTransformer(ClassFileTransformer transformer) {
        this.transformer = transformer;
    }

    public long getListenerId() {
        return listenerId;
    }

    public void setListenerId(long listenerId) {
        this.listenerId = listenerId;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public Collection<File> getClassDumpFiles() {
        return classDumpFiles;
    }

    public List<String> getMethods() {
        return methods;
    }

    @Override
    public String toString() {
        //TODO removing EnhancerAffect.toString(), replace with ViewRenderUtil.renderEnhancerAffect()
        final StringBuilder infoSB = new StringBuilder();
        if (GlobalOptions.isDump
                && !classDumpFiles.isEmpty()) {

            for (File classDumpFile : classDumpFiles) {
                infoSB.append("[dump: ").append(classDumpFile.getAbsoluteFile()).append("]\n");
            }
        }

        if (GlobalOptions.verbose && !methods.isEmpty()) {
            for (String method : methods) {
                infoSB.append("[Affect method: ").append(method).append("]\n");
            }
        }
        infoSB.append(format("Affect(class count: %d , method count: %d) cost in %s ms, listenerId: %d",
                cCnt(),
                mCnt(),
                cost(),
                listenerId));
        if (this.throwable != null) {
            infoSB.append("\nEnhance error! exception: " + this.throwable);
        }
        return infoSB.toString();
    }

}
