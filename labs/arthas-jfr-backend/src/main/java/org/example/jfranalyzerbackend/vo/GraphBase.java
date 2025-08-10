
package org.example.jfranalyzerbackend.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class GraphBase {
    private List<String> threads = new ArrayList<>();
    private Map<String, Long> threadSplit = new HashMap<>();
    private Map<Integer, String> symbolTable = new HashMap<>();
}
