package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassLoaderMetaspaceModel;
import com.taobao.arthas.core.command.model.ClassLoaderMetaspaceModel.Row;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按 ClassLoader 维度统计 metaspace / class metadata 内存。
 *
 * @author Codex 2026-05-08
 */
@com.taobao.middleware.cli.annotations.Name("classloader-metaspace")
@Summary("Show ClassLoader metaspace statistics")
@Description(Constants.EXAMPLE +
        "  classloader-metaspace\n" +
        "  classloader-metaspace --classLoaderClass com.example.ModuleClassLoader\n" +
        "  classloader-metaspace -c 1a2b3c4d --duration 3s\n" +
        "  classloader-metaspace --limit 20\n" +
        "  classloader-metaspace --verbose\n" +
        Constants.WIKI + Constants.WIKI_HOME + "classloader-metaspace")
public class ClassLoaderMetaspaceCommand extends AnnotatedCommand {

    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderMetaspaceCommand.class);
    static final String STATS_EVENT_NAME = "jdk.ClassLoaderStatistics";
    static final String MAPPING_EVENT_NAME = "arthas.ClassLoaderMetaspaceMapping";
    static final long DEFAULT_DURATION_MILLIS = 2500;
    static final long DEFAULT_PERIOD_MILLIS = 500;
    private static final String DIAG_LOG_PREFIX = "[classloader-metaspace]";
    private static final int DIAG_SAMPLE_LIMIT = 3;
    private static final int FALLBACK_CANDIDATE_SAMPLE_LIMIT = 3;

    private String hashCode;
    private String classLoaderClass;
    private String duration = DEFAULT_DURATION_MILLIS + "ms";
    private String period = DEFAULT_PERIOD_MILLIS + "ms";
    private Integer limit;
    private boolean verbose;
    private volatile boolean interrupted;

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special ClassLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special ClassLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Option(longName = "duration")
    @Description("JFR sample duration, support ms/s/m, 2500ms by default.")
    public void setDuration(String duration) {
        this.duration = duration;
    }

    @Option(longName = "period")
    @Description("JFR ClassLoaderStatistics period, support ms/s/m, 500ms by default.")
    public void setPeriod(String period) {
        this.period = period;
    }

    @Option(longName = "limit")
    @Description("Maximum rows to display, unlimited by default.")
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    @Option(shortName = "v", longName = "verbose", flag = true)
    @Description("Show verbose columns, including classLoaderData, hiddenBlockSize and type.")
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void process(CommandProcess process) {
        process.interruptHandler(new ClassLoaderMetaspaceInterruptHandler(this));

        long durationMillis;
        long periodMillis;
        try {
            durationMillis = parseTimeMillis(duration, "duration");
            periodMillis = parseTimeMillis(period, "period");
        } catch (IllegalArgumentException e) {
            process.end(-1, e.getMessage());
            return;
        }
        if (limit != null && limit <= 0) {
            process.end(-1, "limit must be greater than 0.");
            return;
        }

        try {
            logger.debug("{} command start, durationMillis={}, periodMillis={}, hashFilter={}, classLoaderClassFilter={}, limit={}, verbose={}",
                    DIAG_LOG_PREFIX, durationMillis, periodMillis, hashCode, classLoaderClass, limit, verbose);
            List<Row> rows = collect(process.session().getInstrumentation(), durationMillis, periodMillis);
            rows = sortAndLimit(rows, limit);
            logger.debug("{} command finish, rowsAfterLimit={}", DIAG_LOG_PREFIX, rows.size());
            RowAffect affect = new RowAffect();
            affect.rCnt(rows.size());
            process.appendResult(new ClassLoaderMetaspaceModel()
                    .setRows(rows)
                    .setDurationMillis(durationMillis)
                    .setPeriodMillis(periodMillis)
                    .setVerbose(verbose));
            process.appendResult(new RowAffectModel(affect));
            process.end();
        } catch (InterruptedException e) {
            process.end(-1, "Processing has been interrupted");
        } catch (Throwable e) {
            logger.warn("classloader-metaspace failed", e);
            process.end(-1, "classloader-metaspace failed: " + e.getMessage());
        }
    }

    private List<Row> collect(Instrumentation instrumentation, long durationMillis, long periodMillis)
            throws Exception {
        Path output = Files.createTempFile("arthas-classloader-metaspace-", ".jfr");
        Recording recording = null;
        long startNanos = System.nanoTime();
        try {
            recording = new Recording();
            recording.enable(STATS_EVENT_NAME)
                    .withPeriod(Duration.ofMillis(periodMillis))
                    .withoutStackTrace();
            recording.enable(MappingEvent.class).withoutStackTrace();

            recording.start();
            MappingSummary mappingSummary = emitMappings(instrumentation);
            logMappingSummary(mappingSummary);
            waitForSample(durationMillis);
            recording.stop();
            recording.dump(output);
            long recordingSize = safeFileSize(output);
            RecordingData recordingData = readRecording(output);
            logRecordingSummary(recordingData, recordingSize);
            BuildResult buildResult = buildRows(recordingData);
            logBuildResult(buildResult);
            return buildResult.rows;
        } finally {
            logger.debug("{} collection finish, costMillis={}", DIAG_LOG_PREFIX,
                    (System.nanoTime() - startNanos) / 1000000L);
            if (recording != null) {
                try {
                    recording.close();
                } catch (Throwable e) {
                    logger.debug("close JFR recording failed", e);
                }
            }
            try {
                Files.deleteIfExists(output);
            } catch (IOException e) {
                logger.debug("delete temp JFR file failed: {}", output, e);
            }
        }
    }

    private void waitForSample(long durationMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + durationMillis;
        while (System.currentTimeMillis() < deadline) {
            if (interrupted) {
                throw new InterruptedException();
            }
            long sleepMillis = Math.min(100, deadline - System.currentTimeMillis());
            if (sleepMillis > 0) {
                Thread.sleep(sleepMillis);
            }
        }
    }

    private MappingSummary emitMappings(Instrumentation instrumentation) {
        Map<ClassLoader, Class<?>> anchorClasses = new IdentityHashMap<ClassLoader, Class<?>>();
        Map<ClassLoader, Long> loadedClassCountByLoader = new IdentityHashMap<ClassLoader, Long>();
        MappingSummary summary = new MappingSummary();
        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        summary.loadedClassCount = allLoadedClasses.length;
        for (Class<?> clazz : allLoadedClasses) {
            if (clazz == null) {
                summary.nullClassCount++;
                continue;
            }
            if (clazz.isArray()) {
                summary.arrayClassCount++;
            }
            if (clazz.isPrimitive()) {
                summary.primitiveClassCount++;
                continue;
            }

            ClassLoader loader = clazz.getClassLoader();
            if (loader == null) {
                summary.bootstrapClassCount++;
                continue;
            }
            // 线上 HotSpot/JFR 的 ClassLoaderStatistics 计数口径需要和 classCount + hiddenClassCount 对齐，
            // 这里有意把数组类计入 fallback 使用的 loadedClassCount；数组类只是不适合作为 JFR anchorClass。
            Long loadedClassCount = loadedClassCountByLoader.get(loader);
            loadedClassCountByLoader.put(loader, loadedClassCount == null ? 1L : loadedClassCount + 1L);
            if (clazz.isArray()) {
                continue;
            }
            if (anchorClasses.containsKey(loader)) {
                summary.duplicateLoaderClassCount++;
                continue;
            }
            // Mapping 必须覆盖所有 ClassLoader。命令过滤只在输出阶段生效，否则 hash filter 会让
            // fallback 候选集不完整，并可能把同 type/count 的其它 stats row 误映射到目标 hash。
            anchorClasses.put(loader, clazz);
        }
        summary.candidateLoaderCount = anchorClasses.size();

        for (Map.Entry<ClassLoader, Class<?>> entry : anchorClasses.entrySet()) {
            ClassLoader loader = entry.getKey();
            MappingEvent event = new MappingEvent();
            event.anchorClass = entry.getValue();
            event.hash = ClassUtils.classLoaderHash(loader);
            event.type = loader.getClass().getName();
            event.loaderToString = safeToString(loader);
            event.loadedClassCount = loadedClassCountByLoader.get(loader);
            event.commit();
            summary.emittedMappingCount++;
        }
        return summary;
    }

    private RecordingData readRecording(Path recording) throws IOException {
        Map<Long, LoaderMapping> mappingByLoaderId = new HashMap<Long, LoaderMapping>();
        List<LoaderMapping> fallbackMappings = new ArrayList<LoaderMapping>();
        List<String> mappingEventsWithoutLoaderSamples = new ArrayList<String>();
        Map<Long, StatsRow> latestStatsByLoaderId = new HashMap<Long, StatsRow>();
        long mappingEventCount = 0;
        long mappingEventWithoutLoaderCount = 0;
        long statsEventCount = 0;
        long duplicateStatsRowCount = 0;
        RecordingFile file = new RecordingFile(recording);
        try {
            while (file.hasMoreEvents()) {
                RecordedEvent event = file.readEvent();
                String eventName = event.getEventType().getName();
                if (MAPPING_EVENT_NAME.equals(eventName)) {
                    mappingEventCount++;
                    RecordedClass anchorClass = event.getClass("anchorClass");
                    RecordedClassLoader loader = anchorClass == null ? null : anchorClass.getClassLoader();
                    LoaderMapping loaderMapping = LoaderMapping.from(event);
                    fallbackMappings.add(loaderMapping);
                    if (loader != null) {
                        mappingByLoaderId.put(loader.getId(), loaderMapping);
                    } else {
                        mappingEventWithoutLoaderCount++;
                        addSample(mappingEventsWithoutLoaderSamples, describeMapping(loaderMapping));
                    }
                } else if (STATS_EVENT_NAME.equals(eventName)) {
                    statsEventCount++;
                    StatsRow row = StatsRow.from(event);
                    StatsRow previous = latestStatsByLoaderId.get(row.loaderId);
                    if (previous != null) {
                        duplicateStatsRowCount++;
                    }
                    if (previous == null || row.startTime.isAfter(previous.startTime)) {
                        latestStatsByLoaderId.put(row.loaderId, row);
                    }
                }
            }
        } finally {
            file.close();
        }
        return new RecordingData(mappingByLoaderId, fallbackMappings, mappingEventsWithoutLoaderSamples,
                latestStatsByLoaderId.values(), mappingEventCount, mappingEventWithoutLoaderCount, statsEventCount,
                duplicateStatsRowCount);
    }

    private BuildResult buildRows(RecordingData data) {
        BuildResult result = new BuildResult();
        for (StatsRow stats : data.statsRows) {
            LoaderMapping mapping = data.mappingByLoaderId.get(stats.loaderId);
            LoaderMapping parentMapping = data.mappingByLoaderId.get(stats.parentLoaderId);
            if (mapping == null && stats.loaderId != 0) {
                result.statsRowsWithoutMapping++;
                FallbackDiagnostics fallbackDiagnostics = diagnoseFallbackMapping(stats, data.fallbackMappings);
                if (fallbackDiagnostics.mapping != null) {
                    mapping = fallbackDiagnostics.mapping;
                    result.inferredMappingRows++;
                    addSample(result.inferredMappingSamples,
                            describeStatsRow(stats, mapping, parentMapping) + ", fallbackDiagnostics="
                                    + fallbackDiagnostics);
                } else {
                    result.unresolvedStatsRowsWithoutMapping++;
                    addSample(result.unresolvedStatsRowsWithoutMappingSamples,
                            describeStatsRow(stats, mapping, parentMapping) + ", fallbackDiagnostics="
                                    + fallbackDiagnostics);
                }
            }
            if (!matchesStatsRow(stats, mapping)) {
                result.filteredRows++;
                if (matchesParentFilter(stats, parentMapping)) {
                    result.parentMatchedFilterRows++;
                    addSample(result.parentMatchedFilterSamples, describeStatsRow(stats, mapping, parentMapping));
                }
                continue;
            }

            if (mapping == null) {
                result.outputRowsWithoutMapping++;
            }
            result.rows.add(new Row()
                    .setName(displayName(stats, mapping))
                    .setHash(mapping == null ? stats.hash : mapping.hash)
                    .setType(mapping == null ? stats.typeName : mapping.type)
                    .setClassLoaderData(stats.classLoaderData)
                    .setClassCount(stats.classCount)
                    .setChunkSize(stats.chunkSize)
                    .setBlockSize(stats.blockSize)
                    .setHiddenBlockSize(stats.hiddenBlockSize));
        }
        return result;
    }

    static LoaderMapping findUniqueFallbackMapping(StatsRow stats, List<LoaderMapping> fallbackMappings) {
        return diagnoseFallbackMapping(stats, fallbackMappings).mapping;
    }

    private static FallbackDiagnostics diagnoseFallbackMapping(StatsRow stats, List<LoaderMapping> fallbackMappings) {
        LoaderMapping matched = null;
        FallbackDiagnostics diagnostics = new FallbackDiagnostics(stats);
        for (LoaderMapping mapping : fallbackMappings) {
            diagnostics.totalCandidateCount++;
            if (!mapping.matchesType(stats.typeName)) {
                continue;
            }

            diagnostics.sameTypeCandidateCount++;
            diagnostics.addNearestCandidate(mapping);
            if (mapping.loadedClassCount == stats.totalClassCountForMapping()) {
                diagnostics.exactClassCountCandidateCount++;
                diagnostics.addExactCandidate(mapping);
                if (matched != null) {
                    diagnostics.ambiguous = true;
                    diagnostics.mapping = null;
                    continue;
                }
                matched = mapping;
                diagnostics.mapping = mapping;
            }
        }
        if (diagnostics.ambiguous) {
            diagnostics.mapping = null;
        }
        return diagnostics;
    }

    private boolean matchesStatsRow(StatsRow stats, LoaderMapping mapping) {
        if (hashCode != null) {
            return mapping != null && hashCode.equals(mapping.hash);
        }
        if (classLoaderClass != null) {
            if (mapping != null) {
                return classLoaderClass.equals(mapping.type);
            }
            return classLoaderClass.equals(stats.typeName);
        }
        return true;
    }

    private boolean matchesParentFilter(StatsRow stats, LoaderMapping parentMapping) {
        if (hashCode != null) {
            return parentMapping != null && hashCode.equals(parentMapping.hash);
        }
        if (classLoaderClass != null) {
            if (parentMapping != null) {
                return classLoaderClass.equals(parentMapping.type);
            }
            return classLoaderClass.equals(stats.parentTypeName);
        }
        return false;
    }

    private static String displayName(StatsRow stats, LoaderMapping mapping) {
        return selectDisplayName(
                stats.jfrName,
                mapping == null ? null : mapping.loaderToString,
                stats.typeName);
    }

    static String selectDisplayName(String jfrName, String loaderToString, String typeName) {
        if (!StringUtils.isBlank(jfrName)) {
            return jfrName;
        }
        if (!StringUtils.isBlank(loaderToString)) {
            return loaderToString;
        }
        return typeName;
    }

    static List<Row> sortAndLimit(List<Row> rows, Integer limit) {
        List<Row> sorted = new ArrayList<Row>(rows);
        Collections.sort(sorted, new Comparator<Row>() {
            @Override
            public int compare(Row o1, Row o2) {
                int chunkCompare = Long.compare(o2.getChunkSize(), o1.getChunkSize());
                if (chunkCompare != 0) {
                    return chunkCompare;
                }
                int blockCompare = Long.compare(o2.getBlockSize(), o1.getBlockSize());
                if (blockCompare != 0) {
                    return blockCompare;
                }
                return safeName(o1).compareTo(safeName(o2));
            }
        });
        if (limit == null || sorted.size() <= limit) {
            return sorted;
        }
        return new ArrayList<Row>(sorted.subList(0, limit));
    }

    private static String safeName(Row row) {
        return row.getName() == null ? "" : row.getName();
    }

    static long parseTimeMillis(String value, String optionName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(optionName + " must not be blank.");
        }

        String text = value.trim().toLowerCase();
        long multiplier = 1;
        if (text.endsWith("ms")) {
            text = text.substring(0, text.length() - 2).trim();
        } else if (text.endsWith("s")) {
            text = text.substring(0, text.length() - 1).trim();
            multiplier = 1000;
        } else if (text.endsWith("m")) {
            text = text.substring(0, text.length() - 1).trim();
            multiplier = 60 * 1000;
        }

        try {
            long millis = Long.parseLong(text) * multiplier;
            if (millis <= 0) {
                throw new IllegalArgumentException(optionName + " must be greater than 0.");
            }
            return millis;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(optionName + " is invalid: " + value);
        }
    }

    private void logMappingSummary(MappingSummary summary) {
        logger.debug("{} mapping summary, loadedClasses={}, candidateLoaders={}, emittedMappings={}, bootstrapClasses={}, arrayClasses={}, primitiveClasses={}, duplicateLoaderClasses={}",
                DIAG_LOG_PREFIX, summary.loadedClassCount, summary.candidateLoaderCount, summary.emittedMappingCount,
                summary.bootstrapClassCount, summary.arrayClassCount, summary.primitiveClassCount,
                summary.duplicateLoaderClassCount);
    }

    private void logRecordingSummary(RecordingData data, long recordingSize) {
        logger.debug("{} recording summary, fileSizeBytes={}, statsEvents={}, distinctStatsRows={}, mappingEvents={}, fallbackMappings={}, mappedLoaderIds={}, mappingEventsWithoutLoader={}, duplicateStatsRows={}",
                DIAG_LOG_PREFIX, recordingSize, data.statsEventCount, data.statsRows.size(), data.mappingEventCount,
                data.fallbackMappings.size(), data.mappingByLoaderId.size(), data.mappingEventWithoutLoaderCount,
                data.duplicateStatsRowCount);
        if (data.mappingEventWithoutLoaderCount > 0) {
            logger.debug("{} mapping events without RecordedClassLoader. These events can still be used as fallback candidates. samples={}",
                    DIAG_LOG_PREFIX, data.mappingEventsWithoutLoaderSamples);
        }
    }

    private void logBuildResult(BuildResult result) {
        logger.debug("{} build summary, outputRows={}, filteredRows={}, statsRowsWithoutMapping={}, inferredMappingRows={}, unresolvedStatsRowsWithoutMapping={}, outputRowsWithoutMapping={}, parentMatchedFilterRows={}",
                DIAG_LOG_PREFIX, result.rows.size(), result.filteredRows, result.statsRowsWithoutMapping,
                result.inferredMappingRows, result.unresolvedStatsRowsWithoutMapping, result.outputRowsWithoutMapping,
                result.parentMatchedFilterRows);
        if (result.inferredMappingRows > 0) {
            logger.debug("{} inferred Arthas hash mapping for JFR stats rows by exact type and totalClassCount. samples={}",
                    DIAG_LOG_PREFIX, result.inferredMappingSamples);
        }
        if (result.unresolvedStatsRowsWithoutMapping > 0) {
            if ((hashCode == null && classLoaderClass == null) || result.outputRowsWithoutMapping > 0) {
                logger.warn("{} found JFR stats rows without Arthas hash mapping. hashFilter={}, classLoaderClassFilter={}, samples={}",
                        DIAG_LOG_PREFIX, hashCode, classLoaderClass,
                        result.unresolvedStatsRowsWithoutMappingSamples);
            } else {
                logger.debug("{} found JFR stats rows without Arthas hash mapping. This can be expected when filters suppress non-target mapping events. hashFilter={}, classLoaderClassFilter={}, samples={}",
                        DIAG_LOG_PREFIX, hashCode, classLoaderClass,
                        result.unresolvedStatsRowsWithoutMappingSamples);
            }
        }
        if (result.parentMatchedFilterRows > 0) {
            logger.warn("{} filtered JFR rows whose parentClassLoader matched the filter, but classLoader itself did not. The metaspace row belongs to the child ClassLoaderData. samples={}",
                    DIAG_LOG_PREFIX, result.parentMatchedFilterSamples);
        }
    }

    private static long safeFileSize(Path output) {
        try {
            return Files.size(output);
        } catch (Throwable e) {
            return -1L;
        }
    }

    private static void addSample(List<String> samples, String sample) {
        if (samples.size() < DIAG_SAMPLE_LIMIT) {
            samples.add(sample);
        }
    }

    private static String describeStatsRow(StatsRow stats, LoaderMapping mapping, LoaderMapping parentMapping) {
        return "loaderId=" + stats.loaderId
                + ", name=" + stats.jfrName
                + ", type=" + stats.typeName
                + ", parentLoaderId=" + stats.parentLoaderId
                + ", parentName=" + stats.parentName
                + ", parentType=" + stats.parentTypeName
                + ", classLoaderData=" + String.format("0x%016x", stats.classLoaderData)
                + ", classes=" + stats.classCount
                + ", hiddenClasses=" + stats.hiddenClassCount
                + ", totalClassesForMapping=" + stats.totalClassCountForMapping()
                + ", chunkSize=" + stats.chunkSize
                + ", hiddenChunkSize=" + stats.hiddenChunkSize
                + ", blockSize=" + stats.blockSize
                + ", hiddenBlockSize=" + stats.hiddenBlockSize
                + ", mapping=" + describeMapping(mapping)
                + ", parentMapping=" + describeMapping(parentMapping);
    }

    private static String describeMapping(LoaderMapping mapping) {
        if (mapping == null) {
            return "null";
        }
        return "{hash=" + mapping.hash + ", type=" + mapping.type + ", name=" + mapping.loaderToString
                + ", loadedClassCount=" + mapping.loadedClassCount + "}";
    }

    private static String safeToString(ClassLoader loader) {
        try {
            return loader == null ? "BootstrapClassLoader" : loader.toString();
        } catch (Throwable e) {
            return loader == null ? "BootstrapClassLoader" : loader.getClass().getName();
        }
    }

    private static String safeString(RecordedEvent event, String fieldName) {
        try {
            return event.getString(fieldName);
        } catch (Throwable e) {
            return null;
        }
    }

    private static long safeLong(RecordedEvent event, String fieldName) {
        try {
            if (event.hasField(fieldName)) {
                return event.getLong(fieldName);
            }
        } catch (Throwable e) {
            return -1L;
        }
        return -1L;
    }

    static long readHiddenClassCount(RecordedEvent event) {
        return readLongWithFallback(event, "hiddenClassCount", "anonymousClassCount");
    }

    static long readHiddenChunkSize(RecordedEvent event) {
        return readLongWithFallback(event, "hiddenChunkSize", "anonymousChunkSize");
    }

    static long readHiddenBlockSize(RecordedEvent event) {
        return readLongWithFallback(event, "hiddenBlockSize", "anonymousBlockSize");
    }

    private static long readLongWithFallback(RecordedEvent event, String primaryFieldName, String fallbackFieldName) {
        if (event.hasField(primaryFieldName)) {
            return event.getLong(primaryFieldName);
        }
        if (event.hasField(fallbackFieldName)) {
            return event.getLong(fallbackFieldName);
        }
        return 0L;
    }

    private static String typeName(RecordedClassLoader loader) {
        if (loader == null || loader.getType() == null) {
            return "BootstrapClassLoader";
        }
        return loader.getType().getName().replace('/', '.');
    }

    private static String nullableTypeName(RecordedClassLoader loader) {
        if (loader == null || loader.getType() == null) {
            return null;
        }
        return loader.getType().getName().replace('/', '.');
    }

    private static String classLoaderName(RecordedClassLoader loader) {
        if (loader == null) {
            return null;
        }
        try {
            return loader.getName();
        } catch (Throwable e) {
            return null;
        }
    }

    private static RecordedClassLoader recordedClassLoader(RecordedEvent event, String fieldName) {
        try {
            if (event.hasField(fieldName)) {
                return event.getValue(fieldName);
            }
        } catch (Throwable e) {
            return null;
        }
        return null;
    }

    private static class MappingSummary {
        private long loadedClassCount;
        private long nullClassCount;
        private long arrayClassCount;
        private long primitiveClassCount;
        private long bootstrapClassCount;
        private long duplicateLoaderClassCount;
        private long candidateLoaderCount;
        private long emittedMappingCount;
    }

    private static class RecordingData {
        private final Map<Long, LoaderMapping> mappingByLoaderId;
        private final List<LoaderMapping> fallbackMappings;
        private final List<String> mappingEventsWithoutLoaderSamples;
        private final Collection<StatsRow> statsRows;
        private final long mappingEventCount;
        private final long mappingEventWithoutLoaderCount;
        private final long statsEventCount;
        private final long duplicateStatsRowCount;

        private RecordingData(Map<Long, LoaderMapping> mappingByLoaderId, List<LoaderMapping> fallbackMappings,
                List<String> mappingEventsWithoutLoaderSamples, Collection<StatsRow> statsRows,
                long mappingEventCount, long mappingEventWithoutLoaderCount, long statsEventCount,
                long duplicateStatsRowCount) {
            this.mappingByLoaderId = mappingByLoaderId;
            this.fallbackMappings = fallbackMappings;
            this.mappingEventsWithoutLoaderSamples = mappingEventsWithoutLoaderSamples;
            this.statsRows = statsRows;
            this.mappingEventCount = mappingEventCount;
            this.mappingEventWithoutLoaderCount = mappingEventWithoutLoaderCount;
            this.statsEventCount = statsEventCount;
            this.duplicateStatsRowCount = duplicateStatsRowCount;
        }
    }

    private static class BuildResult {
        private final List<Row> rows = new ArrayList<Row>();
        private long filteredRows;
        private long statsRowsWithoutMapping;
        private long inferredMappingRows;
        private long unresolvedStatsRowsWithoutMapping;
        private long outputRowsWithoutMapping;
        private long parentMatchedFilterRows;
        private final List<String> inferredMappingSamples = new ArrayList<String>();
        private final List<String> unresolvedStatsRowsWithoutMappingSamples = new ArrayList<String>();
        private final List<String> parentMatchedFilterSamples = new ArrayList<String>();
    }

    static class LoaderMapping {
        private final String hash;
        private final String type;
        private final String loaderToString;
        private final long loadedClassCount;

        LoaderMapping(String hash, String type, String loaderToString, long loadedClassCount) {
            this.hash = hash;
            this.type = type;
            this.loaderToString = loaderToString;
            this.loadedClassCount = loadedClassCount;
        }

        private static LoaderMapping from(RecordedEvent event) {
            return new LoaderMapping(
                    safeString(event, "hash"),
                    safeString(event, "type"),
                    safeString(event, "loaderToString"),
                    safeLong(event, "loadedClassCount"));
        }

        private boolean matchesType(String typeName) {
            return type != null && type.equals(typeName);
        }

        private long classCountDelta(long classCount) {
            if (loadedClassCount < 0) {
                return Long.MAX_VALUE;
            }
            return Math.abs(loadedClassCount - classCount);
        }
    }

    private static class FallbackDiagnostics {
        private final String typeName;
        private final long classCount;
        private final long hiddenClassCount;
        private final long totalClassCountForMapping;
        private LoaderMapping mapping;
        private long totalCandidateCount;
        private long sameTypeCandidateCount;
        private long exactClassCountCandidateCount;
        private boolean ambiguous;
        private final List<String> exactCandidateSamples = new ArrayList<String>();
        private final List<LoaderMapping> nearestCandidates = new ArrayList<LoaderMapping>();

        private FallbackDiagnostics(StatsRow stats) {
            this.typeName = stats.typeName;
            this.classCount = stats.classCount;
            this.hiddenClassCount = stats.hiddenClassCount;
            this.totalClassCountForMapping = stats.totalClassCountForMapping();
        }

        private void addExactCandidate(LoaderMapping mapping) {
            addSample(exactCandidateSamples, describeFallbackCandidate(mapping));
        }

        private void addNearestCandidate(LoaderMapping mapping) {
            int insertIndex = 0;
            while (insertIndex < nearestCandidates.size()
                    && compareFallbackCandidate(mapping, nearestCandidates.get(insertIndex),
                            totalClassCountForMapping) >= 0) {
                insertIndex++;
            }
            nearestCandidates.add(insertIndex, mapping);
            if (nearestCandidates.size() > FALLBACK_CANDIDATE_SAMPLE_LIMIT) {
                nearestCandidates.remove(nearestCandidates.size() - 1);
            }
        }

        @Override
        public String toString() {
            return "{type=" + typeName
                    + ", classCount=" + classCount
                    + ", hiddenClassCount=" + hiddenClassCount
                    + ", totalClassCountForMapping=" + totalClassCountForMapping
                    + ", totalCandidateCount=" + totalCandidateCount
                    + ", sameTypeCandidateCount=" + sameTypeCandidateCount
                    + ", exactTotalClassCountCandidateCount=" + exactClassCountCandidateCount
                    + ", ambiguous=" + ambiguous
                    + ", exactCandidates=" + exactCandidateSamples
                    + ", nearestSameTypeCandidates=" + describeFallbackCandidates(nearestCandidates,
                            totalClassCountForMapping)
                    + "}";
        }
    }

    private static int compareFallbackCandidate(LoaderMapping left, LoaderMapping right, long classCount) {
        int deltaCompare = Long.compare(left.classCountDelta(classCount), right.classCountDelta(classCount));
        if (deltaCompare != 0) {
            return deltaCompare;
        }
        int countCompare = Long.compare(left.loadedClassCount, right.loadedClassCount);
        if (countCompare != 0) {
            return countCompare;
        }
        return safeText(left.hash).compareTo(safeText(right.hash));
    }

    private static List<String> describeFallbackCandidates(List<LoaderMapping> candidates, long classCount) {
        List<String> descriptions = new ArrayList<String>(candidates.size());
        for (LoaderMapping candidate : candidates) {
            descriptions.add(describeFallbackCandidate(candidate) + ", delta="
                    + candidate.classCountDelta(classCount));
        }
        return descriptions;
    }

    private static String describeFallbackCandidate(LoaderMapping mapping) {
        if (mapping == null) {
            return "null";
        }
        return "{hash=" + mapping.hash + ", type=" + mapping.type + ", name=" + mapping.loaderToString
                + ", loadedClassCount=" + mapping.loadedClassCount + "}";
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    static class StatsRow {
        private final Instant startTime;
        private final long loaderId;
        private final long parentLoaderId;
        private final String hash;
        private final String jfrName;
        private final String typeName;
        private final String parentName;
        private final String parentTypeName;
        private final long classLoaderData;
        private final long classCount;
        private final long hiddenClassCount;
        private final long chunkSize;
        private final long hiddenChunkSize;
        private final long blockSize;
        private final long hiddenBlockSize;

        StatsRow(Instant startTime, long loaderId, long parentLoaderId, String hash, String jfrName,
                String typeName, String parentName, String parentTypeName, long classLoaderData, long classCount,
                long hiddenClassCount, long chunkSize, long hiddenChunkSize, long blockSize, long hiddenBlockSize) {
            this.startTime = startTime;
            this.loaderId = loaderId;
            this.parentLoaderId = parentLoaderId;
            this.hash = hash;
            this.jfrName = jfrName;
            this.typeName = typeName;
            this.parentName = parentName;
            this.parentTypeName = parentTypeName;
            this.classLoaderData = classLoaderData;
            this.classCount = classCount;
            this.hiddenClassCount = hiddenClassCount;
            this.chunkSize = chunkSize;
            this.hiddenChunkSize = hiddenChunkSize;
            this.blockSize = blockSize;
            this.hiddenBlockSize = hiddenBlockSize;
        }

        private long totalClassCountForMapping() {
            return classCount + hiddenClassCount;
        }

        private static StatsRow from(RecordedEvent event) {
            RecordedClassLoader loader = recordedClassLoader(event, "classLoader");
            RecordedClassLoader parentLoader = recordedClassLoader(event, "parentClassLoader");
            long loaderId = loader == null ? 0 : loader.getId();
            long parentLoaderId = parentLoader == null ? 0 : parentLoader.getId();
            String name = loader == null ? "BootstrapClassLoader" : classLoaderName(loader);
            return new StatsRow(
                    event.getStartTime(),
                    loaderId,
                    parentLoaderId,
                    loader == null ? "null" : null,
                    name,
                    typeName(loader),
                    classLoaderName(parentLoader),
                    nullableTypeName(parentLoader),
                    event.getLong("classLoaderData"),
                    event.getLong("classCount"),
                    readHiddenClassCount(event),
                    event.getLong("chunkSize"),
                    readHiddenChunkSize(event),
                    event.getLong("blockSize"),
                    readHiddenBlockSize(event));
        }
    }

    private static class ClassLoaderMetaspaceInterruptHandler implements Handler<Void> {
        private final ClassLoaderMetaspaceCommand command;

        private ClassLoaderMetaspaceInterruptHandler(ClassLoaderMetaspaceCommand command) {
            this.command = command;
        }

        @Override
        public void handle(Void event) {
            command.interrupted = true;
        }
    }

    @Name(MAPPING_EVENT_NAME)
    @Label("Arthas ClassLoader Metaspace Mapping")
    public static final class MappingEvent extends Event {
        @Label("Anchor Class")
        Class<?> anchorClass;

        @Label("Hash")
        String hash;

        @Label("Type")
        String type;

        @Label("ClassLoader ToString")
        String loaderToString;

        @Label("Loaded Class Count")
        long loadedClassCount;
    }
}
