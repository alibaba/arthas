
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Problem {

    private String summary;

    private String solution;

    public Problem(String summary, String solution) {
        this.summary = summary;
        this.solution = solution;
    }
}
