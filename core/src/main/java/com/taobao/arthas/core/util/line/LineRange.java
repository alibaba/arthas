package com.taobao.arthas.core.util.line;

/**
 * Represent a line range([start, end]) to be listened on.
 * <p>
 * Zero and negative values means infinite.
 */
public class LineRange {
    private int start;
    private int end;

    public LineRange(int start, int end) {
        this.start = start;
        this.end = end;
        if (this.start <= 0) {
            this.start = 0;
        }
        if (this.end <= 0) {
            this.end = Integer.MAX_VALUE;
        }
    }

    public boolean inRange(int line) {
        return line >= start && line <= end;
    }

    // accept "1", "1-3", inclusive
    public static LineRange valueOf(String rangeDesc) {
        if (rangeDesc == null) {
            throw new IllegalArgumentException("line range should not be null");
        }

        if (rangeDesc.contains("-")) {
            String[] range = rangeDesc.split("-");
            if (range.length != 2) {
                throw new IllegalArgumentException("range should be seperated by `-`, e.g. 1-3");
            }
            return new LineRange(Integer.parseInt(range[0]), Integer.parseInt(range[1]));
        } else {
            int line = Integer.parseInt(rangeDesc.trim());
            return new LineRange(line, line);
        }
    }

    @Override
    public String toString() {
        return "LineRange{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
