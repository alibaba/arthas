package org.example.jfranalyzerbackend.service.impl;

import org.example.jfranalyzerbackend.service.JFRAnalysisService;
import org.example.jfranalyzerbackend.vo.FlameGraph;
import org.example.jfranalyzerbackend.vo.Metadata;
import org.example.jfranalyzerbackend.entity.PerfDimensionFactory;
import org.example.jfranalyzerbackend.model.PerfDimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JFRAnalysisServiceImpl implements JFRAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(JFRAnalysisServiceImpl.class);

    @Override
    public Metadata getMetadata() {
        // 直接复用 JFRAnalyzerImpl 的 metadata 逻辑
        return new JFRAnalyzerImpl(null, null, null).metadata();
    }

    @Override
    public boolean isValidJFRFile(Path path) {
        try {
            return path != null && Files.exists(path) && Files.isRegularFile(path);
        } catch (Exception e) {
            log.warn("JFR文件校验异常: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public FlameGraph analyzeAndGenerateFlameGraph(Path path, String dimension, boolean include, List<String> taskSet, Map<String, String> options) {
        JFRAnalyzerImpl analyzer = new JFRAnalyzerImpl(path, options, null);
        return analyzer.getFlameGraph(dimension, include, taskSet);
    }

    @Override
    public List<String> getSupportedDimensions() {
        return Arrays.stream(PerfDimensionFactory.PERF_DIMENSIONS)
                .map(PerfDimension::getKey)
                .collect(Collectors.toList());
    }
} 