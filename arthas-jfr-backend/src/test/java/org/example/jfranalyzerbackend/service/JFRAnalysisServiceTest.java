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
        System.out.println("test");
    }
} 