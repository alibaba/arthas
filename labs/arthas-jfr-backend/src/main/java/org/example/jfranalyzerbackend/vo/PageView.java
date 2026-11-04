package org.example.jfranalyzerbackend.vo;

import org.example.jfranalyzerbackend.util.PagingRequest;

import java.util.Collections;
import java.util.List;

public class PageView<T> {
    private static final PageView<?> EMPTY = new PageView<>(null, 0, Collections.emptyList());


    public static <T> PageView<T> empty() {       return (PageView<T>) EMPTY;
    }

    private List<T> data;

    private int page;

    private int pageSize;

    private int totalSize;

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }


    public PageView(PagingRequest request, int totalSize, List<T> data) {
        this.data = data;
        this.page = request != null ? request.getPage() : 0;
        this.pageSize = request != null ? request.getPageSize() : 0;
        this.totalSize = totalSize;
    }


    public PageView(int page, int pageSize, int totalSize, List<T> data) {
        this.data = data;
        this.page = page;
        this.pageSize = pageSize;
        this.totalSize = totalSize;
    }
}
