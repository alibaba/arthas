
package org.example.jfranalyzerbackend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class DimensionResult<T> {

    private List<T> list;

    public void add(T t) {
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(t);
    }
}
