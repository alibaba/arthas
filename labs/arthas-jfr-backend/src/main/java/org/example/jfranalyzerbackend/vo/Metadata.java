
package org.example.jfranalyzerbackend.vo;

import lombok.Getter;
import lombok.Setter;
import org.example.jfranalyzerbackend.model.PerfDimension;


@Setter
@Getter
public class Metadata {
    private PerfDimension[] perfDimensions;
}
