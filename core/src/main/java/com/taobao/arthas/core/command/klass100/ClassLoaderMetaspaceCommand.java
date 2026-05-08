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
            List<Row> rows = collect(process.session().getInstrumentation(), durationMillis, periodMillis);
            rows = sortAndLimit(rows, limit);
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
        try {
            recording = new Recording();
            recording.enable(STATS_EVENT_NAME)
                    .withPeriod(Duration.ofMillis(periodMillis))
                    .withoutStackTrace();
            recording.enable(MappingEvent.class).withoutStackTrace();

            recording.start();
            emitMappings(instrumentation);
            waitForSample(durationMillis);
            recording.stop();
            recording.dump(output);
            return buildRows(readRecording(output));
        } finally {
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

    private void emitMappings(Instrumentation instrumentation) {
        Map<ClassLoader, Class<?>> anchorClasses = new IdentityHashMap<ClassLoader, Class<?>>();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz == null || clazz.isArray() || clazz.isPrimitive()) {
                continue;
            }

            ClassLoader loader = clazz.getClassLoader();
            if (loader == null || anchorClasses.containsKey(loader) || !matchesFilter(loader)) {
                continue;
            }
            anchorClasses.put(loader, clazz);
        }

        for (Map.Entry<ClassLoader, Class<?>> entry : anchorClasses.entrySet()) {
            ClassLoader loader = entry.getKey();
            MappingEvent event = new MappingEvent();
            event.anchorClass = entry.getValue();
            event.hash = ClassUtils.classLoaderHash(loader);
            event.type = loader.getClass().getName();
            event.loaderToString = safeToString(loader);
            event.commit();
        }
    }

    private boolean matchesFilter(ClassLoader loader) {
        if (hashCode != null && !hashCode.equals(ClassUtils.classLoaderHash(loader))) {
            return false;
        }
        return classLoaderClass == null || classLoaderClass.equals(loader.getClass().getName());
    }

    private RecordingData readRecording(Path recording) throws IOException {
        Map<Long, LoaderMapping> mappingByLoaderId = new HashMap<Long, LoaderMapping>();
        Map<Long, StatsRow> latestStatsByLoaderId = new HashMap<Long, StatsRow>();
        RecordingFile file = new RecordingFile(recording);
        try {
            while (file.hasMoreEvents()) {
                RecordedEvent event = file.readEvent();
                String eventName = event.getEventType().getName();
                if (MAPPING_EVENT_NAME.equals(eventName)) {
                    RecordedClass anchorClass = event.getClass("anchorClass");
                    RecordedClassLoader loader = anchorClass == null ? null : anchorClass.getClassLoader();
                    if (loader != null) {
                        mappingByLoaderId.put(loader.getId(), LoaderMapping.from(event));
                    }
                } else if (STATS_EVENT_NAME.equals(eventName)) {
                    StatsRow row = StatsRow.from(event);
                    StatsRow previous = latestStatsByLoaderId.get(row.loaderId);
                    if (previous == null || row.startTime.isAfter(previous.startTime)) {
                        latestStatsByLoaderId.put(row.loaderId, row);
                    }
                }
            }
        } finally {
            file.close();
        }
        return new RecordingData(mappingByLoaderId, latestStatsByLoaderId.values());
    }

    private List<Row> buildRows(RecordingData data) {
        List<Row> rows = new ArrayList<Row>();
        for (StatsRow stats : data.statsRows) {
            LoaderMapping mapping = data.mappingByLoaderId.get(stats.loaderId);
            if (!matchesStatsRow(stats, mapping)) {
                continue;
            }

            rows.add(new Row()
                    .setName(displayName(stats, mapping))
                    .setHash(mapping == null ? stats.hash : mapping.hash)
                    .setType(mapping == null ? stats.typeName : mapping.type)
                    .setClassLoaderData(stats.classLoaderData)
                    .setClassCount(stats.classCount)
                    .setChunkSize(stats.chunkSize)
                    .setBlockSize(stats.blockSize)
                    .setHiddenBlockSize(stats.hiddenBlockSize));
        }
        return rows;
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

    static long readHiddenBlockSize(RecordedEvent event) {
        if (event.hasField("hiddenBlockSize")) {
            return event.getLong("hiddenBlockSize");
        }
        if (event.hasField("anonymousBlockSize")) {
            return event.getLong("anonymousBlockSize");
        }
        return 0L;
    }

    private static String typeName(RecordedClassLoader loader) {
        if (loader == null || loader.getType() == null) {
            return "BootstrapClassLoader";
        }
        return loader.getType().getName().replace('/', '.');
    }

    private static class RecordingData {
        private final Map<Long, LoaderMapping> mappingByLoaderId;
        private final Collection<StatsRow> statsRows;

        private RecordingData(Map<Long, LoaderMapping> mappingByLoaderId, Collection<StatsRow> statsRows) {
            this.mappingByLoaderId = mappingByLoaderId;
            this.statsRows = statsRows;
        }
    }

    private static class LoaderMapping {
        private final String hash;
        private final String type;
        private final String loaderToString;

        private LoaderMapping(String hash, String type, String loaderToString) {
            this.hash = hash;
            this.type = type;
            this.loaderToString = loaderToString;
        }

        private static LoaderMapping from(RecordedEvent event) {
            return new LoaderMapping(
                    safeString(event, "hash"),
                    safeString(event, "type"),
                    safeString(event, "loaderToString"));
        }
    }

    private static class StatsRow {
        private final Instant startTime;
        private final long loaderId;
        private final String hash;
        private final String jfrName;
        private final String typeName;
        private final long classLoaderData;
        private final long classCount;
        private final long chunkSize;
        private final long blockSize;
        private final long hiddenBlockSize;

        private StatsRow(Instant startTime, long loaderId, String hash, String jfrName, String typeName,
                long classLoaderData, long classCount, long chunkSize, long blockSize, long hiddenBlockSize) {
            this.startTime = startTime;
            this.loaderId = loaderId;
            this.hash = hash;
            this.jfrName = jfrName;
            this.typeName = typeName;
            this.classLoaderData = classLoaderData;
            this.classCount = classCount;
            this.chunkSize = chunkSize;
            this.blockSize = blockSize;
            this.hiddenBlockSize = hiddenBlockSize;
        }

        private static StatsRow from(RecordedEvent event) {
            RecordedClassLoader loader = event.getValue("classLoader");
            long loaderId = loader == null ? 0 : loader.getId();
            String name = loader == null ? "BootstrapClassLoader" : loader.getName();
            return new StatsRow(
                    event.getStartTime(),
                    loaderId,
                    loader == null ? "null" : null,
                    name,
                    typeName(loader),
                    event.getLong("classLoaderData"),
                    event.getLong("classCount"),
                    event.getLong("chunkSize"),
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
    }
}
