package org.example.jfranalyzerbackend.service;

import org.example.jfranalyzerbackend.vo.FlameGraph;
import org.example.jfranalyzerbackend.vo.Metadata;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * JFR分析服务接口
 * 定义JFR文件分析相关的核心业务方法
 */
public interface JFRAnalysisService {
    /**
     * 获取分析元数据
     */
    Metadata retrieveAnalysisMetadata();
    
    /**
     * 验证JFR文件完整性
     */
    boolean validateJFRFileIntegrity(Path filePath);
    
    /**
     * 执行分析并生成火焰图
     */
    FlameGraph performAnalysisAndGenerateFlameGraph(Path filePath, String analysisDimension, 
                                                   boolean includeTasks, List<String> taskFilter, 
                                                   Map<String, String> analysisOptions);
    
    /**
     * 获取支持的分析维度列表
     */
    List<String> retrieveSupportedAnalysisDimensions();
    
    // 向后兼容的方法
    Metadata getMetadata();
    boolean isValidJFRFile(Path path);
    FlameGraph analyzeAndGenerateFlameGraph(Path path, String dimension, boolean include, List<String> taskSet, Map<String, String> options);
    List<String> getSupportedDimensions();
}
