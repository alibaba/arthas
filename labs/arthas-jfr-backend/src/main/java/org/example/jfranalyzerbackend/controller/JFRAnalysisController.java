package org.example.jfranalyzerbackend.controller;

import org.example.jfranalyzerbackend.config.Result;
import org.example.jfranalyzerbackend.config.ArthasConfig;
import org.example.jfranalyzerbackend.service.JFRAnalysisService;
import org.example.jfranalyzerbackend.service.FileService;
import org.example.jfranalyzerbackend.vo.FlameGraph;
import org.example.jfranalyzerbackend.vo.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * JFR分析控制器
 * 提供JFR文件分析和火焰图生成的REST API接口
 */
@RestController
@RequestMapping("/api/jfr")
public class JFRAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(JFRAnalysisController.class);

    private final JFRAnalysisService analysisService;
    private final FileService fileManagementService;
    private final ArthasConfig configuration;

    @Autowired
    public JFRAnalysisController(JFRAnalysisService analysisService, FileService fileManagementService, ArthasConfig configuration) {
        this.analysisService = analysisService;
        this.fileManagementService = fileManagementService;
        this.configuration = configuration;
    }

    /**
     * 通过文件ID分析JFR文件并生成火焰图
     *
     * @param fileId 文件ID
     * @param dimension 分析维度
     * @param include 是否包含指定的任务集
     * @param taskSet 任务集（可选）
     * @param options 分析选项（可选）
     * @return 火焰图数据
     */
    @PostMapping("/analyze/{fileId}")
    public Result<FlameGraph> performAnalysisByFileId(
            @PathVariable Long fileId,
            @RequestParam String dimension,
            @RequestParam(defaultValue = "true") boolean include,
            @RequestParam(required = false) List<String> taskSet,
            @RequestParam(required = false) Map<String, String> options) {
        String filePath = fileManagementService.retrieveFilePathById(fileId);
        FlameGraph flameGraph = analysisService.performAnalysisAndGenerateFlameGraph(
            Paths.get(filePath), dimension, include, taskSet, options
        );
        logger.info("火焰图生成完成，数据点数量: {}", flameGraph.getData().length);
        return Result.success(flameGraph);
    }

    /**
     * 分析JFR文件并生成火焰图
     *
     * @param filePath JFR文件路径
     * @param dimension 分析维度
     * @param include 是否包含指定的任务集
     * @param taskSet 任务集（可选）
     * @param options 分析选项（可选）
     * @return 火焰图数据
     */
    @PostMapping("/analyze")
    public Result<FlameGraph> executeAnalysisByPath(
            @RequestParam String filePath,
            @RequestParam String dimension,
            @RequestParam(defaultValue = "true") boolean include,
            @RequestParam(required = false) List<String> taskSet,
            @RequestParam(required = false) Map<String, String> options) {
        FlameGraph flameGraph = analysisService.performAnalysisAndGenerateFlameGraph(
            Paths.get(filePath), dimension, include, taskSet, options
        );
        logger.info("火焰图生成完成，数据点数量: {}", flameGraph.getData().length);
        return Result.success(flameGraph);
    }

    /**
     * 获取分析元数据
     *
     * @return 元数据信息
     */
    @GetMapping("/metadata")
    public Result<Metadata> retrieveAnalysisMetadata() {
        Metadata metadata = analysisService.retrieveAnalysisMetadata();
        logger.info("=== 获取元数据 ===");
        return Result.success(metadata);
    }

    /**
     * 验证JFR文件是否有效
     *
     * @param filePath JFR文件路径
     * @return 验证结果
     */
    @GetMapping("/validate")
    public Result<Boolean> validateFileIntegrity(@RequestParam String filePath) {
        boolean isValid = analysisService.validateJFRFileIntegrity(Paths.get(filePath));
        return Result.success(isValid);
    }

    /**
     * 获取支持的分析维度列表
     *
     * @return 支持的维度列表
     */
    @GetMapping("/dimensions")
    public Result<List<String>> retrieveSupportedDimensions() {
        List<String> dimensions = analysisService.retrieveSupportedAnalysisDimensions();
        return Result.success(dimensions);
    }

    // 向后兼容的方法 - 使用不同的路径避免冲突
    @PostMapping("/analyze/legacy/{fileId}")
    public Result<FlameGraph> analyzeJFRFileById(
            @PathVariable Long fileId,
            @RequestParam String dimension,
            @RequestParam(defaultValue = "true") boolean include,
            @RequestParam(required = false) List<String> taskSet,
            @RequestParam(required = false) Map<String, String> options) {
        return performAnalysisByFileId(fileId, dimension, include, taskSet, options);
    }

    @PostMapping("/analyze/legacy")
    public Result<FlameGraph> analyzeJFRFile(
            @RequestParam String filePath,
            @RequestParam String dimension,
            @RequestParam(defaultValue = "true") boolean include,
            @RequestParam(required = false) List<String> taskSet,
            @RequestParam(required = false) Map<String, String> options) {
        return executeAnalysisByPath(filePath, dimension, include, taskSet, options);
    }

    @GetMapping("/metadata/legacy")
    public Result<Metadata> getMetadata() {
        return retrieveAnalysisMetadata();
    }

    @GetMapping("/validate/legacy")
    public Result<Boolean> validateJFRFile(@RequestParam String filePath) {
        return validateFileIntegrity(filePath);
    }

    @GetMapping("/dimensions/legacy")
    public Result<List<String>> getSupportedDimensions() {
        return retrieveSupportedDimensions();
    }

} 