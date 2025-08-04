
package org.example.jfranalyzerbackend.model.jfr;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.unit.IQuantity;

import java.lang.reflect.Field;

@Slf4j
public class RecordedThread {
    @Setter
    @Getter
    private long javaThreadId;
    @Getter
    private String javaName;
    @Setter
    private long osThreadId;

    public RecordedThread(String javaName, long javaThreadId, long osThreadId) {
        this.javaName = javaName;
        this.javaThreadId = javaThreadId;
        this.osThreadId = osThreadId;
    }

    public RecordedThread(IMCThread imcThread) {
        this.javaThreadId = imcThread.getThreadId();
        this.javaName = imcThread.getThreadName();
        try {
            Field f = imcThread.getClass().getDeclaredField("osThreadId");
            f.setAccessible(true);
            Object value = f.get(imcThread);
            if (value instanceof IQuantity) {
                this.osThreadId = ((IQuantity) value).longValue();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        if (this.javaThreadId == 0 && this.osThreadId > 0) {
            this.javaThreadId = -this.osThreadId;
        }
    }

    public long getOSThreadId() {
        return osThreadId;
    }
}
