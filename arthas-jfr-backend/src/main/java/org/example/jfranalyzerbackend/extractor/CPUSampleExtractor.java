
package org.example.jfranalyzerbackend.extractor;


import org.example.jfranalyzerbackend.enums.EventConstant;
import org.example.jfranalyzerbackend.model.AnalysisResult;
import org.example.jfranalyzerbackend.model.DimensionResult;
import org.example.jfranalyzerbackend.model.TaskCount;
import org.example.jfranalyzerbackend.model.jfr.RecordedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CPUSampleExtractor extends CountExtractor {
    private boolean isWallClockEvents = false;
    protected static final List<String> INTERESTED = Collections.unmodifiableList(new ArrayList<String>() {
        {
            add(EventConstant.EXECUTION_SAMPLE);
            add(EventConstant.ACTIVE_SETTING);
        }
    });

    public CPUSampleExtractor(JFRAnalysisContext context) {
        super(context, INTERESTED);
    }

    @Override
    void visitExecutionSample(RecordedEvent event) {
        visitEvent(event);
    }

    @Override
    void visitActiveSetting(RecordedEvent event) {
        if (this.context.isExecutionSampleEventTypeId(event.getActiveSetting().eventId())) {
            if (EventConstant.WALL.equals(event.getString("name"))) {
                this.isWallClockEvents = true;
            }
        }
        if (EventConstant.EVENT.equals(event.getString("name")) && EventConstant.WALL.equals(event.getString("value"))) {
            this.isWallClockEvents = true;
        }
    }

    public List<TaskCount> buildTaskCounts() {
        if (this.isWallClockEvents) {
            return new ArrayList<>();
        } else {
            return super.buildTaskCounts();
        }
    }

    @Override
    public void fillResult(AnalysisResult result) {
        DimensionResult<TaskCount> tsResult = new DimensionResult<>();
        tsResult.setList(buildTaskCounts());
        result.setCpuSample(tsResult);
    }
}
