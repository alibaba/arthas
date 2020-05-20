package com.taobao.arthas.core.util.affect;

import com.taobao.arthas.core.GlobalOptions;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * 增强影响范围<br/>
 * 统计影响类/方法/耗时
 * Created by vlinux on 15/5/19.
 */
public final class EnhancerAffect extends Affect {

    private final AtomicInteger cCnt = new AtomicInteger();
    private final AtomicInteger mCnt = new AtomicInteger();
    private ClassFileTransformer transformer;
    /**
     * dumpClass的文件存放集合
     */
    private final Collection<File> classDumpFiles = new ArrayList<File>();

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

    /**
     * 获取dump的Class文件集合
     *
     * @return classDumpList
     */
    public Collection<File> getClassDumpFiles() {
        return classDumpFiles;
    }

    public ClassFileTransformer getTransformer() {
        return transformer;
    }

    public void setTransformer(ClassFileTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public String toString() {
        final StringBuilder infoSB = new StringBuilder();
        if (GlobalOptions.isDump
                && !classDumpFiles.isEmpty()) {

            for (File classDumpFile : classDumpFiles) {
                infoSB.append("[dump: ").append(classDumpFile.getAbsoluteFile()).append("]\n");
            }
        }
        infoSB.append(format("Affect(class count:%d , method count:%d) cost in %s ms.",
                cCnt(),
                mCnt(),
                cost()));
        return infoSB.toString();
    }

}
