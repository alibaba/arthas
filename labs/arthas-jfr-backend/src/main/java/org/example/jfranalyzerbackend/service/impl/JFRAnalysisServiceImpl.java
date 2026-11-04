package org.example.jfranalyzerbackend.service.impl;

import org.example.jfranalyzerbackend.service.JFRAnalysisService;
import org.example.jfranalyzerbackend.util.PathSecurityUtil;
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

/**
 * JFR分析服务实现类
 * 提供JFR文件分析、火焰图生成和元数据获取等核心功能
 */
@Service
public class JFRAnalysisServiceImpl implements JFRAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(JFRAnalysisServiceImpl.class);

    @Override
    public Metadata retrieveAnalysisMetadata() {
        // 复用 JFRAnalyzerImpl 的元数据获取逻辑
        return new JFRAnalyzerImpl(null, null, null).metadata();
    }

    @Override
    public boolean validateJFRFileIntegrity(Path filePath) {
        try {
            // 使用安全的路径验证方法
            return PathSecurityUtil.isValidJFRFilePath(filePath);
        } catch (Exception e) {
            logger.warn("JFR文件完整性校验失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public FlameGraph performAnalysisAndGenerateFlameGraph(Path filePath, String analysisDimension, 
                                                          boolean includeTasks, List<String> taskFilter, 
                                                          Map<String, String> analysisOptions) {
        JFRAnalyzerImpl analyzer = new JFRAnalyzerImpl(filePath, analysisOptions, null);
        return analyzer.getFlameGraph(analysisDimension, includeTasks, taskFilter);
    }

    @Override
    public List<String> retrieveSupportedAnalysisDimensions() {
        return Arrays.stream(PerfDimensionFactory.PERF_DIMENSIONS)
                .map(PerfDimension::getKey)
                .collect(Collectors.toList());
    }

    // 保持向后兼容的方法
    @Override
    public Metadata getMetadata() {
        return retrieveAnalysisMetadata();
    }

    @Override
    public boolean isValidJFRFile(Path path) {
        return validateJFRFileIntegrity(path);
    }

    @Override
    public FlameGraph analyzeAndGenerateFlameGraph(Path path, String dimension, boolean include, 
                                                  List<String> taskSet, Map<String, String> options) {
        return performAnalysisAndGenerateFlameGraph(path, dimension, include, taskSet, options);
    }

    @Override
    public List<String> getSupportedDimensions() {
        return retrieveSupportedAnalysisDimensions();
    }
} 