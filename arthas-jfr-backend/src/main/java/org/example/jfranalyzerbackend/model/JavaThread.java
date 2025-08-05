
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JavaThread extends Task {
    private long javaId;
    private long osId;
}
