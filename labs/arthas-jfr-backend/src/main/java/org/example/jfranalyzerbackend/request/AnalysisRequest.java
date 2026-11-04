
package org.example.jfranalyzerbackend.request;

import lombok.Getter;

import java.io.InputStream;
import java.nio.file.Path;

@Getter
public class AnalysisRequest {
    private final int parallelWorkers;
    private final Path input;
    private final InputStream inputStream;
    private final int dimensions;

    public AnalysisRequest(Path input, int dimensions) {
        this(1, input, dimensions);
    }



    public AnalysisRequest(int parallelWorkers, Path input, int dimensions) {
        this(parallelWorkers, input, null, dimensions);
    }

    private AnalysisRequest(int parallelWorkers, Path p, InputStream stream, int dimensions) {
        this.parallelWorkers = parallelWorkers;
        this.input = p;
        this.dimensions = dimensions;
        this.inputStream = stream;
    }
}
