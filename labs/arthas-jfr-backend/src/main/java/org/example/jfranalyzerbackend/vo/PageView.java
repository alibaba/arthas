package org.example.jfranalyzerbackend.vo;

import org.example.jfranalyzerbackend.util.PagingRequest;

import java.util.Collections;
import java.util.List;

public class PageView<T> {
    private static final PageView<?> EMPTY = new PageView<>(null, 0, Collections.emptyList());

    /**
     * Return an empty page view.
     *
     * @param <T> data type
     * @return empty page view
     */
    @SuppressWarnings("unchecked")
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

    /**
     * Create a page view with paging request, total size and data.
     *
     * @param request   paging request
     * @param totalSize total size
     * @param data      data
     */
    public PageView(PagingRequest request, int totalSize, List<T> data) {
        this.data = data;
        this.page = request != null ? request.getPage() : 0;
        this.pageSize = request != null ? request.getPageSize() : 0;
        this.totalSize = totalSize;
    }

    /**
     * Create a page view with page index, page size, total size and data.
     *
     * @param page      page index
     * @param pageSize  page size
     * @param totalSize total size
     * @param data      data
     */
    public PageView(int page, int pageSize, int totalSize, List<T> data) {
        this.data = data;
        this.page = page;
        this.pageSize = pageSize;
        this.totalSize = totalSize;
    }
}
