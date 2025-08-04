package org.example.jfranalyzerbackend.service;

import org.example.jfranalyzerbackend.vo.FlameGraph;
import org.example.jfranalyzerbackend.vo.Metadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JFR分析服务测试类
 */
@SpringBootTest
class JFRAnalysisServiceTest {

    @Autowired
    private JFRAnalysisService jfrAnalysisService;

    @Test
    void testGetMetadata() {
        // 测试获取元数据
        Metadata metadata = jfrAnalysisService.getMetadata();
        
        assertNotNull(metadata);
        assertNotNull(metadata.getPerfDimensions());
        assertFalse(metadata.getPerfDimensions().isEmpty());
        
        // 验证包含预期的维度
        List<String> dimensions = metadata.getPerfDimensions();
        assertTrue(dimensions.contains("cpu"));
        assertTrue(dimensions.contains("alloc"));
        assertTrue(dimensions.contains("mem"));
    }

    @Test
    void testValidateJFRFile_NonExistentFile() {
        // 测试验证不存在的文件
        Path nonExistentPath = Paths.get("/non/existent/file.jfr");
        boolean isValid = jfrAnalysisService.isValidJFRFile(nonExistentPath);
        
        assertFalse(isValid);
    }

    @Test
    void testValidateJFRFile_NullPath() {
        // 测试验证空路径
        boolean isValid = jfrAnalysisService.isValidJFRFile(null);
        
        assertFalse(isValid);
    }

    @Test
    void testAnalyzeJFRFile_InvalidFile() {
        // 测试分析无效文件
        Path invalidPath = Paths.get("/non/existent/file.jfr");
        
        assertThrows(RuntimeException.class, () -> {
            jfrAnalysisService.analyzeAndGenerateFlameGraph(
                invalidPath, "cpu", true, null, null);
        });
    }

    @Test
    void testGetSupportedDimensions() {
        // 测试获取支持的维度列表
        Metadata metadata = jfrAnalysisService.getMetadata();
        List<String> dimensions = metadata.getPerfDimensions();
        
        assertNotNull(dimensions);
        assertTrue(dimensions.size() > 0);
        
        // 验证包含主要维度
        assertTrue(dimensions.contains("cpu"));
        assertTrue(dimensions.contains("cpu-sample"));
        assertTrue(dimensions.contains("wall-clock"));
        assertTrue(dimensions.contains("alloc"));
        assertTrue(dimensions.contains("mem"));
    }
} 