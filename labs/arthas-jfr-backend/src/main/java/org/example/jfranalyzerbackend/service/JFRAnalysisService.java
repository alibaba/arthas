package org.example.jfranalyzerbackend.service;

import org.example.jfranalyzerbackend.vo.FlameGraph;
import org.example.jfranalyzerbackend.vo.Metadata;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface JFRAnalysisService {
    Metadata getMetadata();
    boolean isValidJFRFile(Path path);
    FlameGraph analyzeAndGenerateFlameGraph(Path path, String dimension, boolean include, List<String> taskSet, Map<String, String> options);
    List<String> getSupportedDimensions();
}
