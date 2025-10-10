

package org.example.jfranalyzerbackend.service;



import org.example.jfranalyzerbackend.vo.FlameGraph;
import org.example.jfranalyzerbackend.vo.Metadata;

import java.util.List;

public interface JFRAnalyzer {
    Metadata metadata();
    FlameGraph getFlameGraph(String dimension, boolean include, List<String> taskSet);
}
