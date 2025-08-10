
package org.example.jfranalyzerbackend.extractor;

import java.util.List;

public abstract class FileIOExtractor extends SumExtractor {
    FileIOExtractor(JFRAnalysisContext context, List<String> interested) {
        super(context, interested);
    }
}
