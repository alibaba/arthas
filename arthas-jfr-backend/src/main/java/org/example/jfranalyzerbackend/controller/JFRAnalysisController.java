package org.example.jfranalyzerbackend.controller;

import org.example.jfranalyzerbackend.config.Result;
import org.example.jfranalyzerbackend.config.ArthasConfig;
import org.example.jfranalyzerbackend.entity.PerfDimensionFactory;
import org.example.jfranalyzerbackend.service.JFRAnalysisService;
import org.example.jfranalyzerbackend.service.FileService;
import org.example.jfranalyzerbackend.vo.FlameGraph;
import org.example.jfranalyzerbackend.vo.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * JFR分析控制器
 * 提供JFR文件分析和火焰图生成的REST API
 */
@RestController
@RequestMapping("/api/jfr")
public class JFRAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(JFRAnalysisController.class);

    private final JFRAnalysisService jfrAnalysisService;
    private final FileService fileService;
    private final ArthasConfig arthasConfig;

    @Autowired
    public JFRAnalysisController(JFRAnalysisService jfrAnalysisService, FileService fileService, ArthasConfig arthasConfig) {
        this.jfrAnalysisService = jfrAnalysisService;
        this.fileService = fileService;
        this.arthasConfig = arthasConfig;
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
    public Result<FlameGraph> analyzeJFRFileById(
            @PathVariable Long fileId,
            @RequestParam String dimension,
            @RequestParam(defaultValue = "true") boolean include,
            @RequestParam(required = false) List<String> taskSet,
            @RequestParam(required = false) Map<String, String> options) {
        String filePath = fileService.getFilePathById(fileId);
        FlameGraph fg = jfrAnalysisService.analyzeAndGenerateFlameGraph(
            Paths.get(filePath), dimension, include, taskSet, options
        );
        log.info("火焰图生成完成，数据点数量: {}", fg.getData().length);
        System.out.println(fg.getSymbolTable());
        System.out.println(fg);
        return Result.success(fg);
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
    public Result<FlameGraph> analyzeJFRFile(
            @RequestParam String filePath,
            @RequestParam String dimension,
            @RequestParam(defaultValue = "true") boolean include,
            @RequestParam(required = false) List<String> taskSet,
            @RequestParam(required = false) Map<String, String> options) {
        FlameGraph fg = jfrAnalysisService.analyzeAndGenerateFlameGraph(
            Paths.get(filePath), dimension, include, taskSet, options
        );
        log.info("火焰图生成完成，数据点数量: {}", fg.getData().length);
        System.out.println(fg.getSymbolTable());
        System.out.println(fg);
        return Result.success(fg);
    }

    /**
     * 获取分析元数据
     *
     * @return 元数据信息
     */
    @GetMapping("/metadata")
    public Result<Metadata> getMetadata() {
        Metadata metadata = jfrAnalysisService.getMetadata();
        log.info("=== 获取元数据 ===");
        System.out.println( metadata);
        return Result.success(metadata);
    }

    /**
     * 验证JFR文件是否有效
     *
     * @param filePath JFR文件路径
     * @return 验证结果
     */
    @GetMapping("/validate")
    public Result<Boolean> validateJFRFile(@RequestParam String filePath) {
        boolean valid = jfrAnalysisService.isValidJFRFile(Paths.get(filePath));
        return Result.success(valid);
    }

    /**
     * 获取支持的分析维度列表
     *
     * @return 支持的维度列表
     */
    @GetMapping("/dimensions")
    public Result<List<String>> getSupportedDimensions() {
        List<String> dims = jfrAnalysisService.getSupportedDimensions();
        System.out.println(dims);
        return Result.success(dims);
    }

} 