
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Task {

    private long id;

    private String name;

    // unit: ms, -1 means unknown
    private long start = -1;

    // unit: ms, -1 means unknown
    private long end = -1;
}
