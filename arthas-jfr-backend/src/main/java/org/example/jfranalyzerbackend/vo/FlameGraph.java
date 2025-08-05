
package org.example.jfranalyzerbackend.vo;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FlameGraph extends GraphBase {
    private Object[][] data = new Object[0][];
}
