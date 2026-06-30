package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * classloader-metaspace 命令的结构化结果。
 *
 * @author Codex 2026-05-08
 */
public class ClassLoaderMetaspaceModel extends ResultModel {

    private List<Row> rows;
    private long durationMillis;
    private long periodMillis;
    private boolean verbose;

    @Override
    public String getType() {
        return "classloader-metaspace";
    }

    public List<Row> getRows() {
        return rows;
    }

    public ClassLoaderMetaspaceModel setRows(List<Row> rows) {
        this.rows = rows;
        return this;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public ClassLoaderMetaspaceModel setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
        return this;
    }

    public long getPeriodMillis() {
        return periodMillis;
    }

    public ClassLoaderMetaspaceModel setPeriodMillis(long periodMillis) {
        this.periodMillis = periodMillis;
        return this;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public ClassLoaderMetaspaceModel setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public static class Row {
        private String name;
        private String hash;
        private String type;
        private long classLoaderData;
        private long classCount;
        private long chunkSize;
        private long blockSize;
        private long hiddenBlockSize;

        public String getName() {
            return name;
        }

        public Row setName(String name) {
            this.name = name;
            return this;
        }

        public String getHash() {
            return hash;
        }

        public Row setHash(String hash) {
            this.hash = hash;
            return this;
        }

        public String getType() {
            return type;
        }

        public Row setType(String type) {
            this.type = type;
            return this;
        }

        public long getClassLoaderData() {
            return classLoaderData;
        }

        public Row setClassLoaderData(long classLoaderData) {
            this.classLoaderData = classLoaderData;
            return this;
        }

        public long getClassCount() {
            return classCount;
        }

        public Row setClassCount(long classCount) {
            this.classCount = classCount;
            return this;
        }

        public long getChunkSize() {
            return chunkSize;
        }

        public Row setChunkSize(long chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public long getBlockSize() {
            return blockSize;
        }

        public Row setBlockSize(long blockSize) {
            this.blockSize = blockSize;
            return this;
        }

        public long getHiddenBlockSize() {
            return hiddenBlockSize;
        }

        public Row setHiddenBlockSize(long hiddenBlockSize) {
            this.hiddenBlockSize = hiddenBlockSize;
            return this;
        }
    }
}
