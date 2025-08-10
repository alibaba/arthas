
package org.example.jfranalyzerbackend.service.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.ProgressListener;
import org.example.jfranalyzerbackend.entity.ProfileDimension;
import org.example.jfranalyzerbackend.exception.ProfileAnalysisException;
import org.example.jfranalyzerbackend.extractor.*;
import org.example.jfranalyzerbackend.model.*;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;
import org.example.jfranalyzerbackend.request.AnalysisRequest;
import org.example.jfranalyzerbackend.service.JFRAnalyzer;
import org.example.jfranalyzerbackend.util.DimensionBuilder;
import org.example.jfranalyzerbackend.vo.FlameGraph;
import org.example.jfranalyzerbackend.vo.Metadata;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.example.jfranalyzerbackend.entity.PerfDimensionFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;



@SuppressWarnings("unchecked")
@Slf4j
public class JFRAnalyzerImpl implements JFRAnalyzer {

    private final ProgressListener listener;
    private final JFRAnalysisContext context;

    @Getter
    private final AnalysisResult result;

    public JFRAnalyzerImpl(Path path, Map<String, String> options, ProgressListener listener) {
        this(path, DimensionBuilder.ALL, options, listener);
    }

    public JFRAnalyzerImpl(Path path, int dimension, Map<String, String> options, ProgressListener listener) {
        AnalysisRequest request = new AnalysisRequest(path, dimension);
        this.listener = listener;
        this.context = new JFRAnalysisContext(request);
        try {
            this.result = this.execute(request);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public FlameGraph getFlameGraph(String dimension, boolean include, List<String> taskSet) {
        return createFlameGraph(ProfileDimension.of(dimension), result, include, taskSet);
    }

    @Override
    public Metadata metadata() {
        Metadata basic = new Metadata();
        basic.setPerfDimensions(PerfDimensionFactory.PERF_DIMENSIONS);
        return basic;
    }

    private FlameGraph createFlameGraph(ProfileDimension dimension, AnalysisResult result, boolean include,
                                        List<String> taskSet) {
        List<Object[]> os = new ArrayList<>();
        Map<String, Long> names = new HashMap<>();
        SymbolMap symbolTable = new SymbolMap();
        if (dimension == ProfileDimension.CPU) {
            DimensionResult<TaskCPUTime> cpuTime = result.getCpuTime();
            generateCpuTime(cpuTime, os, names, symbolTable, include, taskSet);
        } else {
            DimensionResult<? extends TaskResultBase> DimensionResult = switch (dimension) {
                case CPU_SAMPLE -> result.getCpuSample();
                case WALL_CLOCK -> result.getWallClock();
                case NATIVE_EXECUTION_SAMPLES -> result.getNativeExecutionSamples();
                case ALLOC -> result.getAllocations();
                case MEM -> result.getAllocatedMemory();
                case FILE_IO_TIME -> result.getFileIOTime();
                case FILE_READ_SIZE -> result.getFileReadSize();
                case FILE_WRITE_SIZE -> result.getFileWriteSize();
                case SOCKET_READ_TIME -> result.getSocketReadTime();
                case SOCKET_READ_SIZE -> result.getSocketReadSize();
                case SOCKET_WRITE_TIME -> result.getSocketWriteTime();
                case SOCKET_WRITE_SIZE -> result.getSocketWriteSize();
                case SYNCHRONIZATION -> result.getSynchronization();
                case THREAD_PARK -> result.getThreadPark();
                case CLASS_LOAD_COUNT -> result.getClassLoadCount();
                case CLASS_LOAD_WALL_TIME -> result.getClassLoadWallTime();
                case THREAD_SLEEP -> result.getThreadSleepTime();
                default -> throw new RuntimeException("should not reach here");
            };
            generate(DimensionResult, os, names, symbolTable, include, taskSet);
        }

        FlameGraph fg = new FlameGraph();
        fg.setData(os.toArray(new Object[0][]));
        fg.setThreadSplit(names);
        fg.setSymbolTable(symbolTable.getReverseMap());
        System.out.println(fg);
        System.out.println(fg.getSymbolTable());
        return fg;
    }

    private void generate(DimensionResult<? extends TaskResultBase> result, List<Object[]> os, Map<String, Long> names,
                          SymbolMap map, boolean include, List<String> taskSet) {
        List<? extends TaskResultBase> list = result.getList();
        Set<String> set = null;
        if (taskSet != null) {
            set = new HashSet<>(taskSet);
        }
        for (TaskResultBase ts : list) {
            if (set != null && !set.isEmpty()) {
                if (include && !set.contains(ts.getTask().getName())) {
                    continue;
                } else if (!include && set.contains(ts.getTask().getName())) {
                    continue;
                }
            }
            this.doTaskResult(ts, os, names, map);
        }
    }

    private void doTaskResult(TaskResultBase taskResult, List<Object[]> os, Map<String, Long> names, SymbolMap map) {
        Map<StackTrace, Long> samples = taskResult.getSamples();
        long total = 0;
        for (StackTrace s : samples.keySet()) {
            Frame[] frames = s.getFrames();
            String[] fs = new String[frames.length];
            for (int i = frames.length - 1, j = 0; i >= 0; i--, j++) {
                fs[j] = frames[i].toString();
            }
            Object[] o = new Object[3];
            o[0] = map.processSymbols(fs);
            o[1] = samples.get(s);
            o[2] = taskResult.getTask().getName();
            os.add(o);
            total += samples.get(s);
        }
        names.put(taskResult.getTask().getName(), total);
    }

    private static boolean isTaskNameIn(String taskName, List<String> taskList) {
        for (String name : taskList) {
            if (taskName.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private void generateCpuTime(DimensionResult<TaskCPUTime> result, List<Object[]> os,
                                 Map<String, Long> names, SymbolMap map, boolean include, List<String> taskSet) {
        List<TaskCPUTime> list = result.getList();
        for (TaskCPUTime ct : list) {
            if (taskSet != null && !taskSet.isEmpty()) {
                if (include) {
                    if (!isTaskNameIn(ct.getTask().getName(), taskSet)) {
                        continue;
                    }
                } else {
                    if (isTaskNameIn(ct.getTask().getName(), taskSet)) {
                        continue;
                    }
                }
            }

            Map<StackTrace, Long> samples = ct.getSamples();
            if (samples != null && !samples.isEmpty()) {
                long taskTotalTime = ct.getUser() + ct.getSystem();
                AtomicLong sampleCount = new AtomicLong();
                samples.values().forEach(sampleCount::addAndGet);
                long perSampleTime = taskTotalTime / sampleCount.get();

                for (StackTrace s : samples.keySet()) {
                    Frame[] frames = s.getFrames();
                    String[] fs = new String[frames.length];
                    for (int i = frames.length - 1, j = 0; i >= 0; i--, j++) {
                        fs[j] = frames[i].toString();
                    }
                    Object[] o = new Object[3];
                    o[0] = map.processSymbols(fs);
                    o[1] = samples.get(s) * perSampleTime;
                    o[2] = ct.getTask().getName();
                    os.add(o);
                }

                names.put(ct.getTask().getName(), taskTotalTime);
            }
        }
    }

    private static class SymbolMap {
        private final Map<String, Integer> map = new HashMap<>();

        String[] processSymbols(String[] fs) {
            if (fs == null || fs.length == 0) {
                return fs;
            }

            String[] result = new String[fs.length];

            synchronized (map) {
                for (int i = 0; i < fs.length; i++) {
                    String symbol = fs[i];
                    int id;
                    if (map.containsKey(symbol)) {
                        id = map.get(symbol);
                    } else {
                        id = map.size() + 1;
                        map.put(symbol, id);
                    }
                    result[i] = String.valueOf(id);
                }
            }

            return result;
        }

        Map<Integer, String> getReverseMap() {
            Map<Integer, String> reverseMap = new HashMap<>();
            map.forEach((key, value) -> reverseMap.put(value, key));
            return reverseMap;
        }
    }

    public AnalysisResult execute(AnalysisRequest request) throws ProfileAnalysisException {
        try {
            return analyze(request);
        } catch (Exception e) {
            if (e instanceof ProfileAnalysisException) {
                throw (ProfileAnalysisException) e;
            }
            throw new ProfileAnalysisException(e);
        }
    }

    private AnalysisResult analyze(AnalysisRequest request) throws Exception {
//        listener.beginTask("Analyzing", 5);
        long startTime = System.currentTimeMillis();
        AnalysisResult r = new AnalysisResult();

        IItemCollection collection = this.loadEvents(request);

        this.analyzeProblemsIfNeeded(request, collection, r);

        this.transformEvents(request, collection);

        this.sortEvents();

        this.processEvents(request, r);

        r.setProcessingTimeMillis(System.currentTimeMillis() - startTime);
        log.info(String.format("Analysis took %d milliseconds", r.getProcessingTimeMillis()));

        return r;
    }

    private void processEvents(AnalysisRequest request, AnalysisResult r) throws Exception {
//        listener.subTask("Do Extractors");
        List<RecordedEvent> events = this.context.getEvents();
        final List<Extractor> extractors = getExtractors(request);

        if (request.getParallelWorkers() > 1) {
            CountDownLatch countDownLatch = new CountDownLatch(extractors.size());
            ExecutorService es = Executors.newFixedThreadPool(request.getParallelWorkers());
            extractors.forEach(item -> es.submit(() -> {
                try {
                    doExtractorWork(events, item, r);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            }));
            countDownLatch.await();
            es.shutdown();
        } else {
            extractors.forEach(item -> {
                doExtractorWork(events, item, r);
            });
        }
//        listener.worked(1);
    }

    private void doExtractorWork(List<RecordedEvent> events, Extractor extractor, AnalysisResult r) {
        events.forEach(extractor::process);
        extractor.fillResult(r);
    }

    private List<Extractor> getExtractors(AnalysisRequest request) {
        return getExtractors(request.getDimensions());
    }

    private void sortEvents() {
//        listener.subTask("Sort Events");
        this.context.getEvents().sort(Comparator.comparing(RecordedEvent::getStartTime));
//        listener.worked(1);
    }

    private void transformEvents(AnalysisRequest request, IItemCollection collection) throws Exception {
//        listener.subTask("Transform Events");
        List<IItem> list = collection.stream().flatMap(IItemIterable::stream).collect(Collectors.toList());

        if (request.getParallelWorkers() > 1) {
            parseEventsParallel(list, request.getParallelWorkers());
        } else {
            list.forEach(this::parseEventItem);
        }

//        listener.worked(1);
    }

    private IItemCollection loadEvents(AnalysisRequest request) throws Exception {
        try {
//            listener.subTask("Load Events");
            if (request.getInput() != null) {
                return JfrLoaderToolkit.loadEvents(request.getInput().toFile());
            } else {
                return JfrLoaderToolkit.loadEvents(request.getInputStream());
            }
        } finally {
//            listener.worked(1);
        }
    }

    private void analyzeProblemsIfNeeded(AnalysisRequest request, IItemCollection collection, AnalysisResult r) {
//        listener.subTask("Analyze Problems");
        if ((request.getDimensions() & ProfileDimension.PROBLEMS.getValue()) != 0) {
            this.analyzeProblems(collection, r);
        }
//        listener.worked(1);
    }

    private void parseEventsParallel(List<IItem> list, int workers) throws Exception {
//        listener.subTask("Transform Events");
        CountDownLatch countDownLatch = new CountDownLatch(list.size());
        ExecutorService es = Executors.newFixedThreadPool(workers);
        list.forEach(item -> es.submit(() -> {
            try {
                this.parseEventItem(item);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                countDownLatch.countDown();
            }
        }));
        countDownLatch.await();
        es.shutdown();
//        listener.worked(workers);
    }

    private void analyzeProblems(IItemCollection collection, AnalysisResult r) {
        r.setProblems(new ArrayList<>());
        for (IRule rule : RuleRegistry.getRules()) {
            RunnableFuture<IResult> future;
            try {
                future = rule.createEvaluation(collection, IPreferenceValueProvider.DEFAULT_VALUES, null);
                future.run();
                IResult result = future.get();
                Severity severity = result.getSeverity();
                if (severity == Severity.WARNING) {
                    r.getProblems().add(new Problem(result.getSummary(), result.getSolution()));
                }
            } catch (Throwable t) {
                log.error("Failed to run jmc rule {}", rule.getName());
            }
        }
    }

    private void parseEventItem(IItem item) {
        RecordedEvent event = RecordedEvent.newInstance(item, this.context.getSymbols());

        synchronized (this.context.getEvents()) {
            this.context.addEvent(event);
            if (event.getActiveSetting() != null) {
                RecordedEvent.ActiveSetting activeSetting = event.getActiveSetting();
                this.context.putEventTypeId(activeSetting.eventType(), activeSetting.eventId());
                this.context.putActiveSetting(activeSetting, event);
            }
        }
    }

    private List<Extractor> getExtractors(int dimensions) {
        List<Extractor> extractors = new ArrayList<>();
        Map<Integer, Extractor> extractorMap = new HashMap<>() {
            {
                put(DimensionBuilder.CPU, new CPUTimeExtractor(context));
                put(DimensionBuilder.CPU_SAMPLE, new CPUSampleExtractor(context));
                put(DimensionBuilder.WALL_CLOCK, new WallClockExtractor(context));
                put(DimensionBuilder.NATIVE_EXECUTION_SAMPLES, new NativeExecutionExtractor(context));
                put(DimensionBuilder.ALLOC, new AllocationsExtractor(context));
                put(DimensionBuilder.MEM, new AllocatedMemoryExtractor(context));

                put(DimensionBuilder.FILE_IO_TIME, new FileIOTimeExtractor(context));
                put(DimensionBuilder.FILE_READ_SIZE, new FileReadExtractor(context));
                put(DimensionBuilder.FILE_WRITE_SIZE, new FileWriteExtractor(context));

                put(DimensionBuilder.SOCKET_READ_TIME, new SocketReadTimeExtractor(context));
                put(DimensionBuilder.SOCKET_READ_SIZE, new SocketReadSizeExtractor(context));
                put(DimensionBuilder.SOCKET_WRITE_TIME, new SocketWriteTimeExtractor(context));
                put(DimensionBuilder.SOCKET_WRITE_SIZE, new SocketWriteSizeExtractor(context));

                put(DimensionBuilder.SYNCHRONIZATION, new SynchronizationExtractor(context));
                put(DimensionBuilder.THREAD_PARK, new ThreadParkExtractor(context));

                put(DimensionBuilder.CLASS_LOAD_COUNT, new ClassLoadCountExtractor(context));
                put(DimensionBuilder.CLASS_LOAD_WALL_TIME, new ClassLoadWallTimeExtractor(context));

                put(DimensionBuilder.THREAD_SLEEP, new ThreadSleepTimeExtractor(context));
            }
        };

        extractorMap.keySet().forEach(item -> {
            if ((dimensions & item) != 0) {
                extractors.add(extractorMap.get(item));
            }
        });

        return extractors;
    }
}
